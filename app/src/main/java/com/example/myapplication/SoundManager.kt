package com.example.myapplication

import android.media.AudioManager
import android.media.ToneGenerator

class SoundManager : AutoCloseable {
    private val lock = Any()
    private var toneGenerator: ToneGenerator? = createToneGenerator()

    fun playClick() {
        synchronized(lock) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, CLICK_DURATION_MS)
        }
    }

    fun playEventStart() = playTone(ToneGenerator.TONE_PROP_PROMPT, 120)

    fun playEventSuccess() = playTone(ToneGenerator.TONE_PROP_ACK, 160)

    fun playEventFailure() = playTone(ToneGenerator.TONE_PROP_NACK, 220)

    private fun playTone(tone: Int, durationMillis: Int) {
        synchronized(lock) {
            toneGenerator?.startTone(tone, durationMillis)
        }
    }

    override fun close() {
        synchronized(lock) {
            toneGenerator?.release()
            toneGenerator = null
        }
    }

    private fun createToneGenerator(): ToneGenerator? =
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, CLICK_VOLUME_PERCENT)
        } catch (_: RuntimeException) {
            null
        }

    private companion object {
        const val CLICK_DURATION_MS = 35
        const val CLICK_VOLUME_PERCENT = 35
    }
}
