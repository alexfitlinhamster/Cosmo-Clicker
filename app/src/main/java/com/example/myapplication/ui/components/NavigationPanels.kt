package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
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
                listOf(R.string.mystery_case, R.string.tab_planets, R.string.tab_click, R.string.utility_upgrades).forEachIndexed { index, title ->
                    SpaceTab(stringResource(title), selectedTab == index, { selectedTab = index }, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = AppColors.Primary.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.22f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painterResource(R.drawable.debris_01), null, Modifier.size(28.dp), contentScale = ContentScale.Fit)
                    Spacer(Modifier.width(9.dp))
                    Text(stringResource(R.string.shop_balance), color = Color.LightGray, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text(formatNum(state.totalDebris), color = AppColors.Primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
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
                    2 -> items(viewModel.clickItems, key = { it.id }) { upgrade ->
                        val level = state.clickLevels[upgrade.id] ?: 0
                        val marketMultiplier = if (state.weeklyGalaxy.active && state.weeklyGalaxy.rule == com.example.myapplication.WeeklyRule.VOLATILE_MARKET) {
                            com.example.myapplication.FeatureEngine.volatilePriceMultiplier()
                        } else 1.0
                        val cost = EconomyBalance.clickUpgradeCost(upgrade.base, level, marketMultiplier).toLong()
                        ClickUpgradeRow(
                            name = localizedUpgradeName(upgrade.id),
                            meta = stringResource(R.string.click_meta, formatNum(upgrade.value), level),
                            cost = cost,
                            enabled = level < EconomyBalance.MAX_CLICK_UPGRADE_LEVEL && state.totalDebris >= cost,
                            iconRes = upgrade.iconRes,
                            onBuy = { viewModel.buyClickUpgrade(upgrade.id) }
                        )
                    }
                    else -> items(listOf("flight", "spawn", "magnet")) { id ->
                        val level = viewModel.utilityUpgradeLevel(id)
                        val max = if (id == "flight") 2 else 5
                        val cost = viewModel.utilityUpgradeCost(id, level)
                        ClickUpgradeRow(
                            name = stringResource(when (id) { "flight" -> R.string.upgrade_flight_slots; "spawn" -> R.string.upgrade_spawn_speed; else -> R.string.upgrade_magnet_radius }),
                            meta = stringResource(R.string.utility_level, level, max),
                            cost = cost.toLong(),
                            enabled = level < max && state.totalDebris >= cost,
                            iconRes = if (id == "magnet") R.drawable.upgrade_magnet_v2 else R.drawable.debris_01,
                            onBuy = { viewModel.buyUtilityUpgrade(id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClickUpgradeRow(
    name: String,
    meta: String,
    cost: Long,
    enabled: Boolean,
    iconRes: Int,
    onBuy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(10.dp),
            color = AppColors.Primary.copy(alpha = 0.09f)
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.padding(5.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                meta,
                color = Color.LightGray,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onBuy,
            enabled = enabled,
            modifier = Modifier.widthIn(min = 82.dp, max = 104.dp).heightIn(min = 38.dp),
            shape = RoundedCornerShape(9.dp),
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = Color.Black)
        ) {
            Text(
                formatNum(cost.toDouble()),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DroneHangarPanel(viewModel: GameViewModel, state: GameState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    SpacePanel(stringResource(R.string.drone_hangar_title), stringResource(R.string.drone_hangar_subtitle), onClose, modifier) {
        item {
            val active = state.activeFleetCounts.values.sum()
            val owned = state.fleetCounts.values.sum()
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Primary.copy(alpha = 0.07f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.20f))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.hangar_overview), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    HangarCapacityLine(stringResource(R.string.drones_in_flight), active, viewModel.activeDroneCapacity(state), AppColors.Primary)
                    Text(stringResource(R.string.drones_in_storage_count, owned), color = AppColors.Secondary, fontSize = 11.sp)
                }
            }
        }
        items(viewModel.fleetItems.chunked(3), key = { row -> row.first().id }) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { drone ->
                    CompactHangarDroneCard(drone, viewModel, state, Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
internal fun CompactHangarDroneCard(
    drone: com.example.myapplication.FleetConfig,
    viewModel: GameViewModel,
    state: GameState,
    modifier: Modifier = Modifier
) {
    val count = state.fleetCounts[drone.id] ?: 0
    val active = state.activeFleetCounts[drone.id] ?: 0
    val discovered = drone.id in state.discoveredDroneIds || count > 0
    val canDeploy = active > 0 || (count > 0 && state.activeFleetCounts.values.sum() < viewModel.activeDroneCapacity(state))
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (active > 0) AppColors.Primary.copy(alpha = .12f) else Color.White.copy(alpha = .04f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (active > 0) AppColors.Primary.copy(alpha = .45f) else Color.White.copy(alpha = .08f))
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(drone.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(58.dp).clip(CircleShape),
                    contentScale = ContentScale.Fit,
                    alpha = if (discovered) 1f else .18f
                )
                if (!discovered) Text("?", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            Text(if (discovered) drone.name else "???", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$active / $count", color = if (active > 0) AppColors.Primary else Color.Gray, fontSize = 10.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = { if (active > 0) viewModel.recallDrone(drone.id) else viewModel.deployDrone(drone.id) },
                    enabled = canDeploy,
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(alpha = .55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary)
                ) { Text(if (active > 0) "↓" else "↑", fontSize = 15.sp) }
                OutlinedButton(
                    onClick = { viewModel.sellFleet(drone.id) },
                    enabled = count > 0,
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Danger.copy(alpha = .45f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Danger)
                ) { Text("−", fontSize = 15.sp) }
            }
        }
    }
}

@Composable
private fun HangarCapacityLine(label: String, value: Int, maximum: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.LightGray, fontSize = 11.sp)
            Text("$value / $maximum", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { (value.toFloat() / maximum.coerceAtLeast(1)).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
            color = color,
            trackColor = Color.White.copy(alpha = 0.10f)
        )
    }
}

@Composable
fun AchievementsPanel(viewModel: GameViewModel, state: GameState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Card(
        modifier = modifier.fillMaxWidth().fillMaxHeight(0.80f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(18.dp)) {
            SpaceSheetHeader(stringResource(R.string.statistics_and_achievements), stringResource(R.string.statistics_subtitle), onClose)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(R.string.statistics, R.string.achievements).forEachIndexed { index, title ->
                    SpaceTab(stringResource(title), selectedTab == index, { selectedTab = index }, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (selectedTab == 0) {
                    val stats = listOf(
                        R.string.stat_planets to "${state.ownedPlanets.size} / ${viewModel.planets.size}",
                        R.string.stat_clicks to formatNum(state.lifetimeStats.clicks.toDouble()),
                        R.string.stat_debris_collected to formatNum(state.lifetimeStats.debrisCollected.toDouble()),
                        R.string.stat_drones_discovered to "${state.discoveredDroneIds.size} / ${viewModel.fleetItems.size}",
                        R.string.stat_cases_opened to formatNum(state.lifetimeStats.casesOpened.toDouble()),
                        R.string.stat_events_completed to formatNum(state.lifetimeStats.eventsCompleted.toDouble()),
                        R.string.stat_prestiges to formatNum(state.lifetimeStats.prestiges.toDouble()),
                        R.string.stat_achievements to "${state.claimedAchievementIds.size} / ${AchievementEngine.definitions.size}"
                    )
                    items(stats, key = { it.first }) { (label, value) -> StatisticRow(stringResource(label), value) }
                } else items(AchievementEngine.definitions, key = { it.id }) { achievement ->
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
    }
}

@Composable
private fun StatisticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), color = Color.White, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(12.dp))
        Text(value, color = AppColors.Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
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

@Composable fun HangarLauncherButton(onClick: () -> Unit) = LauncherIcon(R.drawable.ui_button_hangar_v2, R.string.open_hangar, onClick)
@Composable fun AchievementsLauncherButton(onClick: () -> Unit) = LauncherIcon(R.drawable.ui_button_achievements_v2, R.string.open_achievements, onClick)

@Composable
private fun LauncherIcon(icon: Int, description: Int, onClick: () -> Unit) {
    Image(painterResource(icon), stringResource(description), Modifier.size(60.dp).clickable(onClick = onClick), contentScale = ContentScale.Fit)
}

private fun achievementTitle(id: String): Int = when (id) {
    "click_100" -> R.string.achievement_click_100
    "click_1000" -> R.string.achievement_click_1000
    "click_10000" -> R.string.achievement_click_10000
    "click_100000" -> R.string.achievement_click_100000
    "fleet_5" -> R.string.achievement_fleet_5
    "fleet_12" -> R.string.achievement_fleet_12
    "fleet_50" -> R.string.achievement_fleet_50
    "collection_15" -> R.string.achievement_collection_15
    "collection_29" -> R.string.achievement_collection_29
    "planets_5" -> R.string.achievement_planets_5
    "planets_10" -> R.string.achievement_planets_10
    "planets_20" -> R.string.achievement_planets_20
    "planets_24" -> R.string.achievement_planets_24
    "events_10" -> R.string.achievement_events_10
    "events_50" -> R.string.achievement_events_50
    "cases_25" -> R.string.achievement_cases_25
    "cases_100" -> R.string.achievement_cases_100
    "prestige_1" -> R.string.achievement_prestige_1
    "prestige_5" -> R.string.achievement_prestige_5
    "click_250k" -> R.string.achievement_click_250k
    "click_1m" -> R.string.achievement_click_1m
    "click_5m" -> R.string.achievement_click_5m
    "debris_10m" -> R.string.achievement_debris_10m
    "debris_100m" -> R.string.achievement_debris_100m
    "debris_1b" -> R.string.achievement_debris_1b
    "debris_10b" -> R.string.achievement_debris_10b
    "cases_250" -> R.string.achievement_cases_250
    "cases_500" -> R.string.achievement_cases_500
    "cases_1000" -> R.string.achievement_cases_1000
    "events_100" -> R.string.achievement_events_100
    "events_250" -> R.string.achievement_events_250
    "events_500" -> R.string.achievement_events_500
    "prestige_10" -> R.string.achievement_prestige_10
    "prestige_25" -> R.string.achievement_prestige_25
    "prestige_50" -> R.string.achievement_prestige_50
    "fleet_100" -> R.string.achievement_fleet_100
    "fleet_250" -> R.string.achievement_fleet_250
    "fleet_500" -> R.string.achievement_fleet_500
    "parts_25" -> R.string.achievement_parts_25
    "parts_100" -> R.string.achievement_parts_100
    "parts_300" -> R.string.achievement_parts_300
    "station_5" -> R.string.achievement_station_5
    "station_10" -> R.string.achievement_station_10
    "station_20" -> R.string.achievement_station_20
    "planets_21" -> R.string.achievement_planets_21
    "planets_22" -> R.string.achievement_planets_22
    "planets_23" -> R.string.achievement_planets_23
    "wealth_1t" -> R.string.achievement_wealth_1t
    "wealth_1q" -> R.string.achievement_wealth_1q
    "tech_all" -> R.string.achievement_tech_all
    "achievements_40" -> R.string.achievement_achievements_40
    else -> R.string.unknown_item
}
