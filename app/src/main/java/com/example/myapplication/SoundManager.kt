package com.example.myapplication

import android.content.Context
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.MediaPlayer
import android.media.AudioTrack
import android.media.ToneGenerator
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundManager(context: Context) : AutoCloseable {
    private val lock = Any()
    private val appContext = context.applicationContext
    private var toneGenerator: ToneGenerator? = createToneGenerator()
    private var backgroundPlayer: MediaPlayer? = createBackgroundPlayer()
    private var effectPlayer: MediaPlayer? = null
    private val clickTracks = Array(CLICK_TRACK_COUNT) { index -> createClickTrack(index) }
    private var nextClickTrack = 0

    fun playClick() {
        synchronized(lock) {
            val track = clickTracks[nextClickTrack]
            nextClickTrack = (nextClickTrack + 1) % clickTracks.size
            if (track != null) {
                try {
                    track.stop()
                    track.setPlaybackHeadPosition(0)
                    track.play()
                } catch (_: IllegalStateException) {
                    // Audio can disappear briefly while Android changes output devices.
                }
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, CLICK_DURATION_MS)
            }
        }
    }

    fun playEventStart() = playTone(ToneGenerator.TONE_PROP_PROMPT, 120)

    fun playEventSuccess() = playTone(ToneGenerator.TONE_PROP_ACK, 160)

    fun playEventFailure() = playTone(ToneGenerator.TONE_PROP_NACK, 220)

    fun resumeBackgroundMusic() {
        synchronized(lock) {
            try {
                backgroundPlayer?.start()
            } catch (_: IllegalStateException) {
                // The player may be temporarily unavailable while audio output changes.
            }
        }
    }

    fun pauseBackgroundMusic() {
        synchronized(lock) {
            try {
                backgroundPlayer?.pause()
            } catch (_: IllegalStateException) {
                // The player may be temporarily unavailable while audio output changes.
            }
        }
    }

    fun playPlanetUnlock() = playEffect(R.raw.planet_unlock)

    fun playAchievementClaimed() = playEffect(R.raw.achievement_claimed)

    private fun playEffect(resourceId: Int) {
        synchronized(lock) {
            effectPlayer?.release()
            effectPlayer = MediaPlayer.create(appContext, resourceId)?.also { player ->
                player.setVolume(EFFECT_VOLUME, EFFECT_VOLUME)
                player.setOnCompletionListener {
                    synchronized(lock) {
                        if (effectPlayer === it) effectPlayer = null
                        it.release()
                    }
                }
                player.start()
            }
        }
    }

    private fun playTone(tone: Int, durationMillis: Int) {
        synchronized(lock) {
            toneGenerator?.startTone(tone, durationMillis)
        }
    }

    override fun close() {
        synchronized(lock) {
            clickTracks.forEach { it?.release() }
            backgroundPlayer?.release()
            backgroundPlayer = null
            effectPlayer?.release()
            effectPlayer = null
            toneGenerator?.release()
            toneGenerator = null
        }
    }

    private fun createClickTrack(variant: Int): AudioTrack? = try {
        val samples = createSalvagePop(variant)
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(CLICK_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
            .also {
                it.write(samples, 0, samples.size)
                it.setVolume(CLICK_VOLUME)
            }
    } catch (_: RuntimeException) {
        null
    }

    private fun createSalvagePop(variant: Int): ShortArray {
        val size = CLICK_SAMPLE_RATE * CLICK_DURATION_MS / 1_000
        var noiseState = 0x2468ace1 + variant * 7919
        var lowPassedNoise = 0.0
        var previousLowPassedNoise = 0.0
        return ShortArray(size) { index ->
            val time = index.toDouble() / CLICK_SAMPLE_RATE
            val attack = (time / 0.0008).coerceAtMost(1.0)
            val release = ((size - index).toDouble() / (CLICK_SAMPLE_RATE * 0.007)).coerceAtMost(1.0)
            noiseState = noiseState * 1_664_525 + 1_013_904_223
            val rawNoise = ((noiseState ushr 16) and 0xffff) / 32767.5 - 1.0
            lowPassedNoise += (rawNoise - lowPassedNoise) * 0.16
            val dryTick = lowPassedNoise - previousLowPassedNoise * 0.72
            previousLowPassedNoise = lowPassedNoise
            val mechanism = dryTick * exp(-time * 115.0) * 1.25
            val quietWeight = sin(2.0 * PI * 118.0 * time) * exp(-time * 72.0) * 0.13
            ((mechanism + quietWeight) * attack * release * Short.MAX_VALUE * 0.72)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    private fun createToneGenerator(): ToneGenerator? =
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, CLICK_VOLUME_PERCENT)
        } catch (_: RuntimeException) {
            null
        }

    private fun createBackgroundPlayer(): MediaPlayer? =
        MediaPlayer.create(appContext, R.raw.background_music)?.also {
            it.isLooping = true
            it.setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
        }

    private companion object {
        const val CLICK_DURATION_MS = 36
        const val CLICK_SAMPLE_RATE = 44_100
        const val CLICK_TRACK_COUNT = 4
        const val CLICK_VOLUME = 0.25f
        const val CLICK_VOLUME_PERCENT = 35
        const val MUSIC_VOLUME = 0.22f
        const val EFFECT_VOLUME = 0.7f
    }
}
