package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureEngineTest {
    @Test
    fun weeklyChallengeIsStableWithinAWeek() {
        val monday = 1_788_134_400_000L
        assertEquals(FeatureEngine.weeklyFor(monday), FeatureEngine.weeklyFor(monday + 3 * 86_400_000L))
    }

    @Test
    fun weeklyRulesHaveTheirOwnTargets() {
        val challenges = (0L..20L).map { FeatureEngine.weeklyFor(1_700_000_000_000L + it * 7 * 86_400_000L) }
        challenges.forEach { challenge ->
            assertEquals(when (challenge.rule) {
                WeeklyRule.CLICKS_ONLY -> 500.0
                WeeklyRule.FRAGILE_DRONES -> 75_000.0
                WeeklyRule.VOLATILE_MARKET -> 30.0
            }, challenge.target, 0.0)
        }
    }

    @Test
    fun refreshPreservesCurrentWeekAndResetsOldWeek() {
        val now = 1_788_134_400_000L
        val current = FeatureEngine.weeklyFor(now).copy(progress = 12.0, active = true)
        assertEquals(current, FeatureEngine.refreshWeekly(GameState(weeklyGalaxy = current), now).weeklyGalaxy)

        val stale = current.copy(weekKey = -10L, rewardClaimed = true)
        val refreshed = FeatureEngine.refreshWeekly(GameState(weeklyGalaxy = stale), now).weeklyGalaxy
        assertEquals(0.0, refreshed.progress, 0.0)
        assertFalse(refreshed.active)
        assertFalse(refreshed.rewardClaimed)
    }

    @Test
    fun stationCostsTripleAndModulesUseDifferentBasePrices() {
        assertEquals(25_000.0, FeatureEngine.stationCost(StationModule.HANGAR, 0), 0.0)
        assertEquals(75_000.0, FeatureEngine.stationCost(StationModule.HANGAR, 1), 0.0)
        assertEquals(40_000.0, FeatureEngine.stationCost(StationModule.LABORATORY, 0), 0.0)
        assertEquals(60_000.0, FeatureEngine.stationCost(StationModule.REACTOR, 0), 0.0)
        assertEquals(90_000.0, FeatureEngine.stationCost(StationModule.TRADE_HUB, 0), 0.0)
    }

    @Test
    fun everyStationModuleAppliesItsAdvertisedMultiplier() {
        val state = GameState(stationLevels = mapOf(
            StationModule.HANGAR to 2,
            StationModule.LABORATORY to 2,
            StationModule.REACTOR to 2,
            StationModule.TRADE_HUB to 2
        ))
        assertEquals(1.24, FeatureEngine.stationClickMultiplier(state), 0.0001)
        assertEquals(1.30, FeatureEngine.stationDpsMultiplier(state), 0.0001)
        assertEquals(1.20, FeatureEngine.stationRewardMultiplier(state), 0.0001)
    }

    @Test
    fun volatileMarketAlternatesBetweenDiscountAndMarkup() {
        assertEquals(0.65, FeatureEngine.volatilePriceMultiplier(0L), 0.0)
        assertEquals(1.35, FeatureEngine.volatilePriceMultiplier(60_000L), 0.0)
        assertEquals(0.65, FeatureEngine.volatilePriceMultiplier(120_000L), 0.0)
    }

    @Test
    fun gameStateDefaultsKeepNewFeaturesSafeForOldSaves() {
        val state = GameState()
        assertTrue(state.stationLevels.isEmpty())
        assertFalse(state.weeklyGalaxy.active)
        assertFalse(state.weeklyGalaxy.rewardClaimed)
    }
}
