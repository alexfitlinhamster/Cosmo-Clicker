package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class EconomySimulationTest {
    @Test
    fun longActiveRunMovesQuicklyEarlyAndSlowsIntoLongTermPlay() {
        var balance = 50.0
        var clickPower = 5.0
        var upgradeLevel = 0
        var planet = 1
        val reachedAt = mutableMapOf<Int, Int>()

        repeat(250_000) { elapsed ->
            balance += clickPower * 4.0 + 5.0 * 1.10.pow((planet - 1).toDouble())

            val upgradeCost = EconomyBalance.clickUpgradeCost(15.0, upgradeLevel)
            if (upgradeLevel < EconomyBalance.MAX_CLICK_UPGRADE_LEVEL && balance >= upgradeCost) {
                balance -= upgradeCost
                upgradeLevel++
                clickPower += 5.0
            }

            val next = planet + 1
            if (next <= EconomyBalance.MAX_PLANET_INDEX && balance >= EconomyBalance.planetPrice(next)) {
                balance -= EconomyBalance.planetPrice(next)
                planet = next
                reachedAt.putIfAbsent(planet, elapsed + 1)
            }

            assertTrue(balance.isFinite() && balance >= 0.0)
            assertTrue(clickPower.isFinite() && clickPower > 0.0)
        }

        assertTrue("early game should reveal several worlds within ten simulated minutes", planet >= 5)
        assertTrue("mid game should be reachable in the same run", planet >= 15)
        assertTrue("the route must retain long-term goals", planet < EconomyBalance.MAX_PLANET_INDEX)
        assertTrue(reachedAt.getValue(9) > reachedAt.getValue(5))
    }

    @Test
    fun caseAndUpgradeCurvesRemainMonotonicAndFiniteAcrossLongRuns() {
        CaseType.entries.forEach { type ->
            val costs = (0..1_000).map { GameRules.calculateCaseCost(it, type) }
            assertTrue(costs.all { it.isFinite() && it > 0.0 })
            assertTrue(costs.zipWithNext().all { (current, next) -> next > current })
        }

        listOf(15.0, 220.0, 5_500.0, 220_000_000.0).forEach { base ->
            val costs = (0..EconomyBalance.MAX_CLICK_UPGRADE_LEVEL)
                .map { EconomyBalance.clickUpgradeCost(base, it) }
            assertTrue(costs.all { it.isFinite() && it > 0.0 })
            assertTrue(costs.zipWithNext().all { (current, next) -> next >= current })
        }
    }

    @Test
    fun offlineRewardIsCappedAndCannotOutgrowEquivalentOnlineFleetIncome() {
        val fleet = mapOf("void" to 4)
        val rarities = mapOf("void" to Rarity.VOID)
        val offline = OfflineProgressEngine.calculate(
            lastActiveAtMillis = 1_000L,
            nowMillis = 1_000L + 30L * 24L * 60L * 60L * 1_000L,
            fleetCounts = fleet,
            fleetRarities = rarities
        )
        val onlineForSameWindow = 4.0 * 10_000.0 * OfflineProgressEngine.MAX_OFFLINE_SECONDS

        assertEquals(OfflineProgressEngine.MAX_OFFLINE_SECONDS, offline.elapsedSeconds)
        assertTrue(offline.reward > 0.0)
        assertTrue(offline.reward < onlineForSameWindow)
    }
}
