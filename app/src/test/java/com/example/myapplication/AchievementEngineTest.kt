package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementEngineTest {
    @Test
    fun eligibleAchievementsUnlockTogether() {
        val state = GameState(
            ownedPlanets = (1..5).mapTo(mutableSetOf()) { "p$it" },
            fleetCounts = mapOf("drone" to 5),
            lifetimeStats = LifetimeStats(clicks = 100)
        )

        val result = AchievementEngine.evaluate(state)

        assertTrue("click_100" in result.unlockedAchievementIds)
        assertTrue("fleet_5" in result.unlockedAchievementIds)
        assertTrue("planets_5" in result.unlockedAchievementIds)
    }

    @Test
    fun debrisRewardScalesWithCurrentPlanetAndCanOnlyBeClaimedOnce() {
        val state = GameState(
            totalDebris = 100.0,
            currentPlanetId = "p3",
            unlockedAchievementIds = setOf("click_100")
        )

        val claimed = requireNotNull(AchievementEngine.claim(state, "click_100"))

        assertEquals(6_150.0, claimed.totalDebris, 0.0001)
        assertTrue("click_100" in claimed.claimedAchievementIds)
        assertNull(AchievementEngine.claim(claimed, "click_100"))
    }

    @Test
    fun lockedOrUnknownAchievementCannotBeClaimed() {
        assertNull(AchievementEngine.claim(GameState(), "click_100"))
        assertNull(AchievementEngine.claim(GameState(), "missing"))
    }

    @Test
    fun prestigePointRewardIsApplied() {
        val state = GameState(unlockedAchievementIds = setOf("prestige_1"))
        val result = requireNotNull(AchievementEngine.claim(state, "prestige_1"))
        assertEquals(1, result.prestigePoints)
    }
}
