package com.example.myapplication

object QuestEngine {
    fun advance(quests: List<Quest>, type: QuestType, amount: Double = 1.0): List<Quest> =
        quests.map { quest ->
            if (quest.type != type || quest.isCompleted || quest.isClaimed) return@map quest
            val progress = (quest.progress + amount).coerceAtMost(quest.target)
            quest.copy(progress = progress, isCompleted = progress >= quest.target)
        }
}
