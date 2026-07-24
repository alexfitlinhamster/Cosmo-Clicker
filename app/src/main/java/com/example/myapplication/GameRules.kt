package com.example.myapplication

import kotlin.math.pow

object GameRules {
    private const val CASE_BASE_COST = 1_000.0
    private const val CASE_COST_MULTIPLIER = 1.2
    const val HOTEL_LOAN_AMOUNT = 1_000_000.0
    private const val HOTEL_DEBT_PAYMENT_SHARE = 0.3
    private const val DEBRIS_SPAWN_MARGIN = 0.05f

    data class DebtPayment(
        val totalDebris: Double,
        val remainingDebt: Double,
        val isDebtActive: Boolean
    )

    fun calculateCaseCost(casesPurchased: Int): Double =
        CASE_BASE_COST * CASE_COST_MULTIPLIER.pow(casesPurchased.coerceAtLeast(0).toDouble())

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
