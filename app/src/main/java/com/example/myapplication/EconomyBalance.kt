package com.example.myapplication

import kotlin.math.pow

object EconomyBalance {
    const val MAX_DRONES = 10
    const val PRESTIGE_PLANET_INDEX = 10
    private const val FIRST_PLANET_PRICE = 10_000.0

    fun planetIndex(planetId: String): Int =
        planetId.removePrefix("p").toIntOrNull()?.coerceIn(1, 20) ?: 1

    fun planetPrice(index: Int): Double =
        if (index <= 1) 0.0 else FIRST_PLANET_PRICE * 3.0.pow((index - 2).toDouble())

    fun planetIncomeMultiplier(planetId: String): Double =
        1.5.pow((planetIndex(planetId) - 1).toDouble())

    fun passiveIncome(
        state: GameState,
        fleetById: Map<String, FleetConfig>,
        nowMillis: Long = System.currentTimeMillis()
    ): Double {
        val incomeFleet = state.activeFleetCounts.ifEmpty { state.fleetCounts }
        val base = incomeFleet.entries.sumOf { (id, count) ->
            count.coerceAtLeast(0) * when (fleetById[id]?.rarity ?: Rarity.COMMON) {
                Rarity.COMMON -> 5.0
                Rarity.UNCOMMON -> 15.0
                Rarity.RARE -> 50.0
                Rarity.EPIC -> 200.0
                Rarity.LEGENDARY -> 1_000.0
                Rarity.VOID -> 2_500.0
            }
        }
        val tradeMultiplier = if ((state.activeEffects[SkillType.TRADE_POWER.id] ?: 0L) > nowMillis) 2.0 else 1.0
        val fleetBoost = if ((state.activeEffects[SkillType.TRADE_FLEET_BOOST.id] ?: 0L) > nowMillis) 3.0 else 1.0
        return base * planetIncomeMultiplier(state.currentPlanetId) *
            MetaProgressEngine.technologyMultiplier(state.technologies) *
            MetaProgressEngine.masteryMultiplier(state.droneParts) *
            DroneTraitEngine.modifiers(state.activeFleetCounts).passiveMultiplier * tradeMultiplier * fleetBoost
    }

    fun scaledReward(base: Double, planetId: String): Double =
        base.coerceAtLeast(0.0) * planetIncomeMultiplier(planetId)

    fun canPrestige(state: GameState): Boolean =
        state.ownedPlanets.any { planetIndex(it) >= PRESTIGE_PLANET_INDEX }

    fun prestigeReward(state: GameState): Int {
        val highest = state.ownedPlanets.maxOfOrNull(::planetIndex) ?: 1
        return (highest - PRESTIGE_PLANET_INDEX + 1).coerceAtLeast(1)
    }
}
