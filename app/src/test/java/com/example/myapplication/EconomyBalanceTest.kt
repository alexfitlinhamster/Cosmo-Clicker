package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyBalanceTest {
    @Test
    fun planetPricesGrowByThreeAndIncomeByOnePointFive() {
        assertEquals(10_000.0, EconomyBalance.planetPrice(2), 0.0)
        assertEquals(30_000.0, EconomyBalance.planetPrice(3), 0.0)
        assertEquals(90_000.0, EconomyBalance.planetPrice(4), 0.0)
        assertEquals(1.0, EconomyBalance.planetIncomeMultiplier("p1"), 0.0)
        assertEquals(2.25, EconomyBalance.planetIncomeMultiplier("p3"), 0.0)
    }

    @Test
    fun passiveIncomeUsesRarityPlanetAndTechnology() {
        val fleet = mapOf("rare" to FleetConfig("rare", "", 0.0, 0, rarity = Rarity.RARE))
        val state = GameState(
            currentPlanetId = "p3",
            fleetCounts = mapOf("rare" to 2),
            technologies = setOf(Technology.POWER_CORE)
        )
        assertEquals(281.25, EconomyBalance.passiveIncome(state, fleet), 0.0)
    }

    @Test
    fun prestigeUnlocksAtPlanetTenAndScalesWithProgress() {
        assertFalse(EconomyBalance.canPrestige(GameState(ownedPlanets = setOf("p9"))))
        val p10 = GameState(ownedPlanets = setOf("p1", "p10"))
        assertTrue(EconomyBalance.canPrestige(p10))
        assertEquals(1, EconomyBalance.prestigeReward(p10))
        assertEquals(6, EconomyBalance.prestigeReward(GameState(ownedPlanets = setOf("p15"))))
    }

    @Test
    fun economyTickAddsPassiveIncome() {
        val result = EconomyEngine.processTick(GameState(totalDebris = 100.0), 1_000L, 25.0)
        assertEquals(125.0, result.totalDebris, 0.0)
    }
}
