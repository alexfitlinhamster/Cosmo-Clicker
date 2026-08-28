package com.example.myapplication

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoClickDetectorTest {
    @Test
    fun regularAutoclickerPatternIsBlocked() {
        val detector = AutoClickDetector()
        var result = AutoClickDetector.Result(allowed = true)

        repeat(50) { index ->
            val candidate = detector.registerClick(index * 100L)
            if (candidate.newlyDetected) result = candidate
        }

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
        var firstBlock = 0L
        repeat(50) {
            now += 100L
            val result = detector.registerClick(now)
            if (result.newlyDetected) firstBlock = result.remainingBlockMillis
        }
        now += firstBlock + 1L
        var second = AutoClickDetector.Result(true)
        repeat(50) {
            now += 100L
            val candidate = detector.registerClick(now)
            if (candidate.newlyDetected) second = candidate
        }

        assertFalse(second.allowed)
        assertTrue(second.remainingBlockMillis >= 20_000L)
    }

    @Test
    fun impossiblyFastMacroIsBlockedEvenWhenItMovesSlightly() {
        val detector = AutoClickDetector()
        var detected = false
        repeat(24) { index ->
            val result = detector.registerClick(index * 30L, (index % 5) * .05f, (index % 3) * .05f)
            detected = detected || result.newlyDetected
        }
        assertTrue(detected)
    }

    @Test
    fun regularMacroIsBlockedEvenWhenPointerPositionMoves() {
        val detector = AutoClickDetector()
        var detected = false
        var now = 0L
        repeat(45) { index ->
            now += listOf(96L, 104L, 100L)[index % 3]
            val result = detector.registerClick(now, (index % 9) / 10f, (index % 7) / 10f)
            detected = detected || result.newlyDetected
        }
        assertTrue(detected)
    }

    @Test
    fun commonFourClicksPerSecondAutoclickerIsBlocked() {
        val detector = AutoClickDetector()
        var detected = false

        repeat(30) { index ->
            val result = detector.registerClick(index * 250L, .5f, .5f)
            detected = detected || result.newlyDetected
        }

        assertTrue(detected)
    }

    @Test
    fun slowerAutoclickerIsBlockedAfterTenRegularClicks() {
        val detector = AutoClickDetector()
        var detectedAt = -1

        repeat(16) { index ->
            val result = detector.registerClick(index * 700L, .5f, .5f)
            if (result.newlyDetected && detectedAt < 0) detectedAt = index + 1
        }

        assertTrue(detectedAt in 1..10)
    }

    @Test
    fun staleInjectedTimestampIsBlocked() {
        val detector = AutoClickDetector()
        assertTrue(detector.registerClick(1_000L, .4f, .4f).allowed)
        val result = detector.registerClick(999L, .5f, .5f)
        assertFalse(result.allowed)
        assertTrue(result.newlyDetected)
    }

    @Test
    fun fastButIrregularHumanBurstRemainsAllowed() {
        val detector = AutoClickDetector()
        var now = 0L
        val intervals = listOf(48L, 92L, 57L, 135L, 66L, 111L, 74L, 149L)
        repeat(20) { index ->
            now += intervals[index % intervals.size]
            assertTrue(detector.registerClick(now, (index % 4) * .16f, (index % 5) * .13f).allowed)
        }
    }
}
