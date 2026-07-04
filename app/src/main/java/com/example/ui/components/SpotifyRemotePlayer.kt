package com.example.ui.components

import android.content.Context
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.CallResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * İmza şarkısının kırpılmış kesitini, cihazdaki Spotify uygulaması üzerinden
 * (App Remote SDK) tam şarkının içinden çalar: parçayı başlatır, hemen
 * duraklatıp kırpma başlangıcına atlar, oradan sürdürür ve kesit süresi
 * dolunca duraklatır. Bağlantı kesitler arasında açık tutulur; yeniden
 * yetkilendirme istenmez.
 *
 * ÖNEMLİ: [playClip]'e verilen context bir Activity olmalıdır — yetkilendirme
 * gerektiğinde App Remote bu context üzerinden auth ekranını açar; Application
 * context ile açılamaz ve bağlantı hata verir.
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

        val existing = appRemote
        if (existing != null && existing.isConnected) {
            startClip(existing, session, trackId, startSec, endSec, onFinished, onError)
            return
        }
        appRemote = null

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
        val trackUri = "spotify:track:$trackId"

        clipJob = scope.launch {
            try {
                remote.playerApi.play(trackUri).awaitResult()
                // Şarkının başı duyulmadan kesite atlayabilmek için hemen duraklat
                runCatching { remote.playerApi.pause().awaitResult() }
                if (session != sessionId) return@launch

                // Doğru parçanın yüklenmesini bekle (en çok ~2 sn; parça
                // yeniden yönlendirildiyse URI eşleşmeyebilir, o zaman devam et)
                var attempts = 0
                while (session == sessionId && attempts < 20) {
                    val state = runCatching { remote.playerApi.playerState.awaitResult() }.getOrNull()
                    if (state?.track?.uri == trackUri) break
                    attempts++
                    delay(100)
                }
                if (session != sessionId) return@launch

                if (startMs > 0) {
                    runCatching { remote.playerApi.seekTo(startMs).awaitResult() }
                        .onFailure { e -> Log.w(TAG, "Kesit başına atlanamadı", e) }
                }
                remote.playerApi.resume().awaitResult()

                delay(clipMs)
                if (session == sessionId) {
                    stop()
                    onFinished()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Ücretsiz hesaplarda isteğe bağlı parça çalma reddedilir
                Log.w(TAG, "Spotify çalma başarısız (Premium gerekebilir)", e)
                if (session == sessionId) {
                    stop()
                    onError()
                }
            }
        }
    }

    /**
     * Çalan kesiti duraklatır; App Remote bağlantısını sonraki kesit için
     * açık tutar (tamamen koparmak için [release]).
     */
    fun stop() {
        sessionId++
        clipJob?.cancel()
        clipJob = null
        val remote = appRemote ?: return
        if (!remote.isConnected) {
            appRemote = null
            return
        }
        try {
            remote.playerApi.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Spotify pause hatası", e)
        }
    }

    fun release() {
        stop()
        val remote = appRemote
        appRemote = null
        if (remote != null) {
            try {
                SpotifyAppRemote.disconnect(remote)
            } catch (e: Exception) {
                Log.e(TAG, "Spotify disconnect hatası", e)
            }
        }
        scope.cancel()
    }

    /**
     * [CallResult]'ı coroutine'e köprüler. (SDK'nın kendi `await()` üyesi
     * bloklar; bu uzantı askıya alır.)
     */
    private suspend fun <T : Any> CallResult<T>.awaitResult(): T =
        suspendCancellableCoroutine { cont ->
            setResultCallback { result -> if (cont.isActive) cont.resume(result) }
            setErrorCallback { error -> if (cont.isActive) cont.resumeWithException(error) }
            cont.invokeOnCancellation { runCatching { cancel() } }
        }
}
