package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.GameEvent
import com.example.myapplication.GameEventType
import com.example.myapplication.EventChainResult
import com.example.myapplication.PendingEventChain
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum
import kotlinx.coroutines.delay
import kotlin.math.ceil

@Composable
fun EventBanner(event: GameEvent, tapsLeft: Int, onClick: () -> Unit) {
    var nowMillis by remember(event) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(event) {
        while (nowMillis < event.expiresAt) {
            delay(250)
            nowMillis = System.currentTimeMillis()
        }
    }

    val duration = (event.expiresAt - event.startedAt).coerceAtLeast(1L)
    val remainingMillis = (event.expiresAt - nowMillis).coerceAtLeast(0L)
    val progress = (remainingMillis.toFloat() / duration).coerceIn(0f, 1f)
    val secondsLeft = ceil(remainingMillis / 1000.0).toInt()
    val color = when (event.type) {
        GameEventType.STORM -> AppColors.Warning
        GameEventType.BLACK_HOLE, GameEventType.CYBER_VIRUS -> AppColors.Danger
        GameEventType.SOLAR_FLARE -> Color(0xFFFF5722)
        GameEventType.ASTEROID -> Color(0xFF4CAF50)
        GameEventType.METEOR_SHOWER -> AppColors.Warning
        GameEventType.DISTRESS_SIGNAL -> AppColors.Primary
    }
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(eventTitleResource(event.type)),
                    modifier = Modifier.weight(1f),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = stringResource(R.string.event_seconds_left, secondsLeft),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            val objective = when (event.type) {
                GameEventType.BLACK_HOLE -> stringResource(R.string.event_taps_left, tapsLeft)
                GameEventType.ASTEROID -> stringResource(
                    R.string.event_reward_preview,
                    formatNum(event.reward)
                )
                GameEventType.DISTRESS_SIGNAL -> stringResource(R.string.event_choose_response)
                else -> null
            }
            objective?.let {
                Text(it, color = Color.White, fontSize = 11.sp)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

private fun eventTitleResource(type: GameEventType): Int = when (type) {
    GameEventType.STORM -> R.string.event_space_storm
    GameEventType.ASTEROID -> R.string.event_gold_asteroid
    GameEventType.METEOR_SHOWER -> R.string.event_debris_shower
    GameEventType.BLACK_HOLE -> R.string.event_black_hole
    GameEventType.SOLAR_FLARE -> R.string.event_solar_flare
    GameEventType.CYBER_VIRUS -> R.string.event_cyber_virus
    GameEventType.DISTRESS_SIGNAL -> R.string.event_distress_signal
}

@Composable
fun EventChainPendingBanner(pending: PendingEventChain, modifier: Modifier = Modifier) {
    var nowMillis by remember(pending) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pending) {
        while (nowMillis < pending.resolvesAt) {
            delay(250)
            nowMillis = System.currentTimeMillis()
        }
    }
    val secondsLeft = ceil((pending.resolvesAt - nowMillis).coerceAtLeast(0L) / 1000.0).toInt()
    Card(
        modifier = modifier.padding(16.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Primary.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary)
    ) {
        Text(
            text = stringResource(R.string.event_rescue_in_progress, secondsLeft),
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = AppColors.Primary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun Asteroid(event: GameEvent, gameAreaWidth: Dp, gameAreaHeight: Dp, onClick: () -> Unit) {
    val asteroidSize = 50.dp
    Box(
        modifier = Modifier
            .offset(
                x = gameAreaWidth * event.x - asteroidSize / 2,
                y = gameAreaHeight * event.y - asteroidSize / 2
            )
            .size(asteroidSize)
            .shadow(10.dp, RoundedCornerShape(4.dp), spotColor = Color.Red)
            .background(Color.Red, RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("!", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BlackHoleComponent(
    event: GameEvent,
    tapsLeft: Int,
    gameAreaWidth: Dp,
    gameAreaHeight: Dp,
    onClick: () -> Unit
) {
    val blackHoleSize = 150.dp
    Box(
        modifier = Modifier
            .offset(
                x = gameAreaWidth * event.x - blackHoleSize / 2,
                y = gameAreaHeight * event.y - blackHoleSize / 2
            )
            .size(blackHoleSize)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.dira),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        Text("$tapsLeft", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

@Composable
fun EventInfoDialog(event: GameEvent, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    when (event.type) {
                        GameEventType.STORM -> R.string.event_space_storm
                        GameEventType.ASTEROID -> R.string.event_gold_asteroid
                        GameEventType.METEOR_SHOWER -> R.string.event_debris_shower
                        GameEventType.BLACK_HOLE -> R.string.event_black_hole
                        GameEventType.SOLAR_FLARE -> R.string.event_solar_flare
                        GameEventType.CYBER_VIRUS -> R.string.event_cyber_virus
                        GameEventType.DISTRESS_SIGNAL -> R.string.event_distress_signal
                    }
                ),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(
                    when (event.type) {
                        GameEventType.STORM -> R.string.event_desc_storm
                        GameEventType.ASTEROID -> R.string.event_desc_asteroid
                        GameEventType.METEOR_SHOWER -> R.string.event_desc_debris_shower
                        GameEventType.BLACK_HOLE -> R.string.event_desc_black_hole
                        GameEventType.SOLAR_FLARE -> R.string.event_desc_solar_flare
                        GameEventType.CYBER_VIRUS -> R.string.event_desc_cyber_virus
                        GameEventType.DISTRESS_SIGNAL -> R.string.event_desc_distress_signal
                    }
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun DistressSignalDialog(
    reward: Double,
    onSalvage: () -> Unit,
    onRescue: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.event_distress_signal), fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(R.string.event_distress_choice, formatNum(reward), formatNum(reward * 3.0))) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onRescue) {
                Text(stringResource(R.string.event_rescue))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onSalvage) {
                Text(stringResource(R.string.event_salvage))
            }
        }
    )
}

@Composable
fun EventChainResultDialog(result: EventChainResult, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (result.success) R.string.event_action_success else R.string.event_rescue_failed)) },
        text = {
            Text(
                if (result.success) stringResource(R.string.event_rescue_reward, formatNum(result.reward))
                else stringResource(R.string.event_rescue_no_reward)
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}

@Composable
private fun TextButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        content()
    }
}
