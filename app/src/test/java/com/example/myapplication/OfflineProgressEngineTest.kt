package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineProgressEngineTest {
    @Test
    fun rewardDependsOnFleetRarityAndElapsedTime() {
        val result = OfflineProgressEngine.calculate(
            lastActiveAtMillis = 1_000L,
            nowMillis = 3_601_000L,
            fleetCounts = mapOf("common" to 2, "rare" to 1),
            fleetRarities = mapOf("common" to Rarity.COMMON, "rare" to Rarity.RARE)
        )

        assertEquals(3_600L, result.elapsedSeconds)
        assertEquals(54_000.0, result.reward, 0.0001)
    }

    @Test
    fun offlineDurationIsCappedAtEightHours() {
        val result = OfflineProgressEngine.calculate(
            lastActiveAtMillis = 1_000L,
            nowMillis = 1_000L + 24 * 60 * 60 * 1_000L,
            fleetCounts = mapOf("drone" to 1),
            fleetRarities = mapOf("drone" to Rarity.COMMON)
        )

        assertEquals(OfflineProgressEngine.MAX_OFFLINE_SECONDS, result.elapsedSeconds)
        assertEquals(36_000.0, result.reward, 0.0001)
    }

    @Test
    fun invalidTimestampAndNegativeFleetProduceNoReward() {
        assertEquals(
            0.0,
            OfflineProgressEngine.calculate(2_000L, 1_000L, mapOf("drone" to -2), emptyMap()).reward,
            0.0
        )
    }
}
