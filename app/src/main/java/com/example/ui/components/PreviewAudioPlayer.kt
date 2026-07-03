package com.example.ui.components

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * İmza şarkısının Spotify önizleme MP3'ünü (30 sn) internetten çalar ve
 * kullanıcının kırptığı [startSec, endSec] aralığı bitince kendiliğinden durur.
 * Aralık klip süresinin dışına taşarsa süreye göre kırpılır.
 */
class PreviewAudioPlayer {
    private val TAG = "PreviewAudioPlayer"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var mediaPlayer: MediaPlayer? = null
    private var stopJob: Job? = null

    fun play(url: String, startSec: Float, endSec: Float, onFinished: () -> Unit) {
        stop()

        val player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
        }
        mediaPlayer = player

        try {
            player.setDataSource(url)
        } catch (e: Exception) {
            Log.e(TAG, "Preview URL açılamadı: $url", e)
            stop()
            onFinished()
            return
        }

        player.setOnPreparedListener { mp ->
            if (mp !== mediaPlayer) return@setOnPreparedListener

            val durationMs = mp.duration
            var startMs = (startSec * 1000).toInt().coerceAtLeast(0)
            var endMs = (endSec * 1000).toInt()
            if (durationMs > 0) {
                // Eski profillerde kırpma aralığı klipten uzun olabilir; klibe sığdır
                if (startMs >= durationMs - 1000) startMs = 0
                endMs = endMs.coerceAtMost(durationMs)
            }
            val clipMs = (endMs - startMs).coerceAtLeast(1000).toLong()

            val beginPlayback = {
                mp.start()
                stopJob = scope.launch {
                    delay(clipMs)
                    if (mediaPlayer === mp) {
                        stop()
                        onFinished()
                    }
                }
            }

            if (startMs > 0) {
                mp.setOnSeekCompleteListener { beginPlayback() }
                mp.seekTo(startMs)
            } else {
                beginPlayback()
            }
        }

        player.setOnCompletionListener { mp ->
            if (mp === mediaPlayer) {
                stop()
                onFinished()
            }
        }

        player.setOnErrorListener { mp, what, extra ->
            Log.e(TAG, "Preview çalma hatası: what=$what extra=$extra")
            if (mp === mediaPlayer) {
                stop()
                onFinished()
            }
            true
        }

        player.prepareAsync()
    }

    fun stop() {
        stopJob?.cancel()
        stopJob = null
        val player = mediaPlayer ?: return
        mediaPlayer = null
        try {
            if (player.isPlaying) player.stop()
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer stop hatası", e)
        } finally {
            try {
                player.release()
            } catch (e: Exception) {
                Log.e(TAG, "MediaPlayer release hatası", e)
            }
        }
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
