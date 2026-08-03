package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.GameState
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum

@Composable
fun Header(state: GameState, dps: Double, onSettingsClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatNum(state.totalDebris),
                    modifier = Modifier.alignByBaseline(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AppColors.Primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.debris),
                    modifier = Modifier.alignByBaseline(),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            Image(
                painter = painterResource(R.drawable.ui_button_seting),
                contentDescription = stringResource(R.string.settings),
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onSettingsClick() },
                contentScale = ContentScale.Fit
            )
        }
        Text(
            text = stringResource(R.string.per_second, formatNum(dps)),
            color = AppColors.Secondary,
            fontSize = 14.sp
        )

        NextGoalCard(state)

        if (state.isHotelDebtActive) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.hotel_debt), color = AppColors.Danger, fontSize = 12.sp)
                    Text(formatNum(state.currentHotelDebt), color = AppColors.Danger, fontSize = 12.sp)
                }
                LinearProgressIndicator(
                    progress = { (state.currentHotelDebt / 1000000.0).toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = AppColors.Danger,
                    trackColor = Color.White.copy(alpha = 0.1f),
                )
            }
        }
    }
}

@Composable
private fun NextGoalCard(state: GameState) {
    val fleetSize = state.fleetCounts.values.sum()
    val upgradeCount = state.clickLevels.values.sum()
    val dailyQuest = state.activeQuests.firstOrNull { !it.isClaimed && !it.isCompleted }
    val (label, progress) = when {
        state.lifetimeStats.clicks < 100L -> stringResource(R.string.goal_tap_100) to
            (state.lifetimeStats.clicks / 100f).coerceIn(0f, 1f)
        upgradeCount < 1 -> stringResource(R.string.goal_buy_upgrade) to 0f
        fleetSize < 1 -> stringResource(R.string.goal_get_drone) to 0f
        state.ownedPlanets.size < 2 -> stringResource(R.string.goal_unlock_planet) to 0f
        state.lifetimeStats.eventsCompleted < 1 -> stringResource(R.string.goal_complete_event) to 0f
        dailyQuest != null -> localizedQuestDescription(dailyQuest) to
            (dailyQuest.progress / dailyQuest.target).toFloat().coerceIn(0f, 1f)
        else -> stringResource(R.string.goal_all_done) to 1f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(Color.Black.copy(alpha = 0.38f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.next_goal), color = AppColors.Secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("${(progress * 100).toInt()}%", color = Color.LightGray, fontSize = 10.sp)
        }
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(vertical = 5.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = AppColors.Primary,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
    }
}
