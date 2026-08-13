package com.example.myapplication

import kotlin.math.sqrt

enum class Technology(val cost: Int) {
    POWER_CORE(5), OFFLINE_AI(12), LUCK_MATRIX(25)
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
        if (Technology.POWER_CORE in technologies) 1.15 else 1.0
}
