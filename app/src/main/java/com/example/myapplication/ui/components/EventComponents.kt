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
import androidx.compose.runtime.mutableIntStateOf
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
        GameEventType.ABANDONED_STATION -> Color(0xFF80CBC4)
        GameEventType.PIRATE_RAID -> AppColors.Danger
        GameEventType.TRADING_SHIP -> Color(0xFF66E0FF)
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
                Image(
                    painter = painterResource(eventIconResource(event.type)),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp).padding(end = 8.dp)
                )
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
                GameEventType.ABANDONED_STATION -> stringResource(R.string.event_choose_route)
                GameEventType.PIRATE_RAID -> stringResource(R.string.event_taps_left, tapsLeft)
                GameEventType.CYBER_VIRUS -> stringResource(R.string.cyber_banner_objective)
                GameEventType.TRADING_SHIP -> stringResource(R.string.event_choose_trade)
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
    GameEventType.ABANDONED_STATION -> R.string.event_abandoned_station
    GameEventType.PIRATE_RAID -> R.string.event_pirate_raid
    GameEventType.TRADING_SHIP -> R.string.event_trading_ship
}

private fun eventIconResource(type: GameEventType): Int = when (type) {
    GameEventType.ASTEROID, GameEventType.METEOR_SHOWER -> R.drawable.event_gold_asteroid
    GameEventType.DISTRESS_SIGNAL -> R.drawable.event_rescue_capsule
    GameEventType.ABANDONED_STATION, GameEventType.SOLAR_FLARE -> R.drawable.event_reactor_core
    GameEventType.TRADING_SHIP, GameEventType.PIRATE_RAID -> R.drawable.event_trade_crate
    GameEventType.BLACK_HOLE, GameEventType.CYBER_VIRUS -> R.drawable.event_reactor_core
    GameEventType.STORM -> R.drawable.event_gold_asteroid
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
            text = stringResource(
                if (pending.eventType == GameEventType.ABANDONED_STATION) {
                    R.string.event_station_in_progress
                } else {
                    R.string.event_rescue_in_progress
                },
                secondsLeft
            ),
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
    val asteroidSize = 58.dp
    Box(
        modifier = Modifier
            .offset(
                x = gameAreaWidth * event.x - asteroidSize / 2,
                y = gameAreaHeight * event.y - asteroidSize / 2
            )
            .size(asteroidSize)
            .shadow(12.dp, CircleShape, spotColor = Color(0xFFFFC107))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.event_gold_asteroid),
            contentDescription = stringResource(R.string.event_gold_asteroid),
            modifier = Modifier.fillMaxSize()
        )
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
fun PirateRaidComponent(
    event: GameEvent,
    tapsLeft: Int,
    gameAreaWidth: Dp,
    gameAreaHeight: Dp,
    onClick: () -> Unit
) {
    val shipSize = 96.dp
    Box(
        modifier = Modifier
            .offset(
                x = gameAreaWidth * event.x - shipSize / 2,
                y = gameAreaHeight * event.y - shipSize / 2
            )
            .size(shipSize)
            .shadow(14.dp, CircleShape, spotColor = AppColors.Danger)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.fleet_ufo_alien),
            contentDescription = stringResource(R.string.event_pirate_raid),
            modifier = Modifier.fillMaxSize()
        )
        Text(
            tapsLeft.toString(),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape).padding(5.dp)
        )
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
                        GameEventType.ABANDONED_STATION -> R.string.event_abandoned_station
                        GameEventType.PIRATE_RAID -> R.string.event_pirate_raid
                        GameEventType.TRADING_SHIP -> R.string.event_trading_ship
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
                        GameEventType.ABANDONED_STATION -> R.string.event_desc_abandoned_station
                        GameEventType.PIRATE_RAID -> R.string.event_desc_pirate_raid
                        GameEventType.TRADING_SHIP -> R.string.event_desc_trading_ship
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
fun CyberVirusDialog(event: GameEvent, onResolved: (Boolean) -> Unit, onDismiss: () -> Unit) {
    val sequence = remember(event.startedAt) {
        var seed = event.startedAt
        List(5) {
            seed = seed * 1_103_515_245L + 12_345L
            ((seed ushr 16) % 9).toInt()
        }
    }
    var progress by remember(event.startedAt) { mutableIntStateOf(0) }
    var mistakes by remember(event.startedAt) { mutableIntStateOf(0) }
    val targetNode = sequence.getOrElse(progress) { -1 }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cyber_minigame_title), color = AppColors.Danger, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.cyber_minigame_hint, progress + 1, sequence.size, mistakes, 3), textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                repeat(3) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { column ->
                            val node = row * 3 + column
                            androidx.compose.material3.Button(
                                onClick = {
                                    if (node == targetNode) {
                                        progress++
                                        if (progress == sequence.size) onResolved(true)
                                    } else {
                                        mistakes++
                                        progress = 0
                                        if (mistakes >= 3) onResolved(false)
                                    }
                                },
                                modifier = Modifier.size(62.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = if (node == targetNode) Color(0xFF1B5E20) else Color(0xFF263238)
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text((node + 1).toString(), fontWeight = FontWeight.Black) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(stringResource(R.string.cyber_minigame_sequence, sequence.joinToString(" → ") { (it + 1).toString() }), color = AppColors.Primary, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
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
    val isStation = result.eventType == GameEventType.ABANDONED_STATION
    val isCyberVirus = result.eventType == GameEventType.CYBER_VIRUS
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (result.success) R.string.event_action_success else R.string.event_rescue_failed)) },
        text = {
            Text(
                when {
                    isCyberVirus && result.success -> stringResource(R.string.cyber_success, formatNum(result.reward))
                    isCyberVirus -> stringResource(R.string.cyber_failure, formatNum(result.loss))
                    result.success -> stringResource(R.string.event_expedition_reward, formatNum(result.reward))
                    isStation && result.loss > 0.0 -> stringResource(
                        R.string.event_station_loss,
                        formatNum(result.loss)
                    )
                    else -> stringResource(R.string.event_rescue_no_reward)
                }
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
fun AbandonedStationDialog(
    reward: Double,
    onSafeRoute: () -> Unit,
    onReactorCore: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.event_abandoned_station), fontWeight = FontWeight.Bold) },
        text = {
            Text(
                stringResource(
                    R.string.event_station_choice,
                    formatNum(reward * 1.5),
                    formatNum(reward * 5.0)
                )
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onReactorCore) {
                Text(stringResource(R.string.event_station_reactor))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onSafeRoute) {
                Text(stringResource(R.string.event_station_safe))
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
