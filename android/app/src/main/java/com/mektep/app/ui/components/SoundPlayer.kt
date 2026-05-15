package com.mektep.app.ui.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Web Audio API-style sound effects matching the prototype.
 * Generates tones programmatically — no audio files needed.
 */
object SoundPlayer {

    private const val SAMPLE_RATE = 44100

    fun playCorrect() {
        Thread {
            // Three ascending sine tones: 523, 659, 784 Hz
            val samples = generateToneSequence(
                listOf(
                    Tone(523.0, 0.12, 0.0),
                    Tone(659.0, 0.12, 0.10),
                    Tone(784.0, 0.15, 0.22)
                )
            )
            playPcm(samples)
        }.start()
    }

    fun playWrong() {
        Thread {
            // Two descending sawtooth-ish tones: 300, 220 Hz
            val samples = generateToneSequence(
                listOf(
                    Tone(300.0, 0.10, 0.0),
                    Tone(220.0, 0.15, 0.12)
                )
            )
            playPcm(samples)
        }.start()
    }

    fun playComplete() {
        Thread {
            // Four ascending tones: 523, 659, 784, 1047 Hz
            val samples = generateToneSequence(
                listOf(
                    Tone(523.0, 0.15, 0.0),
                    Tone(659.0, 0.15, 0.13),
                    Tone(784.0, 0.15, 0.26),
                    Tone(1047.0, 0.25, 0.39)
                )
            )
            playPcm(samples)
        }.start()
    }

    private data class Tone(val freq: Double, val duration: Double, val startAt: Double)

    private fun generateToneSequence(tones: List<Tone>): ShortArray {
        val totalDuration = tones.maxOf { it.startAt + it.duration } + 0.05
        val totalSamples = (totalDuration * SAMPLE_RATE).toInt()
        val buffer = FloatArray(totalSamples)

        for (tone in tones) {
            val startSample = (tone.startAt * SAMPLE_RATE).toInt()
            val numSamples = (tone.duration * SAMPLE_RATE).toInt()
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val envelope = (1.0 - t / tone.duration) * 0.3 // linear decay
                val sample = sin(2 * PI * tone.freq * t) * envelope
                val idx = startSample + i
                if (idx < totalSamples) buffer[idx] += sample.toFloat()
            }
        }

        // Convert to 16-bit PCM
        return ShortArray(totalSamples) { i ->
            (buffer[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun playPcm(samples: ShortArray) {
        try {
            val bufferSize = samples.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()

            // Wait for playback to finish, then release
            Thread.sleep((samples.size * 1000L / SAMPLE_RATE) + 100)
            track.stop()
            track.release()
        } catch (_: Exception) { }
    }
}
