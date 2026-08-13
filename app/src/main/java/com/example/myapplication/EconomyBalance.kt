package com.example.myapplication

import kotlin.math.pow
import kotlin.math.floor
import kotlin.math.log10

object EconomyBalance {
    const val MAX_DRONES = Int.MAX_VALUE
    const val MAX_CLICK_UPGRADE_LEVEL = 1_000
    const val MAX_CLICK_UPGRADE_COST = 2_500_000.0
    const val PRESTIGE_PLANET_INDEX = 10
    private const val FIRST_PLANET_PRICE = 10_000.0
    const val MAX_PLANET_INDEX = 39

    fun planetIndex(planetId: String): Int =
        planetId.removePrefix("p").toIntOrNull()?.coerceIn(1, MAX_PLANET_INDEX) ?: 1

    fun planetPrice(index: Int): Double =
        if (index <= 1) 0.0 else FIRST_PLANET_PRICE * 1.9.pow((index - 2).toDouble())

    fun planetIncomeMultiplier(planetId: String): Double =
        1.10.pow((planetIndex(planetId) - 1).toDouble())

    fun planetClickSpecial(planetId: String): Double = when (planetId) {
        "p25" -> 1.25; "p31" -> 1.60; "p34" -> 1.90; "p37" -> 2.25; "p39" -> 3.0
        else -> 1.0
    }

    fun planetFleetSpecial(planetId: String): Double = when (planetId) {
        "p26" -> 1.30; "p30" -> 1.55; "p33" -> 1.80; "p36" -> 2.10; "p39" -> 3.0
        else -> 1.0
    }

    fun planetSalvageSpecial(planetId: String): Double = when (planetId) {
        "p27" -> 1.40; "p28" -> 1.45; "p29" -> 1.35; "p32" -> 1.50
        "p35" -> 1.70; "p38" -> 2.0
        else -> 1.0
    }

    fun nextPlanetIndex(ownedPlanets: Set<String>): Int? =
        (2..MAX_PLANET_INDEX).firstOrNull { index -> "p$index" !in ownedPlanets }

    /** Every stage asks the player to build a fleet; the requirement grows slowly. */
    fun requiredActiveDronesForPlanet(index: Int): Int = when {
        index <= 1 -> 0
        else -> (1 + (index - 2) / 6).coerceAtMost(7)
    }

    fun requiredDroneIdForPlanet(index: Int): String? = when (index) {
        2 -> "drone_1"
        5 -> "drone_5"
        9 -> "drone_9"
        13 -> "drone_13"
        17 -> "drone_17"
        21 -> "drone_21"
        25 -> "drone_25"
        29 -> "drone_29"
        else -> null
    }

    fun requiredPrestigeForPlanet(index: Int): Int = if (index >= 11) 1 else 0

    fun planetFleetObjectiveMet(state: GameState, index: Int): Boolean {
        val requiredDrone = requiredDroneIdForPlanet(index)
        return state.activeFleetCounts.values.sum() >= requiredActiveDronesForPlanet(index) &&
            (requiredDrone == null || requiredDrone in state.discoveredDroneIds) &&
            state.lifetimeStats.prestiges >= requiredPrestigeForPlanet(index)
    }

    fun planetUnlockProgress(balance: Double, price: Double): Float =
        if (price <= 0.0) 1f else (balance / price).toFloat().coerceIn(0f, 1f)

    fun clickUpgradeCost(base: Double, level: Int, marketMultiplier: Double = 1.0): Double =
        (base.coerceAtLeast(0.0) * 10.0 * 1.02.pow(level.coerceAtLeast(0).toDouble()) * marketMultiplier.coerceAtLeast(0.0))
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
                Rarity.COMMON -> 5.0
                Rarity.UNCOMMON -> 20.0
                Rarity.RARE -> 100.0
                Rarity.EPIC -> 500.0
                Rarity.LEGENDARY -> 2_500.0
                Rarity.VOID -> 10_000.0
            }
        }
        val tradeMultiplier = if ((state.activeEffects[SkillType.TRADE_POWER.id] ?: 0L) > nowMillis) 2.0 else 1.0
        val fleetBoost = if ((state.activeEffects[SkillType.TRADE_FLEET_BOOST.id] ?: 0L) > nowMillis) 3.0 else 1.0
        val planetSpecialMultiplier = when (state.currentPlanetId) {
            "p21" -> 1.35
            "p24" -> 2.0
            else -> 1.0
        }
        return base * planetIncomeMultiplier(state.currentPlanetId) * planetSpecialMultiplier * planetFleetSpecial(state.currentPlanetId) *
            MetaProgressEngine.technologyMultiplier(state.technologies) *
            MetaProgressEngine.masteryMultiplier(state.droneParts) *
            MetaProgressEngine.collectionSetMultiplier(state.discoveredDroneIds) *
            DroneTraitEngine.modifiers(state.activeFleetCounts).passiveMultiplier * tradeMultiplier * fleetBoost
    }

    fun scaledReward(base: Double, planetId: String): Double {
        val progressionMultiplier = 1.32.pow((planetIndex(planetId) - 1).toDouble())
        return roundReward(base.coerceAtLeast(0.0) * progressionMultiplier * planetSalvageSpecial(planetId))
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
