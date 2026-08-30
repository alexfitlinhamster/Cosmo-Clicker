package com.example.myapplication

import java.util.Calendar

data class DailyReward(val day: Int, val debris: Double, val prestigePoints: Int)

object DailyRewardEngine {
    fun dayKey(now: Long = System.currentTimeMillis()): Long = Calendar.getInstance().run {
        timeInMillis = now
        get(Calendar.YEAR) * 1_000L + get(Calendar.DAY_OF_YEAR)
    }

    fun canClaim(state: GameState, now: Long = System.currentTimeMillis()): Boolean =
        state.lastDailyRewardDay != dayKey(now)

    fun preview(state: GameState, now: Long = System.currentTimeMillis()): DailyReward {
        val today = dayKey(now)
        val continued = state.lastDailyRewardDay == previousDayKey(now)
        val day = if (continued) state.dailyRewardStreak % 7 + 1 else 1
        return DailyReward(
            day = day,
            debris = EconomyBalance.scaledReward(2_500.0 * day * day, state.currentPlanetId),
            prestigePoints = if (day == 7) 1 else 0
        )
    }

    fun claim(state: GameState, now: Long = System.currentTimeMillis()): GameState? {
        if (!canClaim(state, now)) return null
        val reward = preview(state, now)
        return state.copy(
            totalDebris = state.totalDebris + reward.debris,
            prestigePoints = state.prestigePoints + reward.prestigePoints,
            lastDailyRewardDay = dayKey(now),
            dailyRewardStreak = reward.day
        )
    }

    private fun previousDayKey(now: Long): Long = dayKey(now - 86_400_000L)
}

object OverallProgressEngine {
    data class Category(
        val id: String,
        val completed: Int,
        val total: Int
    ) {
        val fraction: Float
            get() = (completed.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)

        val percent: Int
            get() = (fraction * 100f).toInt()
    }

    fun categories(state: GameState, upgradeIds: Collection<String>): List<Category> = listOf(
        Category(
            id = "planets",
            completed = state.ownedPlanets.count {
                EconomyBalance.planetIndex(it) <= EconomyBalance.MAX_PLANET_INDEX
            },
            total = EconomyBalance.MAX_PLANET_INDEX
        ),
        Category(
            id = "upgrades",
            completed = upgradeIds.count { (state.clickLevels[it] ?: 0) > 0 },
            total = upgradeIds.size
        ),
        Category(
            id = "achievements",
            completed = state.claimedAchievementIds.size,
            total = AchievementEngine.definitions.size
        ),
        Category(
            id = "technologies",
            completed = state.technologies.size,
            total = Technology.entries.size
        ),
        Category(
            id = "station",
            completed = state.stationLevels.values.sum().coerceAtMost(StationModule.entries.size * 5),
            total = StationModule.entries.size * 5
        )
    )

    fun fraction(state: GameState, upgradeIds: Collection<String>): Float {
        val categories = categories(state, upgradeIds)
        return categories.map { it.fraction }.average().toFloat().coerceIn(0f, 1f)
    }

    fun percent(state: GameState, upgradeIds: Collection<String>): Int =
        (fraction(state, upgradeIds) * 100f).toInt()
}

data class GalacticCollectionProgress(
    val planets: Int,
    val totalPlanets: Int,
    val eventTypes: Int,
    val totalEventTypes: Int,
    val achievements: Int,
    val totalAchievements: Int,
    val droneDiscoveries: Int,
    val totalDrones: Int
) {
    val discoveredEntries: Int
        get() = planets + eventTypes + achievements + droneDiscoveries

    val totalEntries: Int
        get() = totalPlanets + totalEventTypes + totalAchievements + totalDrones

    val fraction: Float
        get() = (discoveredEntries.toFloat() / totalEntries.coerceAtLeast(1)).coerceIn(0f, 1f)
}

object GalacticCollectionEngine {
    fun progress(state: GameState, totalDrones: Int): GalacticCollectionProgress =
        GalacticCollectionProgress(
            planets = state.ownedPlanets.count {
                EconomyBalance.planetIndex(it) <= EconomyBalance.MAX_PLANET_INDEX
            },
            totalPlanets = EconomyBalance.MAX_PLANET_INDEX,
            eventTypes = state.encounteredEventTypes.size,
            totalEventTypes = GameEventType.entries.size,
            achievements = state.claimedAchievementIds.size,
            totalAchievements = AchievementEngine.definitions.size,
            droneDiscoveries = state.discoveredDroneIds.size.coerceAtMost(totalDrones),
            totalDrones = totalDrones
        )
}
