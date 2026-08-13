package com.example.myapplication

object CaseController {
    fun cost(casesPurchased: Int, type: CaseType = CaseType.COMMON): Double =
        GameRules.calculateCaseCost(casesPurchased, type)

    fun bundleCost(casesPurchased: Int, type: CaseType, count: Int): Double =
        GameRules.calculateCaseBundleCost(casesPurchased, type, count)

    fun maxAffordable(balance: Double, casesPurchased: Int, type: CaseType): Int =
        GameRules.maxAffordableCases(balance, casesPurchased, type)

    fun startOpening(state: GameState, type: CaseType, count: Int): GameState? {
        val safeCount = count.coerceAtLeast(1)
        val price = bundleCost(state.casesPurchased, type, safeCount)
        if (state.isOpeningCase || state.lastDroppedDroneId != null || state.totalDebris < price) return null
        return state.copy(
            totalDebris = state.totalDebris - price,
            isOpeningCase = true,
            openingCaseType = type,
            pendingCaseOpenings = safeCount - 1,
            caseBundleRewards = emptyMap(),
            showCaseBundleSummary = false,
            lastDroppedDroneId = null
        )
    }
}
