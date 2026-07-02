package com.example.ui.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sin

class ProfileSynthEngine {
    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    var isPlaying = false
        private set

    fun start(genre: String) {
        if (isPlaying) stop()
        isPlaying = true
        
        synthJob = scope.launch {
            try {
                val sampleRate = 22050
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ) * 2

                audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }

                audioTrack?.play()

                // Melodies/chords based on genre
                val freqs = when (genre.lowercase()) {
                    "pop" -> doubleArrayOf(261.63, 329.63, 392.00, 440.00) // C, E, G, A
                    "rock" -> doubleArrayOf(196.00, 220.00, 293.66, 329.63) // G, A, D, E (pentatonic power)
                    "electronic" -> doubleArrayOf(130.81, 146.83, 164.81, 196.00) // Deep Bass
                    "classical" -> doubleArrayOf(261.63, 311.13, 392.00, 493.88) // C minor-ish, G
                    else -> doubleArrayOf(261.63, 329.63, 392.00)
                }

                val fmIndex = when (genre.lowercase()) {
                    "electronic" -> 15.0
                    "rock" -> 8.0
                    "pop" -> 3.0
                    "classical" -> 1.0
                    else -> 4.0
                }

                var currentNoteIndex = 0
                val noteDurationSamples = sampleRate * 1.0 // 1 second per note
                var sampleIndex = 0L

                val buffer = ShortArray(1024)

                while (isActive && isPlaying) {
                    val carrierFreq = freqs[currentNoteIndex]
                    val modulatorFreq = carrierFreq * 1.5 // FM Carrier/Modulator Ratio

                    for (i in buffer.indices) {
                        val t = sampleIndex / sampleRate.toDouble()
                        
                        // Simple FM Synthesis: Modulator alters Carrier phase
                        val modulator = sin(2.0 * Math.PI * modulatorFreq * t)
                        val carrier = sin(2.0 * Math.PI * carrierFreq * t + fmIndex * modulator)
                        
                        // Volume envelope (gentle attack & decay to prevent clicking)
                        val noteProgress = (sampleIndex % noteDurationSamples) / noteDurationSamples
                        val envelope = if (noteProgress < 0.1) {
                            noteProgress / 0.1 // Attack
                        } else {
                            (1.0 - noteProgress) // Decay
                        }

                        val sampleValue = (carrier * 16383.0 * envelope).toInt().toShort()
                        buffer[i] = sampleValue
                        sampleIndex++

                        // Switch notes after noteDurationSamples
                        if (sampleIndex % noteDurationSamples.toLong() == 0L) {
                            currentNoteIndex = (currentNoteIndex + 1) % freqs.size
                        }
                    }

                    audioTrack?.write(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                Log.e("ProfileSynthEngine", "Error in synthesiser loop: ${e.message}", e)
            } finally {
                cleanUp()
            }
        }
    }

    fun stop() {
        isPlaying = false
        synthJob?.cancel()
        synthJob = null
        cleanUp()
    }

    private fun cleanUp() {
        try {
            audioTrack?.apply {
                if (state == AudioTrack.STATE_INITIALIZED) {
                    stop()
                    release()
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileSynthEngine", "Error cleaning up audio track: ${e.message}")
        } finally {
            audioTrack = null
        }
    }
}
