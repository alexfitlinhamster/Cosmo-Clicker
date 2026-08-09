package com.example.myapplication

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoClickDetectorTest {
    @Test
    fun regularAutoclickerPatternIsBlocked() {
        val detector = AutoClickDetector()
        var result = AutoClickDetector.Result(allowed = true)

        repeat(24) { index -> result = detector.registerClick(index * 100L) }

        assertFalse(result.allowed)
        assertTrue(result.newlyDetected)
        assertTrue(result.remainingBlockMillis >= 10_000L)
    }

    @Test
    fun variedHumanPatternIsAllowed() {
        val detector = AutoClickDetector()
        var now = 0L
        val intervals = listOf(90L, 180L, 75L, 240L, 110L, 165L, 85L, 210L)

        repeat(32) { index ->
            now += intervals[index % intervals.size]
            assertTrue(detector.registerClick(now).allowed)
        }
    }

    @Test
    fun repeatedDetectionIncreasesBlockDuration() {
        val detector = AutoClickDetector()
        var now = 0L
        repeat(24) { now += 100L; detector.registerClick(now) }
        now += 10_001L
        var second = AutoClickDetector.Result(true)
        repeat(24) { now += 100L; second = detector.registerClick(now) }

        assertFalse(second.allowed)
        assertTrue(second.remainingBlockMillis >= 20_000L)
    }
}
