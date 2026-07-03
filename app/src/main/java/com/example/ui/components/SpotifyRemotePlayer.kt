package com.example.ui.components

import android.content.Context
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * İmza şarkısının kırpılmış kesitini, cihazdaki Spotify uygulaması üzerinden
 * (App Remote SDK) tam şarkının içinden çalar: parçayı başlatır, kırpma
 * başlangıcına atlar ve kesit süresi dolunca duraklatır.
 *
 * Belirli bir parçayı isteğe bağlı çalmak Spotify tarafında Premium hesap
 * gerektirir. Spotify yüklü değilse, bağlantı kurulamazsa veya çalma
 * reddedilirse [onError] çağrılır; çağıran taraf synth'e geri düşer.
 */
class SpotifyRemotePlayer(
    private val clientId: String,
    private val redirectUri: String
) {
    private val TAG = "SpotifyRemotePlayer"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var appRemote: SpotifyAppRemote? = null
    private var clipJob: Job? = null

    // Her play/stop çağrısında artar; geç gelen bağlantı/çalma callback'lerinin
    // yeni oturumu bozmasını engeller.
    private var sessionId = 0

    fun playClip(
        context: Context,
        trackId: String,
        startSec: Float,
        endSec: Float,
        onFinished: () -> Unit,
        onError: () -> Unit
    ) {
        stop()
        val session = ++sessionId

        if (trackId.isBlank() || !SpotifyAppRemote.isSpotifyInstalled(context)) {
            onError()
            return
        }

        val params = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, params, object : Connector.ConnectionListener {
            override fun onConnected(remote: SpotifyAppRemote) {
                if (session != sessionId) {
                    SpotifyAppRemote.disconnect(remote)
                    return
                }
                appRemote = remote
                startClip(remote, session, trackId, startSec, endSec, onFinished, onError)
            }

            override fun onFailure(error: Throwable) {
                Log.w(TAG, "Spotify App Remote bağlantısı kurulamadı", error)
                if (session == sessionId) onError()
            }
        })
    }

    private fun startClip(
        remote: SpotifyAppRemote,
        session: Int,
        trackId: String,
        startSec: Float,
        endSec: Float,
        onFinished: () -> Unit,
        onError: () -> Unit
    ) {
        val startMs = (startSec * 1000).toLong().coerceAtLeast(0L)
        val clipMs = ((endSec - startSec) * 1000).toLong().coerceAtLeast(1000L)

        remote.playerApi.play("spotify:track:$trackId")
            .setResultCallback {
                if (session != sessionId) return@setResultCallback
                clipJob = scope.launch {
                    // Parçanın Spotify tarafında yüklenmesine kısa bir pay bırak
                    delay(400)
                    if (session != sessionId) return@launch
                    if (startMs > 0) {
                        remote.playerApi.seekTo(startMs).setErrorCallback { e ->
                            Log.w(TAG, "Kesit başına atlanamadı", e)
                        }
                    }
                    delay(clipMs)
                    if (session == sessionId) {
                        stop()
                        onFinished()
                    }
                }
            }
            .setErrorCallback { e ->
                // Ücretsiz hesaplarda isteğe bağlı parça çalma reddedilir
                Log.w(TAG, "Spotify çalma başarısız (Premium gerekebilir)", e)
                if (session == sessionId) {
                    stop()
                    onError()
                }
            }
    }

    fun stop() {
        sessionId++
        clipJob?.cancel()
        clipJob = null
        val remote = appRemote ?: return
        appRemote = null
        try {
            remote.playerApi.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Spotify pause hatası", e)
        }
        try {
            SpotifyAppRemote.disconnect(remote)
        } catch (e: Exception) {
            Log.e(TAG, "Spotify disconnect hatası", e)
        }
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
