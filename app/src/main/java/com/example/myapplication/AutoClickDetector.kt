package com.example.myapplication

import kotlin.math.abs

/** Detects sustained machine-like click rhythms using monotonic timestamps. */
class AutoClickDetector {
    data class Result(
        val allowed: Boolean,
        val newlyDetected: Boolean = false,
        val remainingBlockMillis: Long = 0L
    )

    private val clicks = ArrayDeque<Long>()
    private var blockedUntil = 0L
    private var strikes = 0

    @Synchronized
    fun registerClick(nowMillis: Long): Result {
        if (nowMillis < blockedUntil) {
            return Result(false, remainingBlockMillis = blockedUntil - nowMillis)
        }

        clicks.addLast(nowMillis)
        while (clicks.isNotEmpty() && nowMillis - clicks.first() > ANALYSIS_WINDOW_MILLIS) {
            clicks.removeFirst()
        }
        if (clicks.size < MIN_SAMPLE_SIZE) return Result(true)

        val timestamps = clicks.toList()
        val intervals = timestamps.zipWithNext { first, second -> (second - first).coerceAtLeast(1L).toDouble() }
        val average = intervals.average()
        val relativeDeviation = intervals.sumOf { abs(it - average) } / intervals.size / average
        val impossiblyFast = average <= IMPOSSIBLE_AVERAGE_INTERVAL_MILLIS
        val machineRegular = average <= REGULAR_MAX_INTERVAL_MILLIS && relativeDeviation <= MAX_RELATIVE_DEVIATION
        if (!impossiblyFast && !machineRegular) return Result(true)

        strikes++
        val blockMillis = (BASE_BLOCK_MILLIS * (1L shl (strikes - 1).coerceAtMost(3)))
            .coerceAtMost(MAX_BLOCK_MILLIS)
        blockedUntil = nowMillis + blockMillis
        clicks.clear()
        return Result(false, newlyDetected = true, remainingBlockMillis = blockMillis)
    }

    @Synchronized
    fun remainingBlockMillis(nowMillis: Long): Long = (blockedUntil - nowMillis).coerceAtLeast(0L)

    private companion object {
        const val ANALYSIS_WINDOW_MILLIS = 2_500L
        const val MIN_SAMPLE_SIZE = 12
        const val IMPOSSIBLE_AVERAGE_INTERVAL_MILLIS = 75.0
        const val REGULAR_MAX_INTERVAL_MILLIS = 130.0
        const val MAX_RELATIVE_DEVIATION = 0.055
        const val BASE_BLOCK_MILLIS = 15_000L
        const val MAX_BLOCK_MILLIS = 120_000L
    }
}
