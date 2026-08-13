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
        assertTrue(EconomyBalance.planetPrice(20) > EconomyBalance.planetPrice(19))
        assertTrue(EconomyBalance.planetPrice(24) > EconomyBalance.planetPrice(20))
        assertEquals(1.0, EconomyBalance.planetIncomeMultiplier("p1"), 0.0)
        assertEquals(1.21, EconomyBalance.planetIncomeMultiplier("p3"), 0.0001)
    }

    @Test
    fun planetIndexSupportsExpandedGalaxyAndStillClampsInvalidIds() {
        assertEquals(39, EconomyBalance.planetIndex("p39"))
        assertEquals(39, EconomyBalance.planetIndex("p999"))
        assertEquals(1, EconomyBalance.planetIndex("invalid"))
    }

    @Test
    fun nextPlanetGoalSkipsOwnedPlanetsAndClampsProgress() {
        assertEquals(3, EconomyBalance.nextPlanetIndex(setOf("p1", "p2")))
        assertEquals(2, EconomyBalance.nextPlanetIndex(setOf("p1", "p3")))
        assertEquals(null, EconomyBalance.nextPlanetIndex((1..39).map { "p$it" }.toSet()))
        assertEquals(0.5f, EconomyBalance.planetUnlockProgress(50.0, 100.0), 0.0001f)
        assertEquals(1f, EconomyBalance.planetUnlockProgress(150.0, 100.0), 0.0001f)
    }

    @Test
    fun outerGalaxyGraduallyRequiresALargerActiveFleet() {
        assertEquals(1, EconomyBalance.requiredActiveDronesForPlanet(2))
        assertEquals("drone_1", EconomyBalance.requiredDroneIdForPlanet(2))
        assertEquals("drone_13", EconomyBalance.requiredDroneIdForPlanet(13))
        assertEquals(0, EconomyBalance.requiredPrestigeForPlanet(10))
        assertEquals(1, EconomyBalance.requiredPrestigeForPlanet(11))
        assertEquals(3, EconomyBalance.requiredActiveDronesForPlanet(14))
        assertEquals(7, EconomyBalance.requiredActiveDronesForPlanet(39))
        assertTrue(EconomyBalance.planetFleetObjectiveMet(
            GameState(activeFleetCounts = mapOf("drone_1" to 1), discoveredDroneIds = setOf("drone_1")),
            2
        ))
    }

    @Test
    fun galaxyProgressionIsStrictlyIncreasingWithoutSuddenPriceSpikes() {
        val prices = (2..EconomyBalance.MAX_PLANET_INDEX).map(EconomyBalance::planetPrice)
        assertTrue(prices.zipWithNext().all { (current, next) -> next > current })
        assertTrue(prices.zipWithNext().all { (current, next) -> next / current <= 1.900001 })

        val income = (1..EconomyBalance.MAX_PLANET_INDEX)
            .map { EconomyBalance.planetIncomeMultiplier("p$it") }
        assertTrue(income.zipWithNext().all { (current, next) -> next > current })
    }

    @Test
    fun outerGalaxyPlanetsHaveDistinctGameplaySpecializations() {
        assertEquals(1.25, EconomyBalance.planetClickSpecial("p25"), 0.0001)
        assertEquals(1.30, EconomyBalance.planetFleetSpecial("p26"), 0.0001)
        assertEquals(1.40, EconomyBalance.planetSalvageSpecial("p27"), 0.0001)
        assertEquals(3.0, EconomyBalance.planetClickSpecial("p39"), 0.0001)
        assertEquals(3.0, EconomyBalance.planetFleetSpecial("p39"), 0.0001)
    }

    @Test
    fun passiveIncomeUsesRarityPlanetAndTechnology() {
        val fleet = mapOf("rare" to FleetConfig("rare", "", 0.0, 0, rarity = Rarity.RARE))
        val state = GameState(
            currentPlanetId = "p3",
            fleetCounts = mapOf("rare" to 2),
            technologies = setOf(Technology.POWER_CORE)
        )
        assertEquals(278.3, EconomyBalance.passiveIncome(state, fleet), 0.0001)
    }

    @Test
    fun clickUpgradeCostGrowsSmoothlyAndNeverExceedsCap() {
        assertEquals(150.0, EconomyBalance.clickUpgradeCost(15.0, 0), 0.0)
        assertEquals(1_086.0, EconomyBalance.clickUpgradeCost(15.0, 100), 0.0)
        assertEquals(2_500_000.0, EconomyBalance.clickUpgradeCost(5_000.0, 1_000), 0.0)
        assertEquals(2_500_000.0, EconomyBalance.clickUpgradeCost(4_000.0, 1_000, 1.5), 0.0)
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
