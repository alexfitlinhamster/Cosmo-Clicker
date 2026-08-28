package com.example.myapplication

import kotlin.math.abs

/** Rejects sustained machine-like input while allowing short human bursts. */
class AutoClickDetector {
    data class Result(
        val allowed: Boolean,
        val newlyDetected: Boolean = false,
        val remainingBlockMillis: Long = 0L
    )

    private data class Click(val time: Long, val x: Float, val y: Float)

    private val clicks = ArrayDeque<Click>()
    private var blockedUntil = 0L
    private var strikes = 0
    private var lastDetectionAt = Long.MIN_VALUE

    @Synchronized
    fun registerClick(nowMillis: Long, x: Float = 0f, y: Float = 0f): Result {
        if (nowMillis < blockedUntil) {
            return Result(false, remainingBlockMillis = blockedUntil - nowMillis)
        }

        if (lastDetectionAt != Long.MIN_VALUE && nowMillis - lastDetectionAt >= STRIKE_DECAY_MILLIS) {
            strikes = 0
        }

        // A monotonic timestamp protects the statistics from clock anomalies and
        // from injected events which arrive with an older timestamp.
        if (clicks.isNotEmpty() && nowMillis <= clicks.last().time) {
            return triggerBlock(nowMillis)
        }

        clicks.addLast(Click(nowMillis, x.coerceIn(0f, 1f), y.coerceIn(0f, 1f)))
        while (clicks.isNotEmpty() && nowMillis - clicks.first().time > ANALYSIS_WINDOW_MILLIS) {
            clicks.removeFirst()
        }
        if (clicks.size < MIN_SAMPLE_SIZE) return Result(true)

        val recent = clicks.toList()
        val intervals = recent.zipWithNext { first, second ->
            (second.time - first.time).coerceAtLeast(1L).toDouble()
        }
        val averageInterval = intervals.average()
        val relativeDeviation = intervals.sumOf { abs(it - averageInterval) } /
            intervals.size / averageInterval
        val roundedIntervals = intervals.map { (it / INTERVAL_BUCKET_MILLIS).toInt() }.distinct().size
        val repeatedAtPoint = recent.count { click ->
            val dx = click.x - x
            val dy = click.y - y
            dx * dx + dy * dy <= SAME_POINT_RADIUS * SAME_POINT_RADIUS
        }
        val impossiblyFast = recent.size >= FAST_SAMPLE_SIZE && averageInterval <= IMPOSSIBLE_INTERVAL_MILLIS
        val machineRhythm = recent.size >= REGULAR_SAMPLE_SIZE &&
            repeatedAtPoint >= REGULAR_SAMPLE_SIZE &&
            averageInterval <= REGULAR_MAX_INTERVAL_MILLIS &&
            relativeDeviation <= MAX_RELATIVE_DEVIATION
        val scriptedRhythmWithJitter = recent.size >= JITTER_SAMPLE_SIZE &&
            averageInterval <= JITTER_MAX_INTERVAL_MILLIS &&
            relativeDeviation <= JITTER_MAX_RELATIVE_DEVIATION &&
            roundedIntervals <= MAX_INTERVAL_BUCKETS
        val recentBurstCount = recent.count { nowMillis - it.time <= BURST_WINDOW_MILLIS }
        val impossibleBurst = recentBurstCount >= MAX_BURST_CLICKS
        if (!impossiblyFast && !machineRhythm && !scriptedRhythmWithJitter && !impossibleBurst) {
            return Result(true)
        }

        return triggerBlock(nowMillis)
    }

    private fun triggerBlock(nowMillis: Long): Result {
        strikes++
        val blockMillis = (BASE_BLOCK_MILLIS * (1L shl (strikes - 1).coerceAtMost(3)))
            .coerceAtMost(MAX_BLOCK_MILLIS)
        blockedUntil = nowMillis + blockMillis
        lastDetectionAt = nowMillis
        clicks.clear()
        return Result(false, newlyDetected = true, remainingBlockMillis = blockMillis)
    }

    @Synchronized
    fun remainingBlockMillis(nowMillis: Long): Long = (blockedUntil - nowMillis).coerceAtLeast(0L)

    private companion object {
        const val ANALYSIS_WINDOW_MILLIS = 12_000L
        const val MIN_SAMPLE_SIZE = 10
        const val FAST_SAMPLE_SIZE = 18
        const val REGULAR_SAMPLE_SIZE = 10
        const val JITTER_SAMPLE_SIZE = 36
        const val IMPOSSIBLE_INTERVAL_MILLIS = 45.0
        const val REGULAR_MAX_INTERVAL_MILLIS = 800.0
        const val MAX_RELATIVE_DEVIATION = 0.08
        const val JITTER_MAX_INTERVAL_MILLIS = 180.0
        const val JITTER_MAX_RELATIVE_DEVIATION = 0.085
        const val INTERVAL_BUCKET_MILLIS = 8.0
        const val MAX_INTERVAL_BUCKETS = 4
        const val BURST_WINDOW_MILLIS = 1_000L
        const val MAX_BURST_CLICKS = 22
        const val SAME_POINT_RADIUS = 0.035f
        const val BASE_BLOCK_MILLIS = 15_000L
        const val MAX_BLOCK_MILLIS = 120_000L
        const val STRIKE_DECAY_MILLIS = 5 * 60_000L
    }
}
