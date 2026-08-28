package com.example.myapplication

internal class TimedQuestFactory(private val description: (QuestType, Int) -> String) {
    companion object {
        const val DAILY_ID_PREFIX = "d5_"
        const val WEEKLY_ID_PREFIX = "w5_"
    }

    fun daily(key: Long, planetId: String): List<Quest> {
        val tier = EconomyBalance.planetIndex(planetId)
        val collectEasy = 40 + tier * 10
        val collectMedium = 150 + tier * 30
        return listOf(
            quest("${DAILY_ID_PREFIX}collect_easy_$key", QuestType.COLLECT_DEBRIS, collectEasy, 8_000.0, planetId, QuestCadence.DAILY, QuestDifficulty.EASY),
            quest("${DAILY_ID_PREFIX}click_100_$key", QuestType.CLICK_PLANET, 100, 10_000.0, planetId, QuestCadence.DAILY, QuestDifficulty.EASY),
            quest("${DAILY_ID_PREFIX}collect_medium_$key", QuestType.COLLECT_DEBRIS, collectMedium, 30_000.0, planetId, QuestCadence.DAILY, QuestDifficulty.MEDIUM),
            quest("${DAILY_ID_PREFIX}case_$key", QuestType.OPEN_CASE, 1, 35_000.0, planetId, QuestCadence.DAILY, QuestDifficulty.MEDIUM),
            quest("${DAILY_ID_PREFIX}event_$key", QuestType.COMPLETE_EVENT, 1, 50_000.0, planetId, QuestCadence.DAILY, QuestDifficulty.MEDIUM)
        )
    }

    fun weekly(key: Long, planetId: String): List<Quest> {
        val tier = EconomyBalance.planetIndex(planetId)
        val debrisTarget = (2_000 + tier * 750).coerceAtMost(30_000)
        return listOf(
            quest("${WEEKLY_ID_PREFIX}click_2500_$key", QuestType.CLICK_PLANET, 2_500, 500_000.0, planetId, QuestCadence.WEEKLY, QuestDifficulty.HARD),
            quest("${WEEKLY_ID_PREFIX}rare_drone_$key", QuestType.OBTAIN_RARE_DRONE, 1, 750_000.0, planetId, QuestCadence.WEEKLY, QuestDifficulty.HARD),
            quest("${WEEKLY_ID_PREFIX}debris_$key", QuestType.COLLECT_DEBRIS, debrisTarget, 900_000.0, planetId, QuestCadence.WEEKLY, QuestDifficulty.HARD)
        )
    }

    private fun quest(id: String, type: QuestType, target: Int, baseReward: Double, planetId: String, cadence: QuestCadence, difficulty: QuestDifficulty) = Quest(
        id = id,
        type = type,
        description = description(type, target),
        target = target.toDouble(),
        progress = 0.0,
        rewardDebris = EconomyBalance.scaledReward(baseReward, planetId),
        cadence = cadence,
        difficulty = difficulty
    )
}
