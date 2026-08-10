package com.example.myapplication.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.GameState
import com.example.myapplication.CaseType
import com.example.myapplication.GameResourceRegistry
import com.example.myapplication.GameViewModel
import com.example.myapplication.EconomyBalance
import com.example.myapplication.AchievementEngine
import com.example.myapplication.EventLogOutcome
import com.example.myapplication.GameEventType
import com.example.myapplication.MetaProgressEngine
import com.example.myapplication.R
import com.example.myapplication.Technology
import com.example.myapplication.ui.GameConstants
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
private fun LegacyOperationsPanel(
    viewModel: GameViewModel,
    state: GameState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(R.string.tab_planets, R.string.tab_fleet, R.string.tab_click, R.string.tab_meta)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.80f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
                Image(
                    painter = painterResource(R.drawable.shop_command_header),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(104.dp).clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
                SpaceSheetHeader(
                    title = stringResource(R.string.command_center),
                    subtitle = stringResource(R.string.command_center_subtitle),
                    onClose = onClose
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabs.forEachIndexed { index, title ->
                        SpaceTab(stringResource(title), selectedTab == index, { selectedTab = index }, Modifier.weight(1f))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> {
                            item { LateGalaxyPreview(viewModel) }
                            items(viewModel.planets.toList(), key = { it.first }) { (id, config) ->
                                val active = state.currentPlanetId == id
                                val owned = state.ownedPlanets.contains(id)
                                PlanetRow(
                                    name = localizedPlanetName(id),
                                    desc = localizedPlanetDescription(id),
                                    bonus = localizedPlanetBonus(id) + "\n" + stringResource(
                                        R.string.planet_income_multiplier,
                                        formatNum(EconomyBalance.planetIncomeMultiplier(id))
                                    ),
                                    price = config.price.toLong(),
                                    active = active,
                                    owned = owned,
                                    canBuy = state.totalDebris >= config.price && !active,
                                    iconRes = config.imageRes,
                                    spriteIndex = config.spriteIndex,
                                    showLock = !owned
                                ) { viewModel.buyPlanet(id) }
                            }
                        }
                        1 -> {
                            item {
                                DroneCollectionHeader(state, viewModel)
                            }
                            item {
                                MysteryCaseRow(viewModel, state)
                            }
                            items(viewModel.fleetItems.chunked(3), key = { row -> "hangar_${row.first().id}" }) { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { drone ->
                                        CompactHangarDroneCard(drone, viewModel, state, Modifier.weight(1f))
                                    }
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                            item {
                                Button(
                                    onClick = { viewModel.takeHotelDebt() },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    enabled = !state.isHotelDebtActive,
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger.copy(alpha = 0.1f), contentColor = AppColors.Danger),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Danger)
                                ) {
                                    Text(stringResource(if (state.isHotelDebtActive) R.string.debt_active else R.string.take_hotel_debt))
                                }
                            }
                        }
                        2 -> {
                            items(viewModel.clickItems, key = { it.id }) { item ->
                                val lvl = state.clickLevels[item.id] ?: 0
                                val marketMultiplier = if (state.weeklyGalaxy.active && state.weeklyGalaxy.rule == com.example.myapplication.WeeklyRule.VOLATILE_MARKET) {
                                    com.example.myapplication.FeatureEngine.volatilePriceMultiplier()
                                } else 1.0
                                val cost = EconomyBalance.clickUpgradeCost(item.base, lvl, marketMultiplier).toLong()
                                ShopRow(
                                    name = localizedUpgradeName(item.id),
                                    meta = stringResource(R.string.click_meta, formatNum(item.value), lvl),
                                    cost = cost,
                                    canBuy = lvl < EconomyBalance.MAX_CLICK_UPGRADE_LEVEL && state.totalDebris >= cost,
                                    canSell = false,
                                    iconRes = item.iconRes,
                                    showLock = lvl == 0,
                                    onBuy = { viewModel.buyClickUpgrade(item.id) }
                                )
                            }
                        }
                        3 -> item { MetaProgressPanel(viewModel, state) }
                    }
            }
        }
    }
}

@Composable
private fun DroneCollectionHeader(state: GameState, viewModel: GameViewModel) {
    val discovered = viewModel.fleetItems.count { it.id in state.discoveredDroneIds || (state.fleetCounts[it.id] ?: 0) > 0 }
    val total = viewModel.fleetItems.size.coerceAtLeast(1)
    val milestones = listOf(3, 6, 12, total).distinct()
    val nextMilestone = milestones.firstOrNull { it.toString() !in state.claimedCollectionMilestones }
    val activeCount = state.activeFleetCounts.values.sum()
    val storedCount = state.fleetCounts.values.sum()
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071329))
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(158.dp)) {
            Image(
                painter = painterResource(R.drawable.hangar_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(stringResource(R.string.drone_collection), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(stringResource(R.string.collection_discovered, discovered, total), color = AppColors.Secondary, fontSize = 12.sp)
                Text(stringResource(R.string.hangar_status, activeCount, storedCount), color = Color.White, fontSize = 11.sp)
                LinearProgressIndicator(
                    progress = { discovered.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(5.dp).clip(CircleShape),
                    color = AppColors.Primary,
                    trackColor = Color.White.copy(alpha = 0.18f)
                )
            }
        }
        if (nextMilestone != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.collection_milestone, nextMilestone), color = Color.White, fontSize = 12.sp)
                    Text(stringResource(R.string.collection_reward_amount, formatNum(viewModel.collectionReward(nextMilestone))), color = AppColors.Secondary, fontSize = 10.sp)
                }
                Button(
                    onClick = { viewModel.claimCollectionReward(nextMilestone) },
                    enabled = discovered >= nextMilestone
                ) { Text(stringResource(R.string.collection_claim), fontSize = 10.sp) }
            }
        }
    }
}

@Composable
private fun MetaProgressPanel(viewModel: GameViewModel, state: GameState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(30.dp).background(AppColors.Warning.copy(alpha = .14f), CircleShape)
                    .border(1.dp, AppColors.Warning.copy(alpha = .4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_prestige_core),
                    contentDescription = null,
                    tint = AppColors.Warning,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(stringResource(R.string.prestige_points, state.prestigePoints), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text(
            stringResource(
                R.string.session_stats,
                state.sessionStats.clicks,
                formatNum(state.sessionStats.debrisEarned),
                state.sessionStats.casesOpened
            ),
            color = Color.LightGray,
            fontSize = 12.sp
        )
        val collectionPercent = (((MetaProgressEngine.collectionMultiplier(state.fleetCounts, viewModel.fleetById) *
            MetaProgressEngine.masteryMultiplier(state.droneParts)) - 1.0) * 100).toInt()
        Text(stringResource(R.string.collection_bonus, collectionPercent), color = Color.LightGray)
        val canPrestige = EconomyBalance.canPrestige(state)
        val prestigeReward = if (canPrestige) EconomyBalance.prestigeReward(state) else 0
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = AppColors.Primary.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.22f))
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(stringResource(R.string.prestige_explained_title), color = AppColors.Primary, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.prestige_explained_body), color = Color.White.copy(alpha = .72f), fontSize = 11.sp, lineHeight = 15.sp)
                Text(
                    stringResource(R.string.prestige_preview, prestigeReward),
                    color = if (canPrestige) AppColors.Warning else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Button(onClick = viewModel::prestige, enabled = canPrestige) {
            Text(stringResource(if (canPrestige) R.string.prestige else R.string.prestige_requirement))
        }
        Technology.entries.forEach { technology ->
            val label = when (technology) {
                Technology.POWER_CORE -> R.string.technology_power_core
                Technology.OFFLINE_AI -> R.string.technology_offline_ai
                Technology.LUCK_MATRIX -> R.string.technology_luck_matrix
            }
            Button(
                onClick = { viewModel.buyTechnology(technology) },
                enabled = technology !in state.technologies && state.prestigePoints >= technology.cost,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${stringResource(label)} · ${stringResource(R.string.technology_cost, technology.cost)}")
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).background(AppColors.Warning.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("★", color = AppColors.Warning, fontSize = 17.sp) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(stringResource(R.string.achievements), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(stringResource(R.string.achievements_subtitle), color = Color.Gray, fontSize = 10.sp)
            }
        }
        AchievementEngine.definitions.forEach { achievement ->
            val unlocked = achievement.id in state.unlockedAchievementIds
            val claimed = achievement.id in state.claimedAchievementIds
            val rewardText = when {
                achievement.rewardPrestigePoints > 0 -> stringResource(
                    R.string.achievement_reward_points,
                    achievement.rewardPrestigePoints
                )
                else -> stringResource(
                    R.string.achievement_reward_debris,
                    formatNum(EconomyBalance.scaledReward(achievement.rewardDebris, state.currentPlanetId))
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(if (unlocked) AppColors.Warning.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.035f), RoundedCornerShape(14.dp))
                    .border(1.dp, if (unlocked) AppColors.Warning.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text((if (unlocked) "◆  " else "◇  ") + stringResource(achievementNameResource(achievement.id)), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(rewardText, color = AppColors.Secondary, fontSize = 10.sp)
                }
                Button(
                    onClick = { viewModel.claimAchievement(achievement.id) },
                    enabled = unlocked && !claimed,
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text(
                        stringResource(
                            when {
                                claimed -> R.string.achievement_claimed
                                unlocked -> R.string.achievement_claim
                                else -> R.string.achievement_locked
                            }
                        ),
                        fontSize = 10.sp
                    )
                }
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.event_log),
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = viewModel::clearEventLog, enabled = state.eventLog.isNotEmpty()) {
                Text(stringResource(R.string.event_log_clear))
            }
        }
        if (state.eventLog.isEmpty()) {
            Text(stringResource(R.string.event_log_empty), color = Color.Gray, fontSize = 11.sp)
        } else {
            state.eventLog.asReversed().forEach { entry ->
                val time = remember(entry.timestamp) {
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.timestamp))
                }
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row {
                        Text(
                            stringResource(eventLogNameResource(entry.eventType)),
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            fontSize = 11.sp
                        )
                        Text(time, color = Color.Gray, fontSize = 10.sp)
                    }
                    Text(
                        stringResource(eventLogOutcomeResource(entry.outcome)),
                        color = AppColors.Secondary,
                        fontSize = 10.sp
                    )
                    if (entry.reward > 0.0) {
                        Text(
                            stringResource(R.string.event_log_reward, formatNum(entry.reward)),
                            color = AppColors.Primary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

private fun eventLogNameResource(type: GameEventType): Int = when (type) {
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

private fun eventLogOutcomeResource(outcome: EventLogOutcome): Int = when (outcome) {
    EventLogOutcome.STARTED -> R.string.event_log_started
    EventLogOutcome.COMPLETED -> R.string.event_log_completed
    EventLogOutcome.EXPIRED -> R.string.event_log_expired
    EventLogOutcome.CHOICE -> R.string.event_log_choice
    EventLogOutcome.SUCCESS -> R.string.event_log_success
    EventLogOutcome.FAILURE -> R.string.event_log_failure
}

private fun achievementNameResource(id: String): Int = when (id) {
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

@Composable
fun ShopLauncherButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ui_button_shop_v3),
        contentDescription = stringResource(R.string.open_shop),
        modifier = modifier
            .size(60.dp)
            .clickable(onClick = onClick),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun MysteryCaseRow(viewModel: GameViewModel, state: GameState) {
    val totalDrones = state.fleetCounts.values.sum()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CaseType.entries.forEach { type -> CaseTypeRow(viewModel, state, type, totalDrones) }
    }
}

@Composable
private fun CaseTypeRow(viewModel: GameViewModel, state: GameState, type: CaseType, totalDrones: Int) {
    val caseCost = viewModel.calculateCaseCost(state.casesPurchased, type)
    val maxAffordable = viewModel.maxAffordableCases(state.totalDebris, state.casesPurchased, type)
    var showBundleDialog by remember { mutableStateOf(false) }
    var selectedCaseCount by remember { mutableIntStateOf(1) }
    val accent = when (type) {
        CaseType.COMMON -> AppColors.Primary
        CaseType.RARE -> Color(0xFF42A5F5)
        CaseType.LEGENDARY -> Color(0xFFFFB300)
    }
    val title = when (type) {
        CaseType.COMMON -> R.string.common_case
        CaseType.RARE -> R.string.rare_case
        CaseType.LEGENDARY -> R.string.legendary_case
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(AppColors.WhiteAlpha05, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = GameResourceRegistry.caseFrame(type, 1)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(title), color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.drones_owned_count, totalDrones), color = Color.Gray, fontSize = 11.sp)
                Text(stringResource(R.string.premium_drop_chance, type.premiumChance), color = accent, fontSize = 9.sp)
            }
        }
        Button(
            onClick = {
                if (maxAffordable >= 2) {
                    selectedCaseCount = 1
                    showBundleDialog = true
                }
                else viewModel.startOpeningCase(type)
            },
            enabled = state.totalDebris >= caseCost,
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp)
        ) {
            Text(formatNum(caseCost), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
    if (showBundleDialog) {
        SpaceDialog(
            title = stringResource(R.string.choose_case_amount),
            onDismiss = { showBundleDialog = false },
            content = {
                Text(stringResource(title), color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.case_bundle_hint), color = Color.LightGray, fontSize = 11.sp)
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("×$selectedCaseCount", color = accent, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text(
                        formatNum(viewModel.calculateCaseBundleCost(state.casesPurchased, type, selectedCaseCount)),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = selectedCaseCount.toFloat(),
                        onValueChange = { selectedCaseCount = it.toInt().coerceIn(1, maxAffordable) },
                        valueRange = 1f..maxAffordable.toFloat(),
                        modifier = Modifier.fillMaxWidth(0.82f).height(34.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                        )
                    )
                    Row(Modifier.fillMaxWidth(0.82f), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp)
                        Text(stringResource(R.string.max_cases_short, maxAffordable), color = accent, fontSize = 10.sp)
                    }
                }
            },
            actions = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = { showBundleDialog = false; viewModel.startOpeningCases(type, selectedCaseCount) },
                        modifier = Modifier.fillMaxWidth(0.72f).heightIn(min = 44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                    ) { Text(stringResource(R.string.buy_cases_count, selectedCaseCount), fontWeight = FontWeight.Bold) }
                }
            }
        )
    }
}

@Composable
private fun LateGalaxyPreview(viewModel: GameViewModel) {
    Column(
        Modifier.fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(AppColors.Primary.copy(alpha = .055f), RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.Primary.copy(alpha = .18f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(stringResource(R.string.late_galaxy_preview), color = AppColors.Primary, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.late_galaxy_preview_desc), color = Color.White.copy(alpha = .62f), fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            (21..24).forEach { index ->
                val id = "p$index"
                val planet = viewModel.planets[id] ?: return@forEach
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(planet.imageRes), null, Modifier.size(58.dp), contentScale = ContentScale.Fit)
                    Text(localizedPlanetName(id), color = Color.White, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun ShopRow(
    name: String, 
    meta: String, 
    cost: Long, 
    canBuy: Boolean, 
    canSell: Boolean,
    iconRes: Int, 
    spriteIndex: Int = -1, 
    showLock: Boolean = false,
    fleetActionLabel: String? = null,
    fleetActionEnabled: Boolean = true,
    onFleetAction: () -> Unit = {},
    onBuy: () -> Unit,
    onSell: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFF0B172C), RoundedCornerShape(14.dp))
            .border(1.dp, if (fleetActionLabel != null) AppColors.Primary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(46.dp).background(AppColors.Primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).border(1.dp, AppColors.Primary.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (spriteIndex >= 0) {
                    val columns = 6
                    val rows = 5
                    val row = spriteIndex / columns
                    val col = spriteIndex % columns
                    val iconSize = 34.dp
                    
                    Box(modifier = Modifier.size(iconSize)) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .requiredSize(iconSize * columns, iconSize * rows)
                                .offset(x = -iconSize * col, y = -iconSize * row)
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(34.dp)
                    )
                }
                if (showLock) {
                    Image(
                        painter = painterResource(R.drawable.ui_button_lock),
                        contentDescription = stringResource(R.string.locked),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (name.isNotEmpty()) {
                    Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(meta, color = Color.LightGray, fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
        if (fleetActionLabel != null || canSell || canBuy) Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            if (fleetActionLabel != null) {
                Button(
                    onClick = onFleetAction,
                    enabled = fleetActionEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary.copy(alpha = 0.18f), contentColor = AppColors.Primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.heightIn(min = 40.dp).weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(fleetActionLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (canSell) {
                Button(
                    onClick = onSell,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger.copy(alpha = 0.2f), contentColor = AppColors.Danger),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.heightIn(min = 40.dp).widthIn(min = 72.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(stringResource(R.string.sell), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (canBuy) {
                Button(
                    onClick = onBuy,
                    enabled = canBuy,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.heightIn(min = 40.dp).widthIn(min = 88.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(formatNum(cost.toDouble()), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun droneTraitDescription(id: String): String? = when (id) {
    "drone_5" -> stringResource(R.string.drone_trait_collector)
    "drone_9" -> stringResource(R.string.drone_trait_amplifier)
    "drone_13" -> stringResource(R.string.drone_trait_reactor)
    "drone_17" -> stringResource(R.string.drone_trait_lucky)
    "drone_21" -> stringResource(R.string.drone_trait_overclock)
    else -> null
}

@Composable
fun PlanetRow(
    name: String, 
    desc: String, 
    bonus: String,
    price: Long, 
    active: Boolean, 
    owned: Boolean,
    canBuy: Boolean, 
    iconRes: Int, 
    spriteIndex: Int = -1, 
    showLock: Boolean,
    onClick: () -> Unit
) {
    val isLocked = price < 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(if (active) AppColors.Primary.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconSize = 40.dp
            Box(modifier = Modifier.size(iconSize).clip(CircleShape)) {
                if (spriteIndex >= 0) {
                    val columns = 4
                    val rows = 3
                    val row = spriteIndex / columns
                    val col = spriteIndex % columns
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .requiredSize(iconSize * columns, iconSize * rows)
                            .offset(x = -iconSize * col, y = -iconSize * row)
                            .scale(1.8f) 
                    )
                } else {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .let { if(isLocked) it.alpha(0.3f) else it }
                    )
                }
                if (showLock) {
                    Image(
                        painter = painterResource(R.drawable.ui_button_lock),
                        contentDescription = stringResource(R.string.locked),
                        modifier = Modifier.align(Alignment.Center).size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if(isLocked) "???" else name, color = if(isLocked) Color.Gray else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = Color.Gray, fontSize = 11.sp)
                Text(bonus, color = AppColors.Primary, fontSize = 10.sp, lineHeight = 12.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onClick,
            enabled = (canBuy || owned) && !isLocked,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (active || isLocked) Color.Gray else AppColors.Primary, 
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.widthIn(min = 74.dp, max = 104.dp).heightIn(min = 40.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            val btnText = when {
                isLocked -> stringResource(R.string.locked)
                active -> stringResource(R.string.active)
                owned -> stringResource(R.string.select)
                price == 0L -> stringResource(R.string.free)
                else -> formatNum(price.toDouble())
            }
            Text(btnText, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun localizedPlanetName(id: String): String = stringResource(planetNameResource(id))

internal fun planetNameResource(id: String): Int =
    when (id) {
        "p1" -> R.string.planet_azurea
        "p2" -> R.string.planet_canyon_prime
        "p3" -> R.string.planet_nebula_echo
        "p4" -> R.string.planet_crystal_hearth
        "p5" -> R.string.planet_dune_horizon
        "p6" -> R.string.planet_volt_nova
        "p7" -> R.string.planet_gas_giant
        "p8" -> R.string.planet_jungle_core
        "p9" -> R.string.planet_magma_s15
        "p10" -> R.string.planet_red_dust
        "p11" -> R.string.planet_mech_world
        "p12" -> R.string.planet_luna_silvis
        "p13" -> R.string.planet_abyss_ocean
        "p14" -> R.string.planet_ring_oasis
        "p15" -> R.string.planet_sky_haven
        "p16" -> R.string.planet_toxic_waste
        "p17" -> R.string.planet_pink_nebula
        "p18" -> R.string.planet_cloud_city
        "p19" -> R.string.planet_rocky_bastion
        "p20" -> R.string.planet_foggy_void
        "p21" -> R.string.planet_chronos_rift
        "p22" -> R.string.planet_aurora_forge
        "p23" -> R.string.planet_prism_sanctuary
        "p24" -> R.string.planet_eventide_crown
        else -> R.string.unknown_item
    }

@Composable
internal fun localizedUpgradeName(id: String): String = stringResource(
    when (id) {
        "magnet" -> R.string.upgrade_plasma_magnet
        "torch" -> R.string.upgrade_weld_torch
        "wrench" -> R.string.upgrade_quantum_wrench
        "harvester" -> R.string.upgrade_debris_harvester
        "beacon" -> R.string.upgrade_signal_beacon
        "amplifier" -> R.string.upgrade_quantum_amplifier
        "matrix" -> R.string.upgrade_neural_matrix
        "compressor" -> R.string.upgrade_void_compressor
        "singularity" -> R.string.upgrade_singularity_tap
        else -> R.string.unknown_item
    }
)

@Composable
internal fun localizedPlanetDescription(id: String): String =
    stringResource(planetDescriptionResource(id))

internal fun planetDescriptionResource(id: String): Int =
    when (id) {
        "p1" -> R.string.planet_desc_azurea
        "p2" -> R.string.planet_desc_canyon_prime
        "p3" -> R.string.planet_desc_nebula_echo
        "p4" -> R.string.planet_desc_crystal_hearth
        "p5" -> R.string.planet_desc_dune_horizon
        "p6" -> R.string.planet_desc_volt_nova
        "p7" -> R.string.planet_desc_gas_giant
        "p8" -> R.string.planet_desc_jungle_core
        "p9" -> R.string.planet_desc_magma_s15
        "p10" -> R.string.planet_desc_red_dust
        "p11" -> R.string.planet_desc_mech_world
        "p12" -> R.string.planet_desc_luna_silvis
        "p13" -> R.string.planet_desc_abyss_ocean
        "p14" -> R.string.planet_desc_ring_oasis
        "p15" -> R.string.planet_desc_sky_haven
        "p16" -> R.string.planet_desc_toxic_waste
        "p17" -> R.string.planet_desc_pink_nebula
        "p18" -> R.string.planet_desc_cloud_city
        "p19" -> R.string.planet_desc_rocky_bastion
        "p20" -> R.string.planet_desc_foggy_void
        "p21" -> R.string.planet_desc_chronos_rift
        "p22" -> R.string.planet_desc_aurora_forge
        "p23" -> R.string.planet_desc_prism_sanctuary
        "p24" -> R.string.planet_desc_eventide_crown
        else -> R.string.unknown_item
    }

@Composable
internal fun localizedPlanetBonus(id: String): String = stringResource(planetBonusResource(id))

internal fun planetBonusResource(id: String): Int =
    when (id) {
        "p1" -> R.string.planet_bonus_azurea
        "p2" -> R.string.planet_bonus_canyon_prime
        "p3" -> R.string.planet_bonus_nebula_echo
        "p4" -> R.string.planet_bonus_crystal_hearth
        "p5" -> R.string.planet_bonus_dune_horizon
        "p6" -> R.string.planet_bonus_volt_nova
        "p7" -> R.string.planet_bonus_gas_giant
        "p8" -> R.string.planet_bonus_jungle_core
        "p9" -> R.string.planet_bonus_magma_s15
        "p10" -> R.string.planet_bonus_red_dust
        "p11" -> R.string.planet_bonus_mech_world
        "p12" -> R.string.planet_bonus_luna_silvis
        "p13" -> R.string.planet_bonus_abyss_ocean
        "p14" -> R.string.planet_bonus_ring_oasis
        "p15" -> R.string.planet_bonus_sky_haven
        "p16" -> R.string.planet_bonus_toxic_waste
        "p17" -> R.string.planet_bonus_pink_nebula
        "p18" -> R.string.planet_bonus_cloud_city
        "p19" -> R.string.planet_bonus_rocky_bastion
        "p20" -> R.string.planet_bonus_foggy_void
        "p21" -> R.string.planet_bonus_chronos_rift
        "p22" -> R.string.planet_bonus_aurora_forge
        "p23" -> R.string.planet_bonus_prism_sanctuary
        "p24" -> R.string.planet_bonus_eventide_crown
        else -> R.string.unknown_item
    }
