package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
            .eventClickable(onClick),
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
                GameEventType.ASTEROID, GameEventType.STORM, GameEventType.SOLAR_FLARE ->
                    stringResource(R.string.event_taps_left, tapsLeft)
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
    GameEventType.ASTEROID -> R.drawable.asteroid_gold_game
    GameEventType.METEOR_SHOWER -> R.drawable.event_meteor_hazard_v2
    GameEventType.DISTRESS_SIGNAL -> R.drawable.event_rescue_capsule
    GameEventType.ABANDONED_STATION -> R.drawable.event_abandoned_station_v2
    GameEventType.SOLAR_FLARE -> R.drawable.event_solar_cooler_v2
    GameEventType.TRADING_SHIP -> R.drawable.event_trading_ship_game
    GameEventType.PIRATE_RAID -> R.drawable.event_pirate_ship_v2
    GameEventType.BLACK_HOLE -> R.drawable.event_black_hole_v2
    GameEventType.CYBER_VIRUS -> R.drawable.event_cyber_module_v2
    GameEventType.STORM -> R.drawable.event_storm_node_v2
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
        R.drawable.event_storm_node_v2
    } else {
        R.drawable.event_solar_cooler_v2
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
            .eventClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.asteroid_gold_game),
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
                x = gameAreaWidth * event.x - blackHoleSize / 2,
                y = gameAreaHeight * event.y - blackHoleSize / 2
            )
            .size(blackHoleSize)
            .eventClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.event_black_hole_v2),
            contentDescription = stringResource(R.string.event_black_hole),
            modifier = Modifier.fillMaxSize().graphicsLayer {
                rotationZ = rotation
                scaleX = pulse
                scaleY = pulse
            }
        )
        Text(
            tapsLeft.toString(),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.72f), CircleShape)
                .border(1.dp, Color(0xFFCE5CFF), CircleShape)
                .padding(horizontal = 9.dp, vertical = 5.dp)
        )
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
    val stabilized = ((10 - tapsLeft).coerceIn(0, 10)) / 10f

    SpaceDialog(
        title = stringResource(R.string.event_black_hole),
        onDismiss = onDismiss,
        content = {
            Image(
                painter = painterResource(R.drawable.event_black_hole_v2),
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
                Text(stringResource(R.string.event_taps_left, tapsLeft), color = Color.White, fontWeight = FontWeight.Bold)
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
            .eventClickable(onClick),
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
fun TradingShipComponent(
    event: GameEvent,
    gameAreaWidth: Dp,
    gameAreaHeight: Dp,
    onClick: () -> Unit
) {
    val shipSize = 112.dp
    Box(
        modifier = Modifier
            .offset(
                x = gameAreaWidth * event.x - shipSize / 2,
                y = gameAreaHeight * event.y - shipSize / 2
            )
            .size(shipSize)
            .eventClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.event_trading_ship_game),
            contentDescription = stringResource(R.string.event_trading_ship),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun TradingShipMarket(
    event: GameEvent,
    totalDebris: Double,
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.trade_market_title), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.trade_market_subtitle), color = Color.White.copy(alpha = .62f), fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Image(
                painter = painterResource(R.drawable.event_trading_ship_game),
                contentDescription = null,
                modifier = Modifier.size(132.dp)
            )
            Text(stringResource(R.string.trade_balance, formatNum(totalDebris)), color = Color(0xFF66E0FF), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            offers.forEach { offer ->
                val cost = EventEngine.tradeOfferCost(event, offer)
                TradeOfferCard(
                    offer = offer,
                    cost = cost,
                    enabled = totalDebris >= cost,
                    onBuy = { onBuy(offer) }
                )
                Spacer(Modifier.height(8.dp))
            }
            androidx.compose.material3.TextButton(onClick = onDismiss) {
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
    SpaceDialog(
        title = stringResource(
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
        onDismiss = onDismiss,
        content = {
            Image(
                painter = painterResource(eventIconResource(event.type)),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(14.dp))
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
                ), color = Color.White.copy(alpha = .82f), lineHeight = 19.sp
            )
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
                    painter = painterResource(R.drawable.event_cyber_terminal_v3),
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
                            androidx.compose.material3.Button(
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
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.10f), contentColor = Color.White)
            ) { Text(stringResource(R.string.event_salvage), fontWeight = FontWeight.Bold) }
            Button(
                onClick = onRescue,
                modifier = Modifier.weight(1f),
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
            Button(onClick = onSafeRoute, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = .1f))) { Text(stringResource(R.string.event_station_safe)) }
            Button(onClick = onReactorCore, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.event_station_reactor)) }
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
    androidx.compose.material3.TextButton(onClick = onClick) {
        content()
    }
}

/** Event sprites are already strongly animated; a platform ripple briefly draws a
 * rectangular layer over transparent PNGs and looks like a black flash. */
@Composable
private fun Modifier.eventClickable(onClick: () -> Unit): Modifier = clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick
)
