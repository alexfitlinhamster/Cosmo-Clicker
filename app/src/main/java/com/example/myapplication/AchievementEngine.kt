package com.example.myapplication

data class LifetimeStats(
    val clicks: Long = 0L,
    val debrisCollected: Long = 0L,
    val casesOpened: Int = 0,
    val eventsCompleted: Int = 0,
    val prestiges: Int = 0
)

data class AchievementDefinition(
    val id: String,
    val rewardDebris: Double = 0.0,
    val rewardPrestigePoints: Int = 0,
    val isUnlocked: (GameState) -> Boolean
)

object AchievementEngine {
    val definitions = listOf(
        AchievementDefinition("click_100", rewardDebris = 5_000.0) { it.lifetimeStats.clicks >= 100 },
        AchievementDefinition("click_10000", rewardDebris = 250_000.0) { it.lifetimeStats.clicks >= 10_000 },
        AchievementDefinition("fleet_5", rewardDebris = 25_000.0) { it.fleetCounts.values.sum() >= 5 },
        AchievementDefinition("fleet_12", rewardPrestigePoints = 1) { it.fleetCounts.values.sum() >= 12 },
        AchievementDefinition("planets_5", rewardDebris = 100_000.0) { it.ownedPlanets.size >= 5 },
        AchievementDefinition("planets_10", rewardPrestigePoints = 1) { it.ownedPlanets.size >= 10 },
        AchievementDefinition("planets_20", rewardPrestigePoints = 3) { it.ownedPlanets.size >= 20 },
        AchievementDefinition("events_10", rewardDebris = 100_000.0) { it.lifetimeStats.eventsCompleted >= 10 },
        AchievementDefinition("prestige_1", rewardPrestigePoints = 1) { it.lifetimeStats.prestiges >= 1 }
    )

    private val byId = definitions.associateBy(AchievementDefinition::id)

    fun evaluate(state: GameState): GameState {
        val newlyUnlocked = definitions.asSequence()
            .filter { it.id !in state.unlockedAchievementIds && it.isUnlocked(state) }
            .map(AchievementDefinition::id)
            .toSet()
        return if (newlyUnlocked.isEmpty()) state
        else state.copy(unlockedAchievementIds = state.unlockedAchievementIds + newlyUnlocked)
    }

    fun claim(state: GameState, achievementId: String): GameState? {
        val achievement = byId[achievementId] ?: return null
        if (achievementId !in state.unlockedAchievementIds ||
            achievementId in state.claimedAchievementIds
        ) return null
        return state.copy(
            totalDebris = state.totalDebris + EconomyBalance.scaledReward(
                achievement.rewardDebris,
                state.currentPlanetId
            ),
            prestigePoints = state.prestigePoints + achievement.rewardPrestigePoints,
            claimedAchievementIds = state.claimedAchievementIds + achievementId
        )
    }
}
