package com.example.myapplication

object QuestEngine {
    fun advance(
        quests: List<Quest>,
        type: QuestType,
        amount: Double = 1.0,
        droneId: String? = null,
        droneRarity: Rarity? = null
    ): List<Quest> =
        quests.map { quest ->
            if (quest.type != type || quest.isCompleted || quest.isClaimed) return@map quest
            if (type == QuestType.OBTAIN_DRONE && quest.targetDroneId != droneId) return@map quest
            if (type == QuestType.OBTAIN_RARE_DRONE && (droneRarity == null || droneRarity.ordinal < Rarity.RARE.ordinal)) return@map quest
            val progress = (quest.progress + amount).coerceAtMost(quest.target)
            quest.copy(progress = progress, isCompleted = progress >= quest.target)
        }
}
