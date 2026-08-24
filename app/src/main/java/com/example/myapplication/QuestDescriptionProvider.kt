package com.example.myapplication

import android.content.Context

class QuestDescriptionProvider(private val context: Context) {
    fun describe(type: QuestType, target: Int): String = context.getString(
        when (type) {
            QuestType.COLLECT_DEBRIS -> R.string.quest_collect_debris
            QuestType.CLICK_PLANET -> R.string.quest_click_planet
            QuestType.BUY_UPGRADE -> R.string.quest_buy_upgrade
            QuestType.OPEN_CASE -> R.string.quest_open_case
            QuestType.COMPLETE_EVENT -> R.string.quest_complete_event
            QuestType.OBTAIN_DRONE -> R.string.quest_obtain_any_drone
            QuestType.OBTAIN_RARE_DRONE -> R.string.quest_obtain_rare_drone
        },
        target
    )
}
