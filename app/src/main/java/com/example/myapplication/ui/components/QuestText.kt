package com.example.myapplication.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.myapplication.Quest
import com.example.myapplication.QuestType
import com.example.myapplication.R

@Composable
fun localizedQuestDescription(quest: Quest): String = when (quest.type) {
    QuestType.COLLECT_DEBRIS -> stringResource(R.string.quest_collect_debris, quest.target.toInt())
    QuestType.CLICK_PLANET -> stringResource(R.string.quest_click_planet, quest.target.toInt())
    QuestType.BUY_UPGRADE -> stringResource(R.string.quest_buy_upgrade, quest.target.toInt())
    QuestType.OPEN_CASE -> stringResource(R.string.quest_open_case, quest.target.toInt())
    QuestType.COMPLETE_EVENT -> stringResource(R.string.quest_complete_event, quest.target.toInt())
    QuestType.OBTAIN_DRONE -> {
        val number = quest.targetDroneId?.removePrefix("drone_")?.toIntOrNull() ?: 0
        stringResource(R.string.quest_obtain_drone_number, number)
    }
}
