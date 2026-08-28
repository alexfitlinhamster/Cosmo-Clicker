package com.example.myapplication

import kotlin.math.sqrt

enum class Technology(val cost: Int) {
    POWER_CORE(1), OFFLINE_AI(3), LUCK_MATRIX(6)
}

data class SessionStats(
    val startedAt: Long = System.currentTimeMillis(),
    val clicks: Long = 0,
    val debrisEarned: Double = 0.0,
    val casesOpened: Int = 0
)

data class DroneCollectionSet(
    val id: String,
    val droneIds: Set<String>,
    val bonusPercent: Int
)

object MetaProgressEngine {
    private val masteryThresholds = intArrayOf(3, 8, 15)

    val collectionSets = listOf(
        DroneCollectionSet("first_expedition", (1..5).mapTo(mutableSetOf()) { "drone_$it" }, 3),
        DroneCollectionSet("emerald_squadron", (6..10).mapTo(mutableSetOf()) { "drone_$it" }, 3),
        DroneCollectionSet("crimson_corps", (11..15).mapTo(mutableSetOf()) { "drone_$it" }, 4),
        DroneCollectionSet("cyber_swarm", (16..20).mapTo(mutableSetOf()) { "drone_$it" }, 4),
        DroneCollectionSet("stellar_guard", (21..24).mapTo(mutableSetOf()) { "drone_$it" }, 5),
        DroneCollectionSet("quantum_edge", (25..29).mapTo(mutableSetOf()) { "drone_$it" }, 6)
    )

    fun prestigeReward(totalDebris: Double): Int =
        sqrt((totalDebris.coerceAtLeast(0.0) / 1_000_000_000.0)).toInt().coerceAtLeast(1)

    /** Starts a fresh run while preserving all permanent meta progression. */
    fun prestige(state: GameState): GameState? {
        if (!EconomyBalance.canPrestige(state)) return null
        val reward = EconomyBalance.prestigeReward(state)
        return QuestEngine.reset(
            GameState(
                prestigePoints = state.prestigePoints + reward,
                technologies = state.technologies,
                lifetimeStats = state.lifetimeStats.copy(prestiges = state.lifetimeStats.prestiges + 1),
                unlockedAchievementIds = state.unlockedAchievementIds,
                claimedAchievementIds = state.claimedAchievementIds
            )
        )
    }

    fun buyTechnology(state: GameState, technology: Technology): GameState? {
        if (technology in state.technologies || state.prestigePoints < technology.cost) return null
        return state.copy(
            prestigePoints = state.prestigePoints - technology.cost,
            technologies = state.technologies + technology
        )
    }

    fun collectionMultiplier(fleetCounts: Map<String, Int>, fleetById: Map<String, FleetConfig>): Double {
        val ownedRarities = fleetCounts.filterValues { it > 0 }.keys.mapNotNull { fleetById[it]?.rarity }.toSet()
        return 1.0 + ownedRarities.size * 0.05
    }

    fun completedCollectionSets(discoveredDroneIds: Set<String>): List<DroneCollectionSet> =
        collectionSets.filter { discoveredDroneIds.containsAll(it.droneIds) }

    fun collectionSetMultiplier(discoveredDroneIds: Set<String>): Double =
        1.0 + completedCollectionSets(discoveredDroneIds).sumOf { it.bonusPercent } / 100.0

    fun masteryLevel(parts: Int): Int = masteryThresholds.count { parts >= it }

    fun masteryMultiplier(droneParts: Map<String, Int>): Double =
        1.0 + droneParts.values.sumOf(::masteryLevel) * 0.02

    fun partsForNextLevel(parts: Int): Int? = masteryThresholds.firstOrNull { parts < it }

    fun technologyMultiplier(technologies: Set<Technology>): Double =
        if (Technology.POWER_CORE in technologies) 1.25 else 1.0
}
