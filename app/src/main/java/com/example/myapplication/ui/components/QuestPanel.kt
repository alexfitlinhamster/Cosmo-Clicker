package com.example.myapplication.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.GameState
import com.example.myapplication.Quest
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
            .fillMaxWidth()
            .height(400.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(AppColors.WhiteAlpha20, CircleShape)
                )
            }

            Text(
                text = stringResource(R.string.quests),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (state.activeQuests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_active_quests), color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.activeQuests, key = { it.id }) { quest ->
                        QuestItemRow(quest, onClaim)
                    }
                }
            }
        }
    }
}

@Composable
fun QuestItemRow(quest: Quest, onClaim: (String) -> Unit) {
    val progress = (quest.progress / quest.target).toFloat().coerceIn(0f, 1f)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .border(1.dp, if (quest.isCompleted) AppColors.Primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(quest.description, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (quest.isCompleted) AppColors.Primary else Color.Cyan,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${formatNum(quest.progress)} / ${formatNum(quest.target)}",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
                if (quest.rewardDebris > 0) {
                    Text(
                        stringResource(R.string.reward_debris, formatNum(quest.rewardDebris)),
                        color = AppColors.Primary,
                        fontSize = 10.sp
                    )
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
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { onClaim(quest.id) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(stringResource(R.string.claim), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
