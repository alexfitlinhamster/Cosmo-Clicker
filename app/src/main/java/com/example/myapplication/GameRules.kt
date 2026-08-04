package com.example.myapplication

import kotlin.math.pow

object GameRules {
    private const val CASE_BASE_COST = 1_000.0
    private const val CASE_COST_MULTIPLIER = 1.12
    const val HOTEL_LOAN_AMOUNT = 1_000_000.0
    private const val HOTEL_DEBT_PAYMENT_SHARE = 0.3
    private const val DEBRIS_SPAWN_MARGIN = 0.05f

    data class DebtPayment(
        val totalDebris: Double,
        val remainingDebt: Double,
        val isDebtActive: Boolean
    )

    fun calculateCaseCost(casesPurchased: Int, type: CaseType = CaseType.COMMON): Double =
        CASE_BASE_COST * type.priceMultiplier *
            CASE_COST_MULTIPLIER.pow(casesPurchased.coerceAtLeast(0).toDouble())

    fun caseRarityWeights(type: CaseType): Map<Rarity, Int> = when (type) {
        CaseType.COMMON -> mapOf(Rarity.COMMON to 60, Rarity.UNCOMMON to 25, Rarity.RARE to 10, Rarity.EPIC to 4, Rarity.LEGENDARY to 1)
        CaseType.RARE -> mapOf(Rarity.COMMON to 20, Rarity.UNCOMMON to 35, Rarity.RARE to 28, Rarity.EPIC to 14, Rarity.LEGENDARY to 3)
        CaseType.LEGENDARY -> mapOf(Rarity.COMMON to 5, Rarity.UNCOMMON to 15, Rarity.RARE to 35, Rarity.EPIC to 32, Rarity.LEGENDARY to 13)
    }

    fun rollCaseRarity(type: CaseType, roll: Int): Rarity {
        val safeRoll = roll.coerceIn(0, 99)
        var accumulated = 0
        caseRarityWeights(type).forEach { (rarity, weight) ->
            accumulated += weight
            if (safeRoll < accumulated) return rarity
        }
        return Rarity.COMMON
    }

    fun purchaseOrSelectPlanet(state: GameState, planetId: String, price: Double): GameState? =
        when {
            state.currentPlanetId == planetId -> null
            state.ownedPlanets.contains(planetId) -> state.copy(currentPlanetId = planetId)
            state.totalDebris >= price -> state.copy(
                totalDebris = state.totalDebris - price,
                currentPlanetId = planetId,
                ownedPlanets = state.ownedPlanets + planetId
            )
            else -> null
        }

    fun applyHotelDebtPayment(
        totalDebris: Double,
        currentDebt: Double,
        clickIncome: Double
    ): DebtPayment {
        val debtPayment = minOf(clickIncome * HOTEL_DEBT_PAYMENT_SHARE, currentDebt)
        val remainingDebt = (currentDebt - debtPayment).coerceAtLeast(0.0)
        return DebtPayment(
            totalDebris = totalDebris + clickIncome - debtPayment,
            remainingDebt = remainingDebt,
            isDebtActive = remainingDebt > 0.0
        )
    }

    fun clampDebrisSpawnCoordinate(value: Float): Float =
        value.coerceIn(DEBRIS_SPAWN_MARGIN, 1f - DEBRIS_SPAWN_MARGIN)

    fun encodeDouble(value: Double): Long = value.toRawBits()

    fun decodeDouble(bits: Long): Double = Double.fromBits(bits)
}
