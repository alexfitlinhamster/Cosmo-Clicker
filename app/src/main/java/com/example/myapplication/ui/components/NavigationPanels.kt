package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.AchievementEngine
import com.example.myapplication.DroneTraitEngine
import com.example.myapplication.EconomyBalance
import com.example.myapplication.GameState
import com.example.myapplication.GameViewModel
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum
import kotlin.math.pow

@Composable
fun ShopBar(viewModel: GameViewModel, state: GameState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Card(
        modifier = modifier.fillMaxWidth().fillMaxHeight(0.80f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(18.dp)) {
            SpaceSheetHeader(stringResource(R.string.case_shop_title), stringResource(R.string.case_shop_subtitle), onClose)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(R.string.mystery_case, R.string.tab_planets, R.string.tab_click).forEachIndexed { index, title ->
                    SpaceTab(stringResource(title), selectedTab == index, { selectedTab = index }, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                when (selectedTab) {
                    0 -> item { MysteryCaseRow(viewModel, state) }
                    1 -> items(viewModel.planets.toList(), key = { it.first }) { (id, planet) ->
                        val active = state.currentPlanetId == id
                        val owned = id in state.ownedPlanets
                        PlanetRow(
                            name = localizedPlanetName(id),
                            desc = localizedPlanetDescription(id),
                            bonus = localizedPlanetBonus(id) + "\n" + stringResource(
                                R.string.planet_income_multiplier,
                                formatNum(EconomyBalance.planetIncomeMultiplier(id))
                            ),
                            price = planet.price.toLong(),
                            active = active,
                            owned = owned,
                            canBuy = state.totalDebris >= planet.price && !active,
                            iconRes = planet.imageRes,
                            spriteIndex = planet.spriteIndex,
                            showLock = !owned,
                            onClick = { viewModel.buyPlanet(id) }
                        )
                    }
                    else -> items(viewModel.clickItems, key = { it.id }) { upgrade ->
                        val level = state.clickLevels[upgrade.id] ?: 0
                        val cost = (upgrade.base * 1.15.pow(level.toDouble())).toLong()
                        ShopRow(
                            name = localizedUpgradeName(upgrade.id),
                            meta = stringResource(R.string.click_meta, formatNum(upgrade.value), level),
                            cost = cost,
                            canBuy = state.totalDebris >= cost,
                            canSell = false,
                            iconRes = upgrade.iconRes,
                            showLock = level == 0,
                            onBuy = { viewModel.buyClickUpgrade(upgrade.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DroneHangarPanel(viewModel: GameViewModel, state: GameState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    SpacePanel(stringResource(R.string.drone_hangar_title), stringResource(R.string.drone_hangar_subtitle), onClose, modifier) {
        item {
            val active = state.activeFleetCounts.values.sum()
            val owned = state.fleetCounts.values.sum()
            Text(stringResource(R.string.hangar_status, active, owned), color = AppColors.Secondary, modifier = Modifier.padding(bottom = 10.dp))
        }
        items(viewModel.fleetItems, key = { it.id }) { drone ->
            val count = state.fleetCounts[drone.id] ?: 0
            val activeCount = state.activeFleetCounts[drone.id] ?: 0
            val discovered = drone.id in state.discoveredDroneIds || count > 0
            ShopRow(
                name = if (discovered) drone.name else "???",
                meta = if (discovered) stringResource(
                    R.string.drone_storage_meta,
                    count,
                    activeCount,
                    com.example.myapplication.MetaProgressEngine.masteryLevel(state.droneParts[drone.id] ?: 0),
                    state.droneParts[drone.id] ?: 0
                ) else stringResource(R.string.collection_drone_unknown),
                cost = 0,
                canBuy = false,
                canSell = count > 0,
                iconRes = drone.iconRes,
                spriteIndex = drone.spriteIndex,
                showLock = !discovered,
                fleetActionLabel = when {
                    activeCount > 0 -> stringResource(R.string.send_to_storage)
                    count > 0 -> stringResource(R.string.send_to_flight)
                    else -> null
                },
                fleetActionEnabled = activeCount > 0 || state.activeFleetCounts.values.sum() < DroneTraitEngine.MAX_ACTIVE_DRONES,
                onFleetAction = { if (activeCount > 0) viewModel.recallDrone(drone.id) else viewModel.deployDrone(drone.id) },
                onBuy = {},
                onSell = { viewModel.sellFleet(drone.id) }
            )
        }
    }
}

@Composable
fun AchievementsPanel(viewModel: GameViewModel, state: GameState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    SpacePanel(stringResource(R.string.achievements), stringResource(R.string.achievements_subtitle), onClose, modifier) {
        items(AchievementEngine.definitions, key = { it.id }) { achievement ->
            val unlocked = achievement.id in state.unlockedAchievementIds
            val claimed = achievement.id in state.claimedAchievementIds
            val reward = if (achievement.rewardPrestigePoints > 0) {
                stringResource(R.string.achievement_reward_points, achievement.rewardPrestigePoints)
            } else {
                stringResource(R.string.achievement_reward_debris, formatNum(EconomyBalance.scaledReward(achievement.rewardDebris, state.currentPlanetId)))
            }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(if (unlocked) AppColors.Warning.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.035f), RoundedCornerShape(14.dp))
                    .border(1.dp, if (unlocked) AppColors.Warning.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(achievementTitle(achievement.id)), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(reward, color = AppColors.Secondary, fontSize = 10.sp)
                }
                Button(onClick = { viewModel.claimAchievement(achievement.id) }, enabled = unlocked && !claimed) {
                    Text(stringResource(if (claimed) R.string.achievement_claimed else if (unlocked) R.string.achievement_claim else R.string.achievement_locked), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun SpacePanel(title: String, subtitle: String, onClose: () -> Unit, modifier: Modifier, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().fillMaxHeight(0.80f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(18.dp)) {
            SpaceSheetHeader(title, subtitle, onClose)
            Spacer(Modifier.height(14.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
        }
    }
}

@Composable fun HangarLauncherButton(onClick: () -> Unit) = LauncherIcon(R.drawable.ui_button_hangar, R.string.open_hangar, onClick)
@Composable fun AchievementsLauncherButton(onClick: () -> Unit) = LauncherIcon(R.drawable.ui_button_achievements, R.string.open_achievements, onClick)

@Composable
private fun LauncherIcon(icon: Int, description: Int, onClick: () -> Unit) {
    Image(painterResource(icon), stringResource(description), Modifier.size(60.dp).clickable(onClick = onClick), contentScale = ContentScale.Fit)
}

private fun achievementTitle(id: String): Int = when (id) {
    "click_100" -> R.string.achievement_click_100
    "click_10000" -> R.string.achievement_click_10000
    "fleet_5" -> R.string.achievement_fleet_5
    "fleet_12" -> R.string.achievement_fleet_12
    "planets_5" -> R.string.achievement_planets_5
    "planets_10" -> R.string.achievement_planets_10
    "planets_20" -> R.string.achievement_planets_20
    "events_10" -> R.string.achievement_events_10
    "prestige_1" -> R.string.achievement_prestige_1
    else -> R.string.unknown_item
}
