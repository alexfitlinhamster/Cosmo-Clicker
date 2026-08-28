package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesTest {

    @Test
    fun commonCaseCostGrowsByFifteenPercentAfterEveryPurchase() {
        assertEquals(25_000.0, GameRules.calculateCaseCost(0), 0.0001)
        assertEquals(28_750.0, GameRules.calculateCaseCost(1), 0.0001)
        assertEquals(87_946.9072, GameRules.calculateCaseCost(9), 0.001)
        assertEquals(101_138.9433, GameRules.calculateCaseCost(10), 0.001)
        assertEquals(25_000.0, GameRules.calculateCaseCost(-1), 0.0001)
    }

    @Test
    fun higherTierCasesCostMoreAndIncreasePremiumChance() {
        assertEquals(25_000.0, GameRules.calculateCaseCost(0, CaseType.COMMON), 0.0001)
        assertEquals(125_000.0, GameRules.calculateCaseCost(0, CaseType.RARE), 0.0001)
        assertEquals(500_000.0, GameRules.calculateCaseCost(0, CaseType.LEGENDARY), 0.0001)
        assertTrue(CaseType.COMMON.premiumChance < CaseType.RARE.premiumChance)
        assertTrue(CaseType.RARE.premiumChance < CaseType.LEGENDARY.premiumChance)
        assertEquals(1.15, CaseType.COMMON.growthMultiplier, 0.0)
        assertEquals(1.25, CaseType.RARE.growthMultiplier, 0.0)
        assertEquals(1.40, CaseType.LEGENDARY.growthMultiplier, 0.0)
    }

    @Test
    fun eachCaseTierUsesItsOwnGrowthCurve() {
        assertEquals(28_750.0, GameRules.calculateCaseCost(1, CaseType.COMMON), 0.001)
        assertEquals(156_250.0, GameRules.calculateCaseCost(1, CaseType.RARE), 0.001)
        assertEquals(700_000.0, GameRules.calculateCaseCost(1, CaseType.LEGENDARY), 0.001)
    }

    @Test
    fun caseWeightsAlwaysTotalOneHundred() {
        CaseType.entries.forEach { type ->
            assertEquals(100, GameRules.caseRarityWeights(type).values.sum())
        }
    }

    @Test
    fun cheapCaseCannotDropLegendaryAndSingleDroneSaleCannotExplodeEconomy() {
        assertEquals(0, GameRules.caseRarityWeights(CaseType.COMMON)[Rarity.LEGENDARY])
        val mostExpensiveDroneBase = 250.0 * Math.pow(1.45, 28.0)
        assertTrue(GameRules.droneSaleValue(mostExpensiveDroneBase, 1) < GameRules.calculateCaseCost(0))
    }

    @Test
    fun planetPurchaseDeductsPriceAndUnlocksPlanet() {
        val state = GameState(totalDebris = 25_000.0)

        val result = GameRules.purchaseOrSelectPlanet(state, "p2", 10_000.0)

        requireNotNull(result)
        assertEquals(15_000.0, result.totalDebris, 0.0001)
        assertEquals("p2", result.currentPlanetId)
        assertTrue("p2" in result.ownedPlanets)
    }

    @Test
    fun ownedPlanetCanBeSelectedWithoutPayingAgain() {
        val state = GameState(
            totalDebris = 25_000.0,
            currentPlanetId = "p1",
            ownedPlanets = setOf("p1", "p2")
        )

        val result = GameRules.purchaseOrSelectPlanet(state, "p2", 10_000.0)

        requireNotNull(result)
        assertEquals(25_000.0, result.totalDebris, 0.0001)
        assertEquals("p2", result.currentPlanetId)
    }

    @Test
    fun planetPurchaseIsRejectedWhenFundsAreInsufficientOrPlanetIsActive() {
        val state = GameState(totalDebris = 9_999.0)

        assertNull(GameRules.purchaseOrSelectPlanet(state, "p2", 10_000.0))
        assertNull(GameRules.purchaseOrSelectPlanet(state, "p1", 0.0))
    }

    @Test
    fun activeHotelDebtUsesThirtyPercentOfTapIncome() {
        val result = GameRules.applyHotelDebtPayment(
            totalDebris = 100.0,
            currentDebt = 1_000.0,
            clickIncome = 100.0
        )

        assertEquals(170.0, result.totalDebris, 0.0001)
        assertEquals(970.0, result.remainingDebt, 0.0001)
        assertTrue(result.isDebtActive)
    }

    @Test
    fun finalDebtPaymentKeepsIncomeAndDoesNotClearBalance() {
        val result = GameRules.applyHotelDebtPayment(
            totalDebris = 500.0,
            currentDebt = 10.0,
            clickIncome = 100.0
        )

        assertEquals(590.0, result.totalDebris, 0.0001)
        assertEquals(0.0, result.remainingDebt, 0.0001)
        assertFalse(result.isDebtActive)
    }

    @Test
    fun largeCurrencyValueSurvivesExactSixtyFourBitRoundTrip() {
        val original = 38_146_972_656_250_000.0

        val restored = GameRules.decodeDouble(GameRules.encodeDouble(original))

        assertEquals(original.toRawBits(), restored.toRawBits())
    }

    @Test
    fun blackHoleRewardCoordinatesStayInsideReachableArea() {
        assertEquals(0.05f, GameRules.clampDebrisSpawnCoordinate(-0.4f), 0.0001f)
        assertEquals(0.05f, GameRules.clampDebrisSpawnCoordinate(0f), 0.0001f)
        assertEquals(0.5f, GameRules.clampDebrisSpawnCoordinate(0.5f), 0.0001f)
        assertEquals(0.95f, GameRules.clampDebrisSpawnCoordinate(1f), 0.0001f)
        assertEquals(0.95f, GameRules.clampDebrisSpawnCoordinate(1.4f), 0.0001f)
    }
}
