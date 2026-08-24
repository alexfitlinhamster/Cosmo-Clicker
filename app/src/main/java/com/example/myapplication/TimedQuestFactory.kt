package com.example.myapplication

internal class TimedQuestFactory(private val description: (QuestType, Int) -> String) {
    fun daily(key: Long, planetId: String): List<Quest> = listOf(
        quest("d4_collect_25_$key", QuestType.COLLECT_DEBRIS, 25, 8_000.0, planetId, QuestCadence.DAILY, QuestDifficulty.EASY),
        quest("d4_click_100_$key", QuestType.CLICK_PLANET, 100, 10_000.0, planetId, QuestCadence.DAILY, QuestDifficulty.EASY),
        quest("d4_collect_100_$key", QuestType.COLLECT_DEBRIS, 100, 30_000.0, planetId, QuestCadence.DAILY, QuestDifficulty.MEDIUM),
        quest("d4_click_500_$key", QuestType.CLICK_PLANET, 500, 40_000.0, planetId, QuestCadence.DAILY, QuestDifficulty.MEDIUM),
        quest("d4_events_2_$key", QuestType.COMPLETE_EVENT, 2, 50_000.0, planetId, QuestCadence.DAILY, QuestDifficulty.MEDIUM)
    )

    fun weekly(key: Long, planetId: String): List<Quest> = listOf(
        quest("w4_click_5000_$key", QuestType.CLICK_PLANET, 5_000, 750_000.0, planetId, QuestCadence.WEEKLY, QuestDifficulty.HARD),
        quest("w4_rare_drone_$key", QuestType.OBTAIN_RARE_DRONE, 1, 1_000_000.0, planetId, QuestCadence.WEEKLY, QuestDifficulty.HARD),
        quest("w4_debris_$key", QuestType.COLLECT_DEBRIS, 2_000_000, 1_500_000.0, planetId, QuestCadence.WEEKLY, QuestDifficulty.HARD)
    )

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
