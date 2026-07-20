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
