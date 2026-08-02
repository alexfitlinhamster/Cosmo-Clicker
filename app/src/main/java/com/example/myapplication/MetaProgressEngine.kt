package com.example.myapplication

import kotlin.math.sqrt

enum class Technology(val cost: Int) {
    POWER_CORE(1), OFFLINE_AI(2), LUCK_MATRIX(3)
}

data class SessionStats(
    val startedAt: Long = System.currentTimeMillis(),
    val clicks: Long = 0,
    val debrisEarned: Double = 0.0,
    val casesOpened: Int = 0
)

object MetaProgressEngine {
    fun prestigeReward(totalDebris: Double): Int =
        sqrt((totalDebris.coerceAtLeast(0.0) / 1_000_000_000.0)).toInt().coerceAtLeast(1)

    fun collectionMultiplier(fleetCounts: Map<String, Int>, fleetById: Map<String, FleetConfig>): Double {
        val ownedRarities = fleetCounts.filterValues { it > 0 }.keys.mapNotNull { fleetById[it]?.rarity }.toSet()
        return 1.0 + ownedRarities.size * 0.05
    }

    fun technologyMultiplier(technologies: Set<Technology>): Double =
        if (Technology.POWER_CORE in technologies) 1.25 else 1.0
}
