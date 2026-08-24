package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.*
import com.example.myapplication.R as GameR
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum

@Composable
fun GalaxyRouteDialog(viewModel: GameViewModel, state: GameState, onDismiss: () -> Unit) {
    val currentIndex = EconomyBalance.planetIndex(state.currentPlanetId)
    val nextIndex = EconomyBalance.nextPlanetIndex(state.ownedPlanets)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex - 1).coerceAtLeast(0))
    LaunchedEffect(currentIndex) { listState.animateScrollToItem((currentIndex - 1).coerceAtLeast(0)) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.9f),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5AD9FF).copy(.28f))
        ) {
            Column(
                Modifier
                    .paint(painterResource(GameR.drawable.bg_goals_starchart_v1), contentScale = ContentScale.Crop)
                    .background(Color(0x44030A16))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                SpaceSheetHeader(
                    stringResource(GameR.string.galaxy_route),
                    stringResource(GameR.string.galaxy_route_hint),
                    onDismiss
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(state = listState, contentPadding = PaddingValues(vertical = 6.dp)) {
                    items(EconomyBalance.MAX_PLANET_INDEX, key = { it + 1 }) { zeroIndex ->
                        val index = zeroIndex + 1
                        val id = "p$index"
                        val planet = viewModel.planets.getValue(id)
                        val owned = id in state.ownedPlanets
                        val isNext = index == nextIndex
                        val revealed = owned || isNext
                        val current = id == state.currentPlanetId
                        val requiredFleet = EconomyBalance.requiredActiveDronesForPlanet(index)
                        val requiredDrone = EconomyBalance.requiredDroneIdForPlanet(index)
                        val requiredPrestige = EconomyBalance.requiredPrestigeForPlanet(index)
                        val fleetReady = EconomyBalance.planetFleetObjectiveMet(state, index)
                        val canBuy = isNext && state.totalDebris >= planet.price && fleetReady

                        if (requiredDrone != null) {
                            RouteCheckpoint(
                                iconRes = GameResourceRegistry.drone(requiredDrone.removePrefix("drone_").toIntOrNull() ?: 1),
                                text = stringResource(GameR.string.next_goal_drone),
                                completed = requiredDrone in state.discoveredDroneIds,
                                visible = isNext || owned,
                                alignRight = index % 2 == 0
                            )
                        }
                        if (requiredPrestige > EconomyBalance.requiredPrestigeForPlanet(index - 1)) {
                            RouteCheckpoint(
                                iconRes = GameR.drawable.ic_prestige_hologram_v2,
                                text = stringResource(GameR.string.next_goal_prestige, state.lifetimeStats.prestiges, requiredPrestige),
                                completed = state.lifetimeStats.prestiges >= requiredPrestige,
                                visible = isNext || owned,
                                alignRight = index % 2 == 0
                            )
                        }

                        PlanetRouteNode(
                            planet = planet,
                            id = id,
                            revealed = revealed,
                            owned = owned,
                            current = current,
                            isNext = isNext,
                            canBuy = canBuy,
                            isLast = index == EconomyBalance.MAX_PLANET_INDEX,
                            alignRight = index % 2 == 0,
                            price = planet.price,
                            requirement = when {
                                requiredDrone != null && requiredDrone !in state.discoveredDroneIds ->
                                    stringResource(GameR.string.next_goal_drone)
                                state.lifetimeStats.prestiges < requiredPrestige ->
                                    stringResource(GameR.string.next_goal_prestige, state.lifetimeStats.prestiges, requiredPrestige)
                                requiredFleet > 0 && !fleetReady ->
                                    stringResource(GameR.string.next_goal_fleet, state.activeFleetCounts.values.sum(), requiredFleet)
                                else -> null
                            },
                            onClick = { viewModel.buyPlanet(id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanetRouteNode(
    planet: PlanetConfig,
    id: String,
    revealed: Boolean,
    owned: Boolean,
    current: Boolean,
    isNext: Boolean,
    canBuy: Boolean,
    isLast: Boolean,
    alignRight: Boolean,
    price: Double,
    requirement: String?,
    onClick: () -> Unit
) {
    val accent = when {
        current -> Color(0xFF65E6FF)
        owned -> Color(0xFF41D6A3)
        isNext -> Color(0xFFFFC857)
        else -> Color(0xFF465069)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .scale(scaleX = if (alignRight) -1f else 1f, scaleY = 1f)
    ) {
        Column(Modifier.width(76.dp).fillMaxHeight().scale(scaleX = if (alignRight) -1f else 1f, scaleY = 1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(68.dp)
                    .shadow(if (current) 18.dp else 4.dp, CircleShape, spotColor = accent)
                    .background(Color(0xFF091122), CircleShape)
                    .border(if (current) 3.dp else 1.dp, accent, CircleShape)
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painterResource(planet.imageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(if (revealed) 1f else .1f),
                    contentScale = ContentScale.Fit
                )
                if (!revealed) Text("?", color = Color(0xFF778199), fontSize = 24.sp, fontWeight = FontWeight.Black)
                if (owned) StatusDot(Modifier.align(Alignment.BottomEnd), Color(0xFF41D6A3), "✓")
            }
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(Brush.verticalGradient(listOf(accent.copy(.8f), Color(0xFF283149))))
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(
            Modifier
                .weight(1f)
                .scale(scaleX = if (alignRight) -1f else 1f, scaleY = 1f)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (current) Color(0xFF123047) else Color.White.copy(.045f))
                .border(1.dp, accent.copy(if (current || isNext) .65f else .18f), RoundedCornerShape(18.dp))
                .clickable(enabled = owned || canBuy, onClick = onClick)
                .padding(horizontal = 13.dp, vertical = 11.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (revealed) localizedPlanetName(id) else stringResource(GameR.string.unknown_sector),
                    modifier = Modifier.weight(1f),
                    color = if (revealed) Color.White else Color(0xFF778199),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                RouteStatePill(
                    text = when {
                        current -> stringResource(GameR.string.current_world)
                        owned -> stringResource(GameR.string.select)
                        isNext -> formatNum(price)
                        else -> "•••"
                    },
                    color = accent
                )
            }
            if (revealed) {
                Text(localizedPlanetBonus(id), color = AppColors.Secondary, fontSize = 11.sp, maxLines = 2, lineHeight = 13.sp)
            }
            if (requirement != null) {
                Spacer(Modifier.height(5.dp))
                Text(requirement, color = Color(0xFFFFB86B), fontSize = 11.sp, maxLines = 2, lineHeight = 13.sp)
            }
        }
    }
}

@Composable
private fun RouteCheckpoint(iconRes: Int, text: String, completed: Boolean, visible: Boolean, alignRight: Boolean) {
    val accent = if (completed) Color(0xFF41D6A3) else Color(0xFFFFC857)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).scale(scaleX = if (alignRight) -1f else 1f, scaleY = 1f)) {
        Column(Modifier.width(76.dp).fillMaxHeight().scale(scaleX = if (alignRight) -1f else 1f, scaleY = 1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(42.dp).background(Color(0xFF10182A), CircleShape).border(1.dp, accent.copy(.7f), CircleShape).padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(iconRes), null, Modifier.fillMaxSize().alpha(if (visible) 1f else .1f), contentScale = ContentScale.Fit)
                if (completed) StatusDot(Modifier.align(Alignment.BottomEnd), accent, "✓")
            }
            Box(Modifier.width(2.dp).weight(1f).background(accent.copy(.45f)))
        }
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier.weight(1f).scale(scaleX = if (alignRight) -1f else 1f, scaleY = 1f).padding(bottom = 10.dp).background(accent.copy(.07f), RoundedCornerShape(14.dp)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(GameR.string.route_objective), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(if (visible) text else stringResource(GameR.string.unknown_sector), color = Color.White.copy(.82f), fontSize = 12.sp, lineHeight = 14.sp)
            }
            Text(stringResource(if (completed) GameR.string.route_done else GameR.string.route_pending), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RouteStatePill(text: String, color: Color) {
    Text(
        text,
        modifier = Modifier.background(color.copy(.14f), RoundedCornerShape(50)).border(1.dp, color.copy(.45f), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp),
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black
    )
}

@Composable
private fun StatusDot(modifier: Modifier, color: Color, label: String) {
    Box(modifier.size(20.dp).background(color, CircleShape).border(2.dp, Color(0xFF071426), CircleShape), contentAlignment = Alignment.Center) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}
