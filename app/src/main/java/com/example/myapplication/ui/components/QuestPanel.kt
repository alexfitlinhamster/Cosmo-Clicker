package com.example.myapplication.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.GameState
import com.example.myapplication.Quest
import com.example.myapplication.QuestCadence
import com.example.myapplication.QuestDifficulty
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum

@Composable
fun QuestPanel(
    state: GameState,
    onClaim: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .fillMaxHeight(0.72f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(Color(0xFF0B1A2C), Color(0xFF07101E))))
                .padding(18.dp)
        ) {
            SpaceSheetHeader(
                title = stringResource(R.string.quests),
                subtitle = stringResource(R.string.missions_subtitle),
                onClose = onClose
            )
            Spacer(Modifier.height(14.dp))

            if (state.activeQuests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_active_quests), color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val daily = state.activeQuests.filter { it.cadence == QuestCadence.DAILY }
                    val weekly = state.activeQuests.filter { it.cadence == QuestCadence.WEEKLY }
                    item(key = "daily_header") {
                        QuestSectionHeader(R.string.daily_quests, QuestCadence.DAILY)
                    }
                    items(daily, key = { it.id }) { quest ->
                        QuestItemRow(quest, onClaim)
                    }
                    item(key = "weekly_header") {
                        QuestSectionHeader(R.string.weekly_quests, QuestCadence.WEEKLY)
                    }
                    items(weekly, key = { it.id }) { quest ->
                        QuestItemRow(quest, onClaim)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestSectionHeader(titleRes: Int, cadence: QuestCadence) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(cadence) {
        while (true) {
            kotlinx.coroutines.delay(1_000L)
            nowMillis = System.currentTimeMillis()
        }
    }
    val left = (nextQuestResetAt(nowMillis, cadence) - nowMillis).coerceAtLeast(0L)
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(titleRes), color = AppColors.Secondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            QuestResetTimer(formatDuration(left))
        }
    }
}

@Composable
private fun QuestResetTimer(time: String) {
    Row(
        modifier = Modifier
            .background(Color(0xFF09182B).copy(alpha = 0.92f), RoundedCornerShape(12.dp))
            .border(1.dp, AppColors.Primary.copy(alpha = 0.38f), RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CosmicClock(Modifier.size(18.dp))
        Text(
            text = time,
            color = AppColors.Warning,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.25.sp
        )
    }
}

@Composable
private fun CosmicClock(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.31f
        drawCircle(Color(0xFF102A45), radius * 1.08f, center)
        drawCircle(AppColors.Primary.copy(alpha = 0.9f), radius, center, style = Stroke(width = 1.4f))
        drawLine(AppColors.Secondary, center, Offset(center.x, center.y - radius * 0.58f), strokeWidth = 1.4f)
        drawLine(AppColors.Warning, center, Offset(center.x + radius * 0.48f, center.y + radius * 0.22f), strokeWidth = 1.4f)
        drawCircle(Color.White, radius * 0.13f, center)
        drawArc(
            color = Color(0xFF8D6BFF).copy(alpha = 0.8f),
            startAngle = 205f,
            sweepAngle = 250f,
            useCenter = false,
            style = Stroke(width = 1f)
        )
        drawCircle(AppColors.Warning, size.minDimension * 0.075f, Offset(size.width * 0.82f, size.height * 0.25f))
    }
}

internal fun nextQuestResetAt(nowMillis: Long, cadence: QuestCadence): Long {
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)
    if (cadence == QuestCadence.DAILY) {
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
    } else {
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val firstDay = calendar.firstDayOfWeek
        val daysUntilNextWeek = ((firstDay - currentDay + 7) % 7).let { if (it == 0) 7 else it }
        calendar.add(java.util.Calendar.DAY_OF_YEAR, daysUntilNextWeek)
    }
    return calendar.timeInMillis
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1_000L
    val days = totalSeconds / (24L * 60L * 60L)
    val hours = (totalSeconds / (60L * 60L)) % 24L
    val minutes = (totalSeconds / 60L) % 60L
    val seconds = totalSeconds % 60L
    return if (days > 0L) {
        "%dd %02d:%02d:%02d".format(days, hours, minutes, seconds)
    } else {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}

@Composable
fun QuestItemRow(quest: Quest, onClaim: (String) -> Unit) {
    val progress = (quest.progress / quest.target).toFloat().coerceIn(0f, 1f)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        (if (quest.isCompleted) AppColors.Primary else Color.White).copy(alpha = .11f),
                        Color.White.copy(alpha = .035f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .border(1.dp, if (quest.isCompleted) AppColors.Primary.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val difficultyColor = when (quest.difficulty) {
                QuestDifficulty.EASY -> Color(0xFF66BB6A)
                QuestDifficulty.MEDIUM -> AppColors.Warning
                QuestDifficulty.HARD -> Color(0xFFEF5350)
            }
            val difficultyLabel = when (quest.difficulty) {
                QuestDifficulty.EASY -> R.string.quest_difficulty_easy
                QuestDifficulty.MEDIUM -> R.string.quest_difficulty_medium
                QuestDifficulty.HARD -> R.string.quest_difficulty_hard
            }
            Text(
                stringResource(difficultyLabel),
                color = difficultyColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(localizedQuestDescription(quest), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (quest.isCompleted) AppColors.Primary else difficultyColor,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${formatNum(quest.progress)} / ${formatNum(quest.target)}",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
                if (quest.rewardPrestigePoints > 0) {
                    Text(
                        stringResource(R.string.reward_prestige_points, quest.rewardPrestigePoints),
                        color = AppColors.Warning,
                        fontSize = 10.sp
                    )
                } else if (quest.rewardDebris > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_currency_debris_v2), null, Modifier.size(16.dp), tint = Color.Unspecified)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.reward_debris, formatNum(quest.rewardDebris)), color = AppColors.Primary, fontSize = 10.sp)
                    }
                } else if (quest.rewardCases > 0) {
                    Text(
                        stringResource(R.string.reward_cases, quest.rewardCases),
                        color = AppColors.Secondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
        
        if (quest.isCompleted) {
            Button(
                onClick = { onClaim(quest.id) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                style = CosmicButtonStyle.Reward
            ) {
                Text(stringResource(R.string.claim), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
