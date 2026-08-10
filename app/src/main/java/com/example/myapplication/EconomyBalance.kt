package com.example.myapplication

import kotlin.math.pow
import kotlin.math.floor
import kotlin.math.log10

object EconomyBalance {
    const val MAX_DRONES = Int.MAX_VALUE
    const val MAX_CLICK_UPGRADE_LEVEL = 20
    const val MAX_CLICK_UPGRADE_COST = 5_000.0
    const val PRESTIGE_PLANET_INDEX = 10
    private const val FIRST_PLANET_PRICE = 10_000.0
    private const val MAX_PLANET_PRICE = 1_000_000_000.0

    fun planetIndex(planetId: String): Int =
        planetId.removePrefix("p").toIntOrNull()?.coerceIn(1, 20) ?: 1

    fun planetPrice(index: Int): Double =
        if (index <= 1) 0.0 else (FIRST_PLANET_PRICE * 1.9.pow((index - 2).toDouble()))
            .coerceAtMost(MAX_PLANET_PRICE)

    fun planetIncomeMultiplier(planetId: String): Double =
        1.10.pow((planetIndex(planetId) - 1).toDouble())

    fun clickUpgradeCost(base: Double, level: Int, marketMultiplier: Double = 1.0): Double =
        (base.coerceAtLeast(0.0) * 1.15.pow(level.coerceAtLeast(0).toDouble()) * marketMultiplier.coerceAtLeast(0.0))
            .coerceAtMost(MAX_CLICK_UPGRADE_COST)
            .toLong().toDouble()

    fun passiveIncome(
        state: GameState,
        fleetById: Map<String, FleetConfig>,
        nowMillis: Long = System.currentTimeMillis()
    ): Double {
        val incomeFleet = state.activeFleetCounts.ifEmpty { state.fleetCounts }
        val base = incomeFleet.entries.sumOf { (id, count) ->
            count.coerceAtLeast(0) * when (fleetById[id]?.rarity ?: Rarity.COMMON) {
                Rarity.COMMON -> 1.0
                Rarity.UNCOMMON -> 3.0
                Rarity.RARE -> 10.0
                Rarity.EPIC -> 40.0
                Rarity.LEGENDARY -> 150.0
                Rarity.VOID -> 400.0
            }
        }
        val tradeMultiplier = if ((state.activeEffects[SkillType.TRADE_POWER.id] ?: 0L) > nowMillis) 2.0 else 1.0
        val fleetBoost = if ((state.activeEffects[SkillType.TRADE_FLEET_BOOST.id] ?: 0L) > nowMillis) 3.0 else 1.0
        return base * planetIncomeMultiplier(state.currentPlanetId) *
            MetaProgressEngine.technologyMultiplier(state.technologies) *
            MetaProgressEngine.masteryMultiplier(state.droneParts) *
            DroneTraitEngine.modifiers(state.activeFleetCounts).passiveMultiplier * tradeMultiplier * fleetBoost
    }

    fun scaledReward(base: Double, planetId: String): Double {
        val progressionMultiplier = 1.32.pow((planetIndex(planetId) - 1).toDouble())
        return roundReward(base.coerceAtLeast(0.0) * progressionMultiplier)
    }

    fun roundReward(value: Double): Double {
        if (!value.isFinite() || value <= 0.0) return 0.0
        val magnitude = 10.0.pow(floor(log10(value)))
        val step = when {
            value / magnitude < 2.0 -> magnitude / 20.0
            value / magnitude < 5.0 -> magnitude / 10.0
            else -> magnitude / 5.0
        }.coerceAtLeast(1.0)
        return kotlin.math.round(value / step) * step
    }

    fun canPrestige(state: GameState): Boolean =
        state.ownedPlanets.any { planetIndex(it) >= PRESTIGE_PLANET_INDEX }

    fun prestigeReward(state: GameState): Int {
        val highest = state.ownedPlanets.maxOfOrNull(::planetIndex) ?: 1
        return (highest - PRESTIGE_PLANET_INDEX + 1).coerceAtLeast(1)
    }
}
