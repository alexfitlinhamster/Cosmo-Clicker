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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex - 2).coerceAtLeast(0))
    LaunchedEffect(currentIndex) { listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0)) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.92f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF071426),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(.35f))
        ) {
            Column(Modifier.padding(16.dp)) {
                SpaceSheetHeader(stringResource(GameR.string.galaxy_route), stringResource(GameR.string.galaxy_route_hint), onDismiss)
                Spacer(Modifier.height(10.dp))
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    items(EconomyBalance.MAX_PLANET_INDEX, key = { it + 1 }) { zeroIndex ->
                        val index = zeroIndex + 1
                        val id = "p$index"
                        val planet = viewModel.planets.getValue(id)
                        val owned = id in state.ownedPlanets
                        val revealed = owned || index == nextIndex
                        val current = id == state.currentPlanetId
                        val requiredFleet = EconomyBalance.requiredActiveDronesForPlanet(index)
                        val requiredDrone = EconomyBalance.requiredDroneIdForPlanet(index)
                        val requiredPrestige = EconomyBalance.requiredPrestigeForPlanet(index)
                        val fleetReady = EconomyBalance.planetFleetObjectiveMet(state, index)
                        val hasDroneObjective = requiredDrone != null
                        val hasPrestigeObjective = requiredPrestige > EconomyBalance.requiredPrestigeForPlanet(index - 1)
                        Column(Modifier.fillMaxWidth()) {
                            if (hasDroneObjective) {
                                RouteObjectiveNode(
                                    iconRes = GameResourceRegistry.drone(requiredDrone!!.removePrefix("drone_").toIntOrNull() ?: 1),
                                    title = stringResource(GameR.string.next_goal_drone, requiredDrone.removePrefix("drone_")),
                                    completed = requiredDrone in state.discoveredDroneIds,
                                    revealed = owned || index == nextIndex
                                )
                            }
                            if (hasPrestigeObjective) {
                                RouteObjectiveNode(
                                    iconRes = GameR.drawable.ic_prestige_core,
                                    title = stringResource(GameR.string.next_goal_prestige, state.lifetimeStats.prestiges, requiredPrestige),
                                    completed = state.lifetimeStats.prestiges >= requiredPrestige,
                                    revealed = owned || index == nextIndex
                                )
                            }
                        Box(Modifier.fillMaxWidth()) {
                            if (index < EconomyBalance.MAX_PLANET_INDEX) {
                                Box(Modifier.align(Alignment.BottomCenter).offset(y = 22.dp).width(3.dp).height(38.dp).background(if (owned) AppColors.Primary else Color.DarkGray))
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                    .background(if (current) AppColors.Primary.copy(.12f) else Color.White.copy(.035f), RoundedCornerShape(16.dp))
                                    .border(1.dp, if (current) AppColors.Primary else Color.White.copy(.08f), RoundedCornerShape(16.dp))
                                    .clickable(enabled = owned || (index == nextIndex && state.totalDebris >= planet.price && fleetReady)) { viewModel.buyPlanet(id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(74.dp).clip(CircleShape).background(Color.Black.copy(.3f)), contentAlignment = Alignment.Center) {
                                    if (revealed) Image(painterResource(planet.imageRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                    else {
                                        Image(painterResource(planet.imageRes), null, Modifier.fillMaxSize().alpha(.08f), contentScale = ContentScale.Fit)
                                        Text("?", color = Color.Gray, fontSize = 30.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(if (revealed) localizedPlanetName(id) else stringResource(GameR.string.unknown_sector), color = if (current) AppColors.Primary else Color.White, fontWeight = FontWeight.Bold)
                                    if (revealed) Text(localizedPlanetBonus(id), color = AppColors.Secondary, fontSize = 11.sp)
                                    when {
                                        current -> Text(stringResource(GameR.string.current_world), color = AppColors.Warning, fontSize = 11.sp)
                                        owned -> Text(stringResource(GameR.string.select), color = Color.LightGray, fontSize = 11.sp)
                                        index == nextIndex -> {
                                            Text(formatNum(planet.price), color = AppColors.Warning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            if (requiredDrone != null && requiredDrone !in state.discoveredDroneIds) {
                                                Text(stringResource(GameR.string.next_goal_drone, requiredDrone.removePrefix("drone_")), color = AppColors.Warning, fontSize = 10.sp)
                                            } else if (state.lifetimeStats.prestiges < requiredPrestige) {
                                                Text(stringResource(GameR.string.next_goal_prestige, state.lifetimeStats.prestiges, requiredPrestige), color = AppColors.Warning, fontSize = 10.sp)
                                            } else if (requiredFleet > 0) {
                                                Text(stringResource(GameR.string.next_goal_fleet, state.activeFleetCounts.values.sum(), requiredFleet), color = if (fleetReady) AppColors.Primary else AppColors.Warning, fontSize = 10.sp)
                                            }
                                        }
                                        else -> Text("••••••", color = Color.DarkGray, fontSize = 11.sp)
                                    }
                                }
                                Text("$index", color = Color.White.copy(.38f), fontWeight = FontWeight.Black)
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteObjectiveNode(
    iconRes: Int,
    title: String,
    completed: Boolean,
    revealed: Boolean
) {
    val accent = if (completed) AppColors.Primary else AppColors.Warning
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .background(accent.copy(alpha = if (revealed) .10f else .035f), RoundedCornerShape(14.dp))
            .border(1.dp, accent.copy(alpha = if (revealed) .38f else .10f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(50.dp).clip(CircleShape).background(Color.Black.copy(.28f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painterResource(iconRes),
                null,
                Modifier.size(46.dp).alpha(if (revealed) 1f else .12f),
                contentScale = ContentScale.Fit
            )
            if (!revealed) Text("?", color = Color.Gray, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(GameR.string.route_objective), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(if (revealed) title else stringResource(GameR.string.unknown_sector), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            if (completed) "✓" else "○",
            color = accent,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.width(3.dp).height(16.dp).background(if (completed) AppColors.Primary else Color.DarkGray))
    }
}
