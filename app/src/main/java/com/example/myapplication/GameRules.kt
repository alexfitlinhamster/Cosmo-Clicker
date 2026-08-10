package com.example.myapplication

import kotlin.math.pow

object GameRules {
    private const val CASE_BASE_COST = 1_000.0
    private const val CASE_COST_MULTIPLIER = 1.12
    private const val CASE_PRICE_STEP = 20
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
            CASE_COST_MULTIPLIER.pow((casesPurchased.coerceAtLeast(0) / CASE_PRICE_STEP).toDouble())

    fun calculateCaseBundleCost(casesOpened: Int, type: CaseType, count: Int): Double {
        var remaining = count.coerceAtLeast(0)
        var opened = casesOpened.coerceAtLeast(0)
        var total = 0.0
        while (remaining > 0) {
            val inTier = opened % CASE_PRICE_STEP
            val tierSpace = CASE_PRICE_STEP - inTier
            val chunk = minOf(remaining, tierSpace)
            total += calculateCaseCost(opened, type) * chunk
            opened += chunk
            remaining -= chunk
        }
        return total
    }

    fun maxAffordableCases(balance: Double, casesOpened: Int, type: CaseType): Int {
        var remainingBalance = balance.coerceAtLeast(0.0)
        var opened = casesOpened.coerceAtLeast(0)
        var count = 0L
        while (count < Int.MAX_VALUE) {
            val price = calculateCaseCost(opened, type)
            if (!price.isFinite() || remainingBalance < price) break
            val tierSpace = CASE_PRICE_STEP - opened % CASE_PRICE_STEP
            val affordableInTier = (remainingBalance / price).toLong().coerceAtMost(tierSpace.toLong())
            if (affordableInTier <= 0) break
            remainingBalance -= price * affordableInTier
            opened += affordableInTier.toInt()
            count += affordableInTier
        }
        return count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun caseRarityWeights(type: CaseType): Map<Rarity, Int> = when (type) {
        CaseType.COMMON -> mapOf(Rarity.COMMON to 70, Rarity.UNCOMMON to 20, Rarity.RARE to 7, Rarity.EPIC to 2, Rarity.LEGENDARY to 1)
        CaseType.RARE -> mapOf(Rarity.COMMON to 30, Rarity.UNCOMMON to 38, Rarity.RARE to 22, Rarity.EPIC to 8, Rarity.LEGENDARY to 2)
        CaseType.LEGENDARY -> mapOf(Rarity.COMMON to 10, Rarity.UNCOMMON to 22, Rarity.RARE to 38, Rarity.EPIC to 24, Rarity.LEGENDARY to 6)
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
