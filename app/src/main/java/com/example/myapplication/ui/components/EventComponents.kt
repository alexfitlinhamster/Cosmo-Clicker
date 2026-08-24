package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.GameEvent
import com.example.myapplication.GameEventType
import com.example.myapplication.EventChainResult
import kotlinx.coroutines.delay
import com.example.myapplication.EventEngine
import com.example.myapplication.TradeOffer
import com.example.myapplication.CaseType
import com.example.myapplication.GameResourceRegistry
import com.example.myapplication.PendingEventChain
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum
import kotlinx.coroutines.delay
import kotlin.math.ceil

@Composable
fun EventBanner(event: GameEvent, tapsLeft: Int) {
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
            .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFF5B5FEF), spotColor = color),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (event.isElite) Color(0xFFB49CFF) else color.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xF20A1028),
                            Color(0xF21A1740),
                            Color(0xF20A2338)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(eventIconResource(event.type)),
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(Modifier.width(8.dp))
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
                GameEventType.BLACK_HOLE -> stringResource(R.string.black_hole_nodes_left, tapsLeft)
                GameEventType.STORM, GameEventType.SOLAR_FLARE ->
                    stringResource(R.string.event_taps_left, tapsLeft)
                GameEventType.DISTRESS_SIGNAL -> stringResource(R.string.event_choose_response)
                GameEventType.ABANDONED_STATION -> stringResource(R.string.event_choose_route)
                GameEventType.PIRATE_RAID -> stringResource(R.string.event_taps_left, tapsLeft)
                GameEventType.CYBER_VIRUS -> stringResource(R.string.cyber_banner_objective)
                GameEventType.TRADING_SHIP -> stringResource(R.string.event_ship_hull, tapsLeft)
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
    GameEventType.METEOR_SHOWER -> R.string.event_debris_shower
    GameEventType.BLACK_HOLE -> R.string.event_black_hole
    GameEventType.SOLAR_FLARE -> R.string.event_solar_flare
    GameEventType.CYBER_VIRUS -> R.string.event_cyber_virus
    GameEventType.DISTRESS_SIGNAL -> R.string.event_distress_signal
    GameEventType.ABANDONED_STATION -> R.string.event_abandoned_station
    GameEventType.PIRATE_RAID -> R.string.event_pirate_raid
    GameEventType.TRADING_SHIP -> R.string.event_trading_ship
}

private fun eventAccent(type: GameEventType): Color = when (type) {
    GameEventType.STORM, GameEventType.METEOR_SHOWER -> AppColors.Warning
    GameEventType.BLACK_HOLE, GameEventType.CYBER_VIRUS, GameEventType.PIRATE_RAID -> AppColors.Danger
    GameEventType.SOLAR_FLARE -> Color(0xFFFF6B35)
    GameEventType.DISTRESS_SIGNAL -> AppColors.Primary
    GameEventType.ABANDONED_STATION -> Color(0xFF80CBC4)
    GameEventType.TRADING_SHIP -> Color(0xFF66E0FF)
}

private fun eventIconResource(type: GameEventType): Int = when (type) {
    GameEventType.METEOR_SHOWER -> R.drawable.event_meteor_minimal_v2
    GameEventType.DISTRESS_SIGNAL -> R.drawable.event_distress_minimal_v2
    GameEventType.ABANDONED_STATION -> R.drawable.event_station_minimal_v2
    GameEventType.SOLAR_FLARE -> R.drawable.event_solar_minimal_v2
    GameEventType.TRADING_SHIP -> R.drawable.event_trade_minimal_v2
    GameEventType.PIRATE_RAID -> R.drawable.event_pirate_minimal_v2
    GameEventType.BLACK_HOLE -> R.drawable.event_black_hole_minimal_v2
    GameEventType.CYBER_VIRUS -> R.drawable.event_cyber_minimal_v2
    GameEventType.STORM -> R.drawable.event_storm_minimal_v2
}

@Composable
fun EventChallengeComponent(
    event: GameEvent,
    gameAreaWidth: Dp,
    gameAreaHeight: Dp,
    onClick: () -> Unit
) {
    val size = 76.dp
    val icon = if (event.type == GameEventType.STORM) {
        R.drawable.event_storm_minimal_v2
    } else {
        R.drawable.event_solar_minimal_v2
    }
    Box(
        modifier = Modifier
            .offset(
                x = gameAreaWidth * event.x - size / 2,
                y = gameAreaHeight * event.y - size / 2
            )
            .size(size)
            .eventClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun MeteorInterceptChallenge(
    event: GameEvent,
    target: Int,
    remaining: Int,
    gameAreaWidth: Dp,
    gameAreaHeight: Dp,
    onTargetClick: (Int) -> Unit
) {
    val positions = listOf(-82.dp to (-38).dp, 82.dp to (-38).dp, -82.dp to 48.dp, 82.dp to 48.dp)
    Box(
        Modifier
            .offset(x = gameAreaWidth * event.x - 138.dp, y = gameAreaHeight * event.y - 108.dp)
            .size(276.dp, 216.dp)
            .background(Color(0xD90A1220), RoundedCornerShape(22.dp))
            .border(1.dp, Color(0xFFFFA33D).copy(.48f), RoundedCornerShape(22.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.meteor_intercept_title), color = Color(0xFFFFB44D), fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(stringResource(R.string.meteor_intercept_hint, remaining), color = Color.White.copy(.68f), fontSize = 9.sp)
        }
        positions.forEachIndexed { index, (x, y) ->
            val active = index == target
            Box(
                Modifier
                    .offset(x, y)
                    .size(if (active) 62.dp else 52.dp)
                    .background(Color(0xFF111D2D), CircleShape)
                    .border(if (active) 3.dp else 1.dp, if (active) Color(0xFFFFB13D) else Color.White.copy(.14f), CircleShape)
                    .eventClickable { onTargetClick(index) },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painterResource(R.drawable.event_meteor_minimal_v2),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(if (active) 5.dp else 9.dp),
                    alpha = if (active) 1f else .34f
                )
                if (active) Box(Modifier.size(12.dp).background(Color.White.copy(.9f), CircleShape))
            }
        }
    }
}

@Composable
fun StormNodeChallenge(
    event: GameEvent,
    sequence: List<Int>,
    progress: Int,
    round: Int,
    gameAreaWidth: Dp,
    gameAreaHeight: Dp,
    onNodeClick: (Int) -> Unit
) {
    val nodeColors = listOf(Color(0xFF62E8FF), Color(0xFFB47CFF), Color(0xFFFFC857))
    val positions = listOf(-92.dp to 34.dp, 0.dp to (-28).dp, 92.dp to 34.dp)
    Box(
        Modifier.offset(x = gameAreaWidth * event.x - 130.dp, y = gameAreaHeight * event.y - 74.dp)
            .size(260.dp, 148.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(R.string.event_storm_round, round, 3), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter))
        positions.forEachIndexed { index, (x, y) ->
            val isNext = sequence.getOrNull(progress) == index
            Box(
                Modifier.offset(x, y).size(if (isNext) 66.dp else 56.dp)
                    .shadow(if (isNext) 22.dp else 8.dp, CircleShape, spotColor = nodeColors[index])
                    .background(nodeColors[index].copy(alpha = if (isNext) .95f else .42f), CircleShape)
                    .border(2.dp, Color.White.copy(alpha = if (isNext) .9f else .25f), CircleShape)
                    .eventClickable { onNodeClick(index) },
                contentAlignment = Alignment.Center
            ) { Text("${index + 1}", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 20.sp) }
        }
        Text(sequence.joinToString("  ") { "${it + 1}" }, color = AppColors.Warning, fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun SolarFlareProtocol(event: GameEvent, sequence: List<Int>, progress: Int, phase: Int, gameAreaWidth: Dp, gameAreaHeight: Dp, onChannelClick: (Int) -> Unit) {
    val colors = listOf(Color(0xFFFF5A36), Color(0xFFFFC247), Color(0xFF65E7FF), Color(0xFFD879FF))
    val symbols = listOf("▲", "◆", "●", "✦")
    val positions = listOf(0.dp to (-82).dp, 82.dp to 0.dp, 0.dp to 82.dp, (-82).dp to 0.dp)
    var memorizing by remember(event.startedAt, phase, sequence) { mutableStateOf(true) }
    LaunchedEffect(event.startedAt, phase, sequence) {
        memorizing = true
        delay((1_900L + sequence.size * 180L).coerceAtMost(3_000L))
        memorizing = false
    }
    val pulseMotion = rememberInfiniteTransition(label = "solar_flare_pulse")
    val pulse by pulseMotion.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(430), RepeatMode.Reverse), label = "solar_pulse")
    Box(Modifier.offset(x = (gameAreaWidth - 260.dp) / 2, y = (gameAreaHeight - 260.dp) / 2).size(260.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(206.dp).background(Color(0xB8081020), CircleShape).border(1.dp, Color(0xFFFF7A38).copy(.35f), CircleShape))
        Box(Modifier.size(128.dp).border(1.dp, Color(0xFFFFC247).copy(.25f), CircleShape))
        Box(Modifier.size(62.dp).shadow(24.dp, CircleShape, spotColor = Color(0xFFFF6A32)).background(Brush.radialGradient(listOf(Color(0xFFFFF0A5), Color(0xFFFF6A32), Color(0xFF8B2330))), CircleShape).border(2.dp, Color.White.copy(.72f), CircleShape), contentAlignment = Alignment.Center) {
            Text("✦", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        }
        Column(Modifier.align(Alignment.TopCenter).background(Color(0xDD0A1425), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.solar_protocol_phase, phase, 4), color = Color(0xFFFFB14A), fontWeight = FontWeight.Black)
            Text(stringResource(if (memorizing) R.string.solar_protocol_memorize else R.string.solar_protocol_repeat), color = if (memorizing) AppColors.Warning else AppColors.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        positions.forEachIndexed { index, (x, y) ->
            Box(Modifier.offset(x, y).size(52.dp).shadow(if (memorizing) 14.dp else 5.dp, CircleShape, spotColor = colors[index]).background(Color(0xE90B1728), CircleShape).border(2.dp, colors[index].copy(alpha = if (memorizing) .45f else .9f), CircleShape).eventClickable { if (!memorizing) onChannelClick(index) }, contentAlignment = Alignment.Center) {
                Text(symbols[index], color = colors[index], fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
        }
        Row(Modifier.align(Alignment.BottomCenter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sequence.forEachIndexed { step, channel ->
                Text(if (memorizing) symbols[channel] else if (step < progress) "✓" else "•", color = if (step < progress) AppColors.Primary else if (memorizing) colors[channel] else Color.Gray, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun CyberVirusField(
    event: GameEvent,
    target: Int,
    remaining: Int,
    gameAreaWidth: Dp,
    gameAreaHeight: Dp,
    onNodeClick: (Int) -> Unit
) {
    val positions = listOf(.22f to .25f, .72f to .29f, .28f to .68f, .74f to .66f)
    val glitch = rememberInfiniteTransition(label = "cyber_glitch")
    val alpha by glitch.animateFloat(.10f, .24f, infiniteRepeatable(tween(120), RepeatMode.Reverse), label = "glitch_alpha")
    Canvas(Modifier.size(gameAreaWidth, gameAreaHeight)) {
        repeat(14) { line ->
            val y = size.height * ((line * 73 + event.startedAt.toInt()) % 1000) / 1000f
            drawRect(Color(0xFF41F5C7).copy(alpha = alpha), Offset(0f, y), Size(size.width, 2f + line % 3))
        }
    }
    Text(
        "SYS_ERR // 0x${event.startedAt.toString(16).takeLast(4)} // NODE_BREACH",
        modifier = Modifier.offset(x = 18.dp, y = gameAreaHeight * .10f),
        color = Color(0xFF63FFD8).copy(alpha = .72f),
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp
    )
    positions.forEachIndexed { index, (x, y) ->
        val active = index == target
        Box(
            Modifier.offset(x = gameAreaWidth * x - 30.dp, y = gameAreaHeight * y - 30.dp)
                .size(60.dp).shadow(if (active) 18.dp else 3.dp, CircleShape, spotColor = Color(0xFF36FFD0))
                .background(if (active) Color(0xFF36FFD0).copy(.82f) else Color(0xCC102536), CircleShape)
                .border(2.dp, if (active) Color.White else Color(0xFF36FFD0).copy(.38f), CircleShape)
                .eventClickable { onNodeClick(index) },
            contentAlignment = Alignment.Center
        ) {
            Text(if (active) "</>" else "00", color = if (active) Color(0xFF06161A) else Color(0xFF36FFD0), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
        }
    }
    Text(
        stringResource(R.string.event_taps_left, remaining),
        modifier = Modifier.offset(x = (gameAreaWidth - 150.dp) / 2, y = gameAreaHeight * .82f).width(150.dp),
        color = Color(0xFF63FFD8), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold
    )
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
fun DistressSignalScanner(
    event: GameEvent,
    sequence: List<Int>,
    progress: Int,
    phase: Int,
    gameAreaWidth: Dp,
    gameAreaHeight: Dp,
    onNodeClick: (Int) -> Unit,
    onSalvage: () -> Unit,
    onRescue: () -> Unit
) {
    val accent = Color(0xFF72E4FF)
    val requiredLocks = if (event.isElite) 4 else 3
    val panelWidth = minOf(gameAreaWidth - 32.dp, 360.dp)
    val scan = rememberInfiniteTransition(label = "distress_scan")
    val pulse by scan.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "distress_pulse"
    )

    Column(
        modifier = Modifier
            .offset(x = (gameAreaWidth - panelWidth) / 2, y = minOf(gameAreaHeight * 0.06f, 36.dp))
            .width(panelWidth)
            .background(
                Brush.verticalGradient(listOf(Color(0xF20A1229), Color(0xF20B2638))),
                RoundedCornerShape(24.dp)
            )
            .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = accent)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.distress_scanner_title),
            color = accent,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
        Text(
            if (phase == 1) stringResource(R.string.distress_scanner_hint)
            else stringResource(R.string.distress_capsule_found),
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))

        Box(Modifier.size(196.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(190.dp).border(1.dp, accent.copy(alpha = 0.18f), CircleShape))
            Box(Modifier.size(132.dp).border(1.dp, accent.copy(alpha = 0.24f), CircleShape))
            Image(
                painterResource(R.drawable.event_distress_minimal_v2),
                contentDescription = null,
                modifier = Modifier.size(94.dp).graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                    alpha = if (phase == 1) 0.42f else 1f
                },
                contentScale = ContentScale.Fit
            )
            if (phase == 1) {
                val offsets = listOf(77.dp to 1.dp, 153.dp to 77.dp, 77.dp to 153.dp, 1.dp to 77.dp)
                offsets.forEachIndexed { index, (x, y) ->
                    val active = sequence.firstOrNull() == index
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x, y)
                            .size(42.dp)
                            .background(if (active) Color(0xFF164760) else Color(0xFF101B30), CircleShape)
                            .border(
                                if (active) 3.dp else 1.dp,
                                if (active) accent else Color.White.copy(alpha = 0.15f),
                                CircleShape
                            )
                            .shadow(if (active) 14.dp else 1.dp, CircleShape, spotColor = accent)
                            .eventClickable { onNodeClick(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(listOf("N", "E", "S", "W")[index], color = if (active) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (phase == 1) {
            Text(
                stringResource(R.string.distress_locks, progress, requiredLocks),
                color = Color.White,
                fontSize = 11.sp
            )
            LinearProgressIndicator(
                progress = { progress.toFloat() / requiredLocks },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = accent,
                trackColor = Color.White.copy(alpha = 0.09f)
            )
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSalvage,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5A21))
                ) { Text(stringResource(R.string.event_salvage)) }
                Button(
                    onClick = onRescue,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF176C74))
                ) { Text(stringResource(R.string.event_rescue)) }
            }
        }
    }
}

@Composable
fun AbandonedStationAccess(
    event: GameEvent,
    sequence: List<Int>,
    progress: Int,
    phase: Int,
    gameAreaWidth: Dp,
    gameAreaHeight: Dp,
    onRelayClick: (Int) -> Unit,
    onSafeRoute: () -> Unit,
    onReactorCore: () -> Unit
) {
    val accent = Color(0xFF80CBC4)
    val requiredRelays = if (event.isElite) 4 else 3
    val panelWidth = minOf(gameAreaWidth - 32.dp, 360.dp)
    Column(
        modifier = Modifier
            .offset(x = (gameAreaWidth - panelWidth) / 2, y = minOf(gameAreaHeight * 0.05f, 32.dp))
            .width(panelWidth)
            .background(
                Brush.verticalGradient(listOf(Color(0xF20B1724), Color(0xF2152730), Color(0xF209111E))),
                RoundedCornerShape(24.dp)
            )
            .border(1.dp, accent.copy(alpha = 0.62f), RoundedCornerShape(24.dp))
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = accent)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.station_access_title), color = accent, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text(
            if (phase == 1) stringResource(R.string.station_access_hint) else stringResource(R.string.station_airlock_open),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
        Image(
            painterResource(R.drawable.event_station_minimal_v2),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(104.dp),
            contentScale = ContentScale.Fit
        )
        if (phase == 1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(3) { index ->
                    val active = sequence.firstOrNull() == index
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(if (active) Color(0xFF17463F) else Color(0xFF151C28), RoundedCornerShape(16.dp))
                                .border(
                                    if (active) 3.dp else 1.dp,
                                    if (active) accent else Color.White.copy(alpha = 0.13f),
                                    RoundedCornerShape(16.dp)
                                )
                                .shadow(if (active) 14.dp else 1.dp, RoundedCornerShape(16.dp), spotColor = accent)
                                .eventClickable { onRelayClick(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(listOf("Ⅰ", "Ⅱ", "Ⅲ")[index], color = if (active) Color.White else Color.Gray, fontWeight = FontWeight.Black, fontSize = 21.sp)
                        }
                        Text(
                            stringResource(R.string.station_relay, index + 1),
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 9.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(stringResource(R.string.station_relays_progress, progress, requiredRelays), color = Color.White, fontSize = 11.sp)
            LinearProgressIndicator(
                progress = { progress.toFloat() / requiredRelays },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = accent,
                trackColor = Color.White.copy(alpha = 0.09f)
            )
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSafeRoute,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF236C67))
                ) { Text(stringResource(R.string.event_station_safe)) }
                Button(
                    onClick = onReactorCore,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A3748))
                ) { Text(stringResource(R.string.event_station_reactor)) }
            }
        }
    }
}

@Composable
fun BlackHoleComponent(
    event: GameEvent,
    tapsLeft: Int,
    sequence: List<Int>,
    progress: Int,
    mistakes: Int,
    gameAreaWidth: Dp,
    gameAreaHeight: Dp,
    onNodeClick: (Int) -> Unit
) {
    val fieldSize = minOf(gameAreaWidth - 36.dp, 252.dp)
    val blackHoleSize = 116.dp
    val nodeSize = 44.dp
    val motion = rememberInfiniteTransition(label = "black_hole_motion")
    val rotation by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing)),
        label = "black_hole_rotation"
    )
    val pulse by motion.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "black_hole_pulse"
    )
    Box(
        modifier = Modifier
            .offset(
                x = (gameAreaWidth - fieldSize) / 2,
                y = minOf(gameAreaHeight * 0.06f, 36.dp)
            )
            .size(fieldSize)
            .background(Color(0xD9081026), CircleShape)
            .border(1.dp, Color(0xFF8155C7).copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(218.dp).border(1.dp, Color(0xFF9C72E8).copy(alpha = 0.25f), CircleShape))
        Box(Modifier.size(170.dp).border(1.dp, Color(0xFF66D9FF).copy(alpha = 0.18f), CircleShape))
        Image(
            painter = painterResource(id = R.drawable.event_black_hole_minimal_v2),
            contentDescription = stringResource(R.string.event_black_hole),
            modifier = Modifier.size(blackHoleSize).graphicsLayer {
                rotationZ = rotation
                scaleX = pulse
                scaleY = pulse
            }
        )
        val nodeOffsets = listOf(
            104.dp to 8.dp,
            200.dp to 104.dp,
            104.dp to 200.dp,
            8.dp to 104.dp
        )
        nodeOffsets.forEachIndexed { index, (x, y) ->
            val active = sequence.firstOrNull() == index
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = x, y = y)
                    .size(nodeSize)
                    .shadow(if (active) 16.dp else 2.dp, CircleShape, spotColor = Color(0xFF7DE8FF))
                    .background(
                        if (active) Color(0xFF143B57) else Color(0xFF17172C),
                        CircleShape
                    )
                    .border(
                        if (active) 3.dp else 1.dp,
                        if (active) Color(0xFF7DE8FF) else Color.White.copy(alpha = 0.16f),
                        CircleShape
                    )
                    .eventClickable { onNodeClick(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    listOf("◆", "●", "▲", "✦")[index],
                    color = if (active) Color.White else Color.White.copy(alpha = 0.38f),
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp
                )
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.black_hole_nodes_progress, progress, progress + tapsLeft),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            if (mistakes > 0) {
                Text(
                    stringResource(R.string.black_hole_instability, mistakes),
                    color = Color(0xFFFF8A9A),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun BlackHoleEventDialog(event: GameEvent, tapsLeft: Int, onDismiss: () -> Unit) {
    var nowMillis by remember(event) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(event) {
        while (nowMillis < event.expiresAt) {
            delay(250)
            nowMillis = System.currentTimeMillis()
        }
    }
    val secondsLeft = ceil((event.expiresAt - nowMillis).coerceAtLeast(0L) / 1000.0).toInt()
    val totalNodes = if (event.isElite) 8 else 6
    val stabilized = ((totalNodes - tapsLeft).coerceIn(0, totalNodes)) / totalNodes.toFloat()

    SpaceDialog(
        title = stringResource(R.string.event_black_hole),
        onDismiss = onDismiss,
        content = {
            Image(
                painter = painterResource(R.drawable.event_black_hole_minimal_v2),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(150.dp)
            )
            Text(
                stringResource(R.string.event_desc_black_hole),
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = AppColors.Danger.copy(alpha = 0.13f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Danger.copy(alpha = 0.45f))
            ) {
                Text(
                    stringResource(R.string.event_black_hole_danger),
                    modifier = Modifier.fillMaxWidth().padding(11.dp),
                    color = Color(0xFFFF8A80),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.black_hole_nodes_left, tapsLeft), color = Color.White, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.event_seconds_left, secondsLeft), color = AppColors.Warning, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { stabilized },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color(0xFFCE5CFF),
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        },
        actions = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.event_black_hole_engage), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun PirateAmbushComponent(
    event: GameEvent,
    sequence: List<Int>,
    progress: Int,
    phase: Int,
    gameAreaWidth: Dp,
    gameAreaHeight: Dp,
    onTargetClick: (Int) -> Unit,
    onResolve: (Boolean) -> Unit
) {
    val accent = Color(0xFFFF536D)
    val scanner = Color(0xFF72E4FF)
    val requiredHits = if (event.isElite) 7 else 5
    val transition = rememberInfiniteTransition(label = "pirate_pursuit")
    val drift by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pirate_drift"
    )
    val panelWidth = minOf(gameAreaWidth - 32.dp, 360.dp)

    Box(
        modifier = Modifier
            .offset(x = (gameAreaWidth - panelWidth) / 2, y = minOf(gameAreaHeight * 0.06f, 36.dp))
            .width(panelWidth)
            .height(258.dp)
            .shadow(18.dp, RoundedCornerShape(24.dp), spotColor = accent)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF20A1026), Color(0xF21A1537), Color(0xF2071C2D))
                ),
                RoundedCornerShape(24.dp)
            )
            .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
            .padding(14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.pirate_pursuit_title),
                color = accent,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            Text(
                if (phase == 1) stringResource(R.string.pirate_pursuit_hint)
                else stringResource(R.string.pirate_target_disabled),
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))

            if (phase == 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isTarget = index == sequence.firstOrNull()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(106.dp)
                                .graphicsLayer {
                                    translationY = drift * if (index % 2 == 0) 1f else -1f
                                    scaleX = if (isTarget) 1.04f else 0.92f
                                    scaleY = scaleX
                                }
                                .background(
                                    if (isTarget) scanner.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.025f),
                                    RoundedCornerShape(18.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isTarget) scanner.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.10f),
                                    RoundedCornerShape(18.dp)
                                )
                                .eventClickable { onTargetClick(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painterResource(R.drawable.event_pirate_minimal_v2),
                                contentDescription = stringResource(R.string.event_pirate_raid),
                                modifier = Modifier.fillMaxSize().padding(4.dp).alpha(if (isTarget) 1f else 0.48f),
                                contentScale = ContentScale.Fit
                            )
                            if (isTarget) {
                                Text(
                                    stringResource(R.string.pirate_signal_locked),
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                                    color = scanner,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.pirate_hits, progress, requiredHits), color = Color.White, fontSize = 11.sp)
                    Text(stringResource(R.string.pirate_evading), color = scanner, fontSize = 11.sp)
                }
                LinearProgressIndicator(
                    progress = { progress.toFloat() / requiredHits },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.09f)
                )
            } else {
                Image(
                    painterResource(R.drawable.event_pirate_minimal_v2),
                    contentDescription = null,
                    modifier = Modifier.height(132.dp).fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onResolve(false) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9F2942))
                    ) { Text(stringResource(R.string.pirate_destroy)) }
                    Button(
                        onClick = { onResolve(true) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16746F))
                    ) { Text(stringResource(R.string.pirate_capture)) }
                }
            }
        }
    }
}

@Composable
fun TradingShipComponent(
    event: GameEvent,
    hullLeft: Int,
    gameAreaWidth: Dp,
    gameAreaHeight: Dp
) {
    val shipSize = 112.dp
    Box(
        modifier = Modifier
            .offset(
                x = gameAreaWidth * event.x - shipSize / 2,
                y = gameAreaHeight * event.y - shipSize / 2
            )
            .size(shipSize),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.event_trade_minimal_v2),
                contentDescription = stringResource(R.string.event_trading_ship),
                modifier = Modifier.size(88.dp)
            )
            LinearProgressIndicator(
                progress = { (hullLeft / 18f).coerceIn(0f, 1f) },
                modifier = Modifier.width(92.dp).height(6.dp).clip(CircleShape),
                color = Color(0xFFFF6B6B),
                trackColor = Color.White.copy(alpha = .16f)
            )
        }
    }
}

@Composable
fun TradingShipMarket(
    event: GameEvent,
    totalDebris: Double,
    phase: Int,
    sequence: List<Int>,
    progress: Int,
    priceMultiplier: Double,
    onChannelClick: (Int) -> Unit,
    onBuy: (TradeOffer) -> Unit,
    onDismiss: () -> Unit
) {
    val offers = remember(event.startedAt) { EventEngine.tradeOffers(event) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF20A1020)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .heightIn(max = 620.dp)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFF101A2D), RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFF66E0FF).copy(alpha = .42f), RoundedCornerShape(24.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.trade_market_title), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(
                stringResource(if (phase == 1) R.string.trade_docking_hint else R.string.trade_market_subtitle),
                color = Color.White.copy(alpha = .68f), fontSize = 12.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Image(
                painter = painterResource(R.drawable.event_trade_minimal_v2),
                contentDescription = null,
                modifier = Modifier.size(104.dp)
            )
            if (phase == 1) {
                Text(stringResource(R.string.trade_signal_progress, progress, 3), color = Color(0xFF66E0FF), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) { channel ->
                        val active = sequence.firstOrNull() == channel
                        Button(
                            onClick = { onChannelClick(channel) },
                            modifier = Modifier.size(72.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) Color(0xFF36C5E8) else Color(0xFF17253D)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF66E0FF).copy(alpha = if (active) 1f else .35f)),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text(listOf("α", "β", "γ")[channel], fontSize = 22.sp, fontWeight = FontWeight.Black) }
                    }
                }
                Text(
                    stringResource(R.string.trade_price_status, ((priceMultiplier - 1.0) * 100).toInt()),
                    color = if (priceMultiplier <= 1.0) Color(0xFF63E6BE) else Color(0xFFFF8A80),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            } else {
            Text(stringResource(R.string.trade_balance, formatNum(totalDebris)), color = Color(0xFF66E0FF), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            offers.forEach { offer ->
                val cost = EventEngine.tradeOfferCost(event, offer, priceMultiplier)
                TradeOfferCard(
                    offer = offer,
                    cost = cost,
                    enabled = totalDebris >= cost,
                    onBuy = { onBuy(offer) }
                )
                Spacer(Modifier.height(8.dp))
            }
            }
            Button(onClick = onDismiss, style = CosmicButtonStyle.Secondary) {
                Text(stringResource(R.string.trade_leave), color = Color.White.copy(alpha = .7f))
            }
        }
    }
}

@Composable
private fun TradeOfferCard(offer: TradeOffer, cost: Double, enabled: Boolean, onBuy: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151E32)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF66E0FF).copy(alpha = .45f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(painterResource(tradeOfferIcon(offer)), contentDescription = null, modifier = Modifier.size(52.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(tradeOfferTitle(offer)), color = Color.White, fontWeight = FontWeight.Bold)
                Text(stringResource(tradeOfferDescription(offer)), color = Color.White.copy(alpha = .62f), fontSize = 11.sp)
            }
            Button(
                onClick = onBuy,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF36C5E8)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(formatNum(cost), fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

private fun tradeOfferTitle(offer: TradeOffer): Int = when (offer) {
    TradeOffer.POWER_CORE -> R.string.trade_title_power
    TradeOffer.LUCK_SCANNER -> R.string.trade_title_luck
    TradeOffer.CLICK_AMPLIFIER -> R.string.trade_title_click
    TradeOffer.FLEET_OVERDRIVE -> R.string.trade_title_fleet
    TradeOffer.DEBRIS_CARGO -> R.string.trade_title_debris
    TradeOffer.COMMON_CASE -> R.string.trade_title_common_case
    TradeOffer.RARE_CASE -> R.string.trade_title_rare_case
    TradeOffer.LEGENDARY_CASE -> R.string.trade_title_legendary_case
    TradeOffer.RANDOM_DRONE -> R.string.trade_title_drone
}

private fun tradeOfferDescription(offer: TradeOffer): Int = when (offer) {
    TradeOffer.POWER_CORE -> R.string.trade_desc_power
    TradeOffer.LUCK_SCANNER -> R.string.trade_desc_luck
    TradeOffer.CLICK_AMPLIFIER -> R.string.trade_desc_click
    TradeOffer.FLEET_OVERDRIVE -> R.string.trade_desc_fleet
    TradeOffer.DEBRIS_CARGO -> R.string.trade_desc_debris
    TradeOffer.COMMON_CASE -> R.string.trade_desc_common_case
    TradeOffer.RARE_CASE -> R.string.trade_desc_rare_case
    TradeOffer.LEGENDARY_CASE -> R.string.trade_desc_legendary_case
    TradeOffer.RANDOM_DRONE -> R.string.trade_desc_drone
}

private fun tradeOfferIcon(offer: TradeOffer): Int = when (offer) {
    TradeOffer.POWER_CORE, TradeOffer.CLICK_AMPLIFIER -> R.drawable.event_reactor_core
    TradeOffer.LUCK_SCANNER -> R.drawable.upgrade_signal_beacon_v2
    TradeOffer.FLEET_OVERDRIVE -> R.drawable.drone_20
    TradeOffer.DEBRIS_CARGO -> R.drawable.debris_01
    TradeOffer.COMMON_CASE -> GameResourceRegistry.caseFrame(CaseType.COMMON, 1)
    TradeOffer.RARE_CASE -> GameResourceRegistry.caseFrame(CaseType.RARE, 1)
    TradeOffer.LEGENDARY_CASE -> GameResourceRegistry.caseFrame(CaseType.LEGENDARY, 1)
    TradeOffer.RANDOM_DRONE -> R.drawable.drone_29
}

@Composable
fun EventInfoDialog(event: GameEvent, onDismiss: () -> Unit) {
    val accent = eventAccent(event.type)
    SpaceDialog(
        title = stringResource(
                    when (event.type) {
                        GameEventType.STORM -> R.string.event_space_storm
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
        onDismiss = onDismiss,
        content = {
            Surface(
                modifier = Modifier.fillMaxWidth().height(132.dp),
                shape = RoundedCornerShape(18.dp),
                color = accent.copy(alpha = .10f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .42f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(eventIconResource(event.type)),
                        contentDescription = null,
                        modifier = Modifier.size(104.dp).padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = .035f), RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(stringResource(R.string.event_how_to_play), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(
                    when (event.type) {
                        GameEventType.STORM -> R.string.event_desc_storm
                        GameEventType.METEOR_SHOWER -> R.string.event_desc_debris_shower
                        GameEventType.BLACK_HOLE -> R.string.event_desc_black_hole
                        GameEventType.SOLAR_FLARE -> R.string.event_desc_solar_flare
                        GameEventType.CYBER_VIRUS -> R.string.event_desc_cyber_virus
                        GameEventType.DISTRESS_SIGNAL -> R.string.event_desc_distress_signal
                        GameEventType.ABANDONED_STATION -> R.string.event_desc_abandoned_station
                        GameEventType.PIRATE_RAID -> R.string.event_desc_pirate_raid
                        GameEventType.TRADING_SHIP -> R.string.event_desc_trading_ship
                    }
                    ), color = Color.White.copy(alpha = .86f), fontSize = 13.sp, lineHeight = 19.sp
                )
            }
        },
        actions = { Button(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

@Composable
fun CyberVirusDialog(event: GameEvent, onResolved: (Boolean) -> Unit, onDismiss: () -> Unit) {
    val sequence = remember(event.startedAt) {
        var seed = event.startedAt
        List(6) {
            seed = seed * 1_103_515_245L + 12_345L
            ((seed ushr 16) % 9).toInt()
        }
    }
    var progress by remember(event.startedAt) { mutableIntStateOf(0) }
    var mistakes by remember(event.startedAt) { mutableIntStateOf(0) }
    var isMemorizing by remember(event.startedAt) { mutableStateOf(true) }
    LaunchedEffect(event.startedAt, mistakes) {
        isMemorizing = true
        delay(2_600L)
        isMemorizing = false
    }
    val targetNode = sequence.getOrElse(progress) { -1 }
    SpaceDialog(
        title = stringResource(R.string.cyber_minigame_title),
        onDismiss = onDismiss,
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.event_cyber_minimal_v2),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress.toFloat() / sequence.size },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = AppColors.Primary,
                    trackColor = Color.White.copy(alpha = .1f)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(if (isMemorizing) R.string.cyber_memorize else R.string.cyber_minigame_hint, progress + 1, sequence.size, mistakes, 3),
                    color = if (isMemorizing) AppColors.Primary else Color.White.copy(alpha = .82f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                repeat(3) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { column ->
                            val node = row * 3 + column
                            Button(
                                onClick = {
                                    if (node == targetNode) {
                                        progress++
                                        if (progress == sequence.size) onResolved(true)
                                    } else {
                                        mistakes++
                                        progress = (progress - 1).coerceAtLeast(0)
                                        if (mistakes >= 3) onResolved(false)
                                    }
                                },
                                modifier = Modifier.size(62.dp),
                                enabled = !isMemorizing,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF14283D),
                                    disabledContainerColor = if (node in sequence) Color(0xFF7A1741) else Color(0xFF14283D),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text((node + 1).toString(), fontWeight = FontWeight.Black) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (isMemorizing) Text(
                    stringResource(R.string.cyber_minigame_sequence, sequence.joinToString(" → ") { (it + 1).toString() }),
                    color = AppColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }
        },
        actions = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = .1f))) { Text(stringResource(R.string.close)) } }
    )
}

@Composable
fun DistressSignalDialog(
    reward: Double,
    onSalvage: () -> Unit,
    onRescue: () -> Unit,
    onDismiss: () -> Unit
) {
    SpaceDialog(
        title = stringResource(R.string.event_distress_signal),
        onDismiss = onDismiss,
        content = {
            Image(
                painter = painterResource(R.drawable.event_distress_background_v2),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.event_choice_instruction),
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(10.dp))
            EventChoicePreview(
                title = stringResource(R.string.event_salvage),
                description = stringResource(R.string.event_salvage_explained, formatNum(reward)),
                accent = AppColors.Primary
            )
            Spacer(Modifier.height(8.dp))
            EventChoicePreview(
                title = stringResource(R.string.event_rescue),
                description = stringResource(R.string.event_rescue_explained, formatNum(reward * 3.0)),
                accent = AppColors.Warning
            )
        },
        actions = {
            Button(
                onClick = onSalvage,
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.10f), contentColor = Color.White)
            ) { Text(stringResource(R.string.event_salvage), fontWeight = FontWeight.Bold) }
            Button(
                onClick = onRescue,
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = Color.Black)
            ) { Text(stringResource(R.string.event_rescue), fontWeight = FontWeight.Black) }
        }
    )
}

@Composable
fun EventChainResultDialog(result: EventChainResult, onDismiss: () -> Unit) {
    val isStation = result.eventType == GameEventType.ABANDONED_STATION
    val isCyberVirus = result.eventType == GameEventType.CYBER_VIRUS
    SpaceDialog(
        title = stringResource(if (result.success) R.string.event_action_success else R.string.event_rescue_failed),
        onDismiss = onDismiss,
        content = {
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
            , color = Color.White.copy(alpha = .82f), lineHeight = 19.sp)
        },
        actions = { Button(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } }
    )
}

@Composable
fun AbandonedStationDialog(
    reward: Double,
    onSafeRoute: () -> Unit,
    onReactorCore: () -> Unit,
    onDismiss: () -> Unit
) {
    SpaceDialog(
        title = stringResource(R.string.event_abandoned_station),
        onDismiss = onDismiss,
        content = {
            Text(stringResource(R.string.event_choice_instruction), color = Color.White.copy(alpha = .82f))
            Spacer(Modifier.height(10.dp))
            EventChoicePreview(stringResource(R.string.event_station_safe), stringResource(R.string.event_station_safe_explained, formatNum(reward * 1.5)), AppColors.Primary)
            Spacer(Modifier.height(8.dp))
            EventChoicePreview(stringResource(R.string.event_station_reactor), stringResource(R.string.event_station_reactor_explained, formatNum(reward * 5.0)), AppColors.Danger)
        },
        actions = {
            Button(onClick = onSafeRoute, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = .1f))) { Text(stringResource(R.string.event_station_safe), maxLines = 2, textAlign = TextAlign.Center) }
            Button(onClick = onReactorCore, modifier = Modifier.weight(1f).height(52.dp)) { Text(stringResource(R.string.event_station_reactor), maxLines = 2, textAlign = TextAlign.Center) }
        }
    )
}

@Composable
private fun EventChoicePreview(title: String, description: String, accent: Color) {
    Column(
        Modifier.fillMaxWidth()
            .background(accent.copy(alpha = .08f), RoundedCornerShape(12.dp))
            .border(1.dp, accent.copy(alpha = .28f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(title, color = accent, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(description, color = Color.White.copy(alpha = .76f), fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun TextButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Button(onClick = onClick, style = CosmicButtonStyle.Secondary) { content() }
}

/** Event sprites are already strongly animated; a platform ripple briefly draws a
 * rectangular layer over transparent PNGs and looks like a black flash. */
@Composable
private fun Modifier.eventClickable(onClick: () -> Unit): Modifier = clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick
)
