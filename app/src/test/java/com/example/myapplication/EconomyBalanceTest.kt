package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyBalanceTest {
    @Test
    fun planetPricesAndIncomeUseSlowerLongTermCurve() {
        assertEquals(10_000.0, EconomyBalance.planetPrice(2), 0.0)
        assertEquals(19_000.0, EconomyBalance.planetPrice(3), 0.0)
        assertEquals(36_100.0, EconomyBalance.planetPrice(4), 0.0001)
        assertEquals(1_000_000_000.0, EconomyBalance.planetPrice(20), 0.0)
        assertEquals(1.0, EconomyBalance.planetIncomeMultiplier("p1"), 0.0)
        assertEquals(1.21, EconomyBalance.planetIncomeMultiplier("p3"), 0.0001)
    }

    @Test
    fun passiveIncomeUsesRarityPlanetAndTechnology() {
        val fleet = mapOf("rare" to FleetConfig("rare", "", 0.0, 0, rarity = Rarity.RARE))
        val state = GameState(
            currentPlanetId = "p3",
            fleetCounts = mapOf("rare" to 2),
            technologies = setOf(Technology.POWER_CORE)
        )
        assertEquals(30.25, EconomyBalance.passiveIncome(state, fleet), 0.0001)
    }

    @Test
    fun clickUpgradeCostNeverExceedsFiveThousand() {
        assertEquals(15.0, EconomyBalance.clickUpgradeCost(15.0, 0), 0.0)
        assertEquals(5_000.0, EconomyBalance.clickUpgradeCost(5_000.0, 20), 0.0)
        assertEquals(5_000.0, EconomyBalance.clickUpgradeCost(4_000.0, 20, 1.5), 0.0)
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
