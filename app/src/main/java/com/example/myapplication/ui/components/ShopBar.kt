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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.GameState
import com.example.myapplication.CaseType
import com.example.myapplication.GameResourceRegistry
import com.example.myapplication.GameViewModel
import com.example.myapplication.EconomyBalance
import com.example.myapplication.AchievementEngine
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
    var selectedTab by remember { mutableIntStateOf(0) }
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
                                    canBuy = state.totalDebris >= config.price && !active &&
                                        EconomyBalance.planetFleetObjectiveMet(state, EconomyBalance.planetIndex(id)),
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
                                DroneCollectionSets(state)
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
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Danger),
                                    style = CosmicButtonStyle.Danger
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
                    enabled = discovered >= nextMilestone,
                    style = CosmicButtonStyle.Reward
                ) { Text(stringResource(R.string.collection_claim), fontSize = 10.sp) }
            }
        }
    }
}

@Composable
private fun MetaProgressPanel(viewModel: GameViewModel, state: GameState) {
    var showPrestigeConfirmation by remember { mutableStateOf(false) }
    val canPrestige = EconomyBalance.canPrestige(state)
    val prestigeReward = if (canPrestige) EconomyBalance.prestigeReward(state) else 0
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(30.dp).background(AppColors.Warning.copy(alpha = .14f), CircleShape)
                    .border(1.dp, AppColors.Warning.copy(alpha = .4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_prestige_hologram_v2),
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
        Button(onClick = { showPrestigeConfirmation = true }, enabled = canPrestige) {
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
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    style = CosmicButtonStyle.Reward
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
            Button(onClick = viewModel::clearEventLog, enabled = state.eventLog.isNotEmpty(), style = CosmicButtonStyle.Secondary) {
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
    if (showPrestigeConfirmation) {
        SpaceDialog(
            title = stringResource(R.string.prestige_confirm_title),
            onDismiss = { showPrestigeConfirmation = false },
            content = {
                Text(
                    stringResource(R.string.prestige_confirm_message, prestigeReward),
                    color = Color.White.copy(alpha = .86f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            actions = {
                Button(onClick = { showPrestigeConfirmation = false }, style = CosmicButtonStyle.Secondary) {
                    Text(stringResource(R.string.cancel))
                }
                Button(onClick = {
                    showPrestigeConfirmation = false
                    viewModel.prestige()
                }) { Text(stringResource(R.string.prestige_confirm)) }
            }
        )
    }
}

@Composable
fun ShopLauncherButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.size(56.dp).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = Color(0xFF0B1728), border = BorderStroke(1.dp, AppColors.Primary.copy(.42f))) {
        MinimalShopIcon(1, AppColors.Primary, Modifier.padding(10.dp))
    }
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
            .background(Brush.horizontalGradient(listOf(accent.copy(.12f), Color(0xFF0A1424))), RoundedCornerShape(14.dp))
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
                GeneratedSheetIcon(
                    drawable = R.drawable.shop_cases_minimal_sheet_v1,
                    index = when (type) { CaseType.COMMON -> 0; CaseType.RARE -> 3; CaseType.LEGENDARY -> 6 },
                    size = 44.dp,
                    modifier = Modifier.clip(RoundedCornerShape(7.dp))
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(title), color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.drones_owned_count, totalDrones), color = Color.Gray, fontSize = 11.sp)
                Text(stringResource(R.string.premium_drop_chance, type.premiumChance), color = accent, fontSize = 9.sp)
            }
        }
        val caseEnabled = state.totalDebris >= caseCost
        Box(
            modifier = Modifier.fillMaxWidth().height(46.dp)
                .alpha(if (caseEnabled) 1f else .38f)
                .clickable(enabled = caseEnabled) {
                if (maxAffordable >= 2) {
                    selectedCaseCount = 1
                    showBundleDialog = true
                }
                else viewModel.startOpeningCase(type)
            },
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(R.drawable.ui_shop_case_button_v3), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Row(verticalAlignment = Alignment.CenterVertically) {
                GeneratedSheetIcon(R.drawable.shop_ui_minimal_sheet_v1, 4, 17.dp, columns = 4, rows = 4)
                Spacer(Modifier.width(5.dp))
                Text(formatNum(caseCost), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
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
                    Text("×$selectedCaseCount", color = accent, fontSize = 30.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    Text(
                        formatNum(viewModel.calculateCaseBundleCost(state.casesPurchased, type, selectedCaseCount)),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
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
                    Box(
                        modifier = Modifier.fillMaxWidth(0.72f).height(46.dp).clickable {
                            showBundleDialog = false
                            viewModel.startOpeningCases(type, selectedCaseCount)
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(painterResource(R.drawable.ui_shop_case_button_v3), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                        Text(stringResource(R.string.buy_cases_count, selectedCaseCount), color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        )
    }
}

@Composable
private fun DroneCollectionSets(state: GameState) {
    val totalBonus = ((MetaProgressEngine.collectionSetMultiplier(state.discoveredDroneIds) - 1.0) * 100).toInt()
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.collection_sets_title), color = Color.White, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.collection_sets_total_bonus, totalBonus), color = AppColors.Secondary, fontSize = 12.sp)
        }
        MetaProgressEngine.collectionSets.forEach { set ->
            val discovered = set.droneIds.count { it in state.discoveredDroneIds }
            val complete = discovered == set.droneIds.size
            val name = stringResource(when (set.id) {
                "first_expedition" -> R.string.collection_set_first_expedition
                "emerald_squadron" -> R.string.collection_set_emerald_squadron
                "crimson_corps" -> R.string.collection_set_crimson_corps
                "cyber_swarm" -> R.string.collection_set_cyber_swarm
                "stellar_guard" -> R.string.collection_set_stellar_guard
                else -> R.string.collection_set_quantum_edge
            })
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (complete) AppColors.Primary.copy(alpha = .11f) else Color.White.copy(alpha = .035f),
                border = BorderStroke(1.dp, if (complete) AppColors.Primary.copy(alpha = .42f) else Color.White.copy(alpha = .08f))
            ) {
                Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("$discovered/${set.droneIds.size}", color = if (complete) AppColors.Primary else Color.Gray, fontSize = 11.sp)
                    }
                    LinearProgressIndicator(
                        progress = { discovered.toFloat() / set.droneIds.size },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = AppColors.Primary,
                        trackColor = Color.White.copy(alpha = .10f)
                    )
                    Text(
                        stringResource(if (complete) R.string.collection_set_bonus_active else R.string.collection_set_bonus_locked, set.bonusPercent),
                        color = if (complete) AppColors.Secondary else Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
        }
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
                    MinimalShopIcon(iconRes, AppColors.Primary, Modifier.size(34.dp))
                }
                if (showLock) {
                    GeneratedSheetIcon(R.drawable.shop_ui_minimal_sheet_v1, 6, 28.dp, columns = 4, rows = 4)
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
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    style = CosmicButtonStyle.Danger
                ) {
                    Text(stringResource(R.string.sell), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (canBuy) {
                Box(
                    modifier = Modifier.width(116.dp).height(42.dp).clickable(onClick = onBuy),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painterResource(R.drawable.ui_shop_upgrade_button_v3), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    Text(formatNum(cost.toDouble()), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
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
    val accent = when {
        active -> AppColors.Primary
        owned -> AppColors.Secondary
        canBuy -> AppColors.Warning
        else -> AppColors.Outline
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface.copy(alpha = .9f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = if (active) .52f else .25f))
    ) {
        Column(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(accent.copy(alpha = .10f), Color.Transparent)))
                .padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
            val iconSize = 68.dp
            Surface(
                modifier = Modifier.size(iconSize),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF071525),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .30f))
            ) {
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))) {
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
                            .scale(1.62f)
                    )
                } else {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .let { if(isLocked) it.alpha(0.3f) else it }
                    )
                }
                if (showLock) {
                    Image(
                        painter = painterResource(R.drawable.ui_lock_control_v2),
                        contentDescription = stringResource(R.string.locked),
                        modifier = Modifier.align(Alignment.Center).size(30.dp)
                    )
                }
            }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if(isLocked) "???" else name, modifier = Modifier.weight(1f), color = if(isLocked) Color.Gray else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Surface(shape = RoundedCornerShape(8.dp), color = accent.copy(alpha = .13f)) {
                        Text(
                            text = when { active -> stringResource(R.string.active); owned -> stringResource(R.string.owned); else -> stringResource(R.string.locked) },
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            color = accent,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(desc, color = AppColors.TextMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text(bonus, color = accent, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (!owned && !isLocked) {
                Icon(painterResource(R.drawable.ic_currency_debris_v2), null, Modifier.size(20.dp), tint = Color.Unspecified)
                Spacer(Modifier.width(6.dp))
                Text(formatNum(price.toDouble()), color = AppColors.Warning, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            } else {
                Text(
                    if (active) stringResource(R.string.active) else stringResource(R.string.owned),
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        val planetEnabled = (canBuy || owned) && !isLocked
        Box(
            modifier = Modifier.width(116.dp).height(42.dp)
                .alpha(if (planetEnabled) 1f else .35f)
                .clickable(enabled = planetEnabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(R.drawable.ui_shop_planet_button_v3), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            val btnText = when {
                isLocked -> stringResource(R.string.locked)
                active -> stringResource(R.string.active)
                owned -> stringResource(R.string.select)
                price == 0L -> stringResource(R.string.free)
                else -> stringResource(R.string.buy)
            }
            Text(btnText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        }
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
        "p25" -> R.string.planet_emberglass
        "p26" -> R.string.planet_verdant_halo
        "p27" -> R.string.planet_iron_tempest
        "p28" -> R.string.planet_frozen_reliquary
        "p29" -> R.string.planet_celestial_bloom
        "p30" -> R.string.planet_binary_grave
        "p31" -> R.string.planet_mirage_engine
        "p32" -> R.string.planet_leviathan_deep
        "p33" -> R.string.planet_solar_archive
        "p34" -> R.string.planet_shattered_meridian
        "p35" -> R.string.planet_clockwork_eden
        "p36" -> R.string.planet_phantom_orchard
        "p37" -> R.string.planet_cinder_cathedral
        "p38" -> R.string.planet_null_beacon
        "p39" -> R.string.planet_origin_vault
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
        "p25" -> R.string.planet_desc_emberglass
        "p26" -> R.string.planet_desc_verdant_halo
        "p27" -> R.string.planet_desc_iron_tempest
        "p28" -> R.string.planet_desc_frozen_reliquary
        "p29" -> R.string.planet_desc_celestial_bloom
        "p30" -> R.string.planet_desc_binary_grave
        "p31" -> R.string.planet_desc_mirage_engine
        "p32" -> R.string.planet_desc_leviathan_deep
        "p33" -> R.string.planet_desc_solar_archive
        "p34" -> R.string.planet_desc_shattered_meridian
        "p35" -> R.string.planet_desc_clockwork_eden
        "p36" -> R.string.planet_desc_phantom_orchard
        "p37" -> R.string.planet_desc_cinder_cathedral
        "p38" -> R.string.planet_desc_null_beacon
        "p39" -> R.string.planet_desc_origin_vault
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
        "p25" -> R.string.planet_bonus_emberglass
        "p26" -> R.string.planet_bonus_verdant_halo
        "p27" -> R.string.planet_bonus_iron_tempest
        "p28" -> R.string.planet_bonus_frozen_reliquary
        "p29" -> R.string.planet_bonus_celestial_bloom
        "p30" -> R.string.planet_bonus_binary_grave
        "p31" -> R.string.planet_bonus_mirage_engine
        "p32" -> R.string.planet_bonus_leviathan_deep
        "p33" -> R.string.planet_bonus_solar_archive
        "p34" -> R.string.planet_bonus_shattered_meridian
        "p35" -> R.string.planet_bonus_clockwork_eden
        "p36" -> R.string.planet_bonus_phantom_orchard
        "p37" -> R.string.planet_bonus_cinder_cathedral
        "p38" -> R.string.planet_bonus_null_beacon
        "p39" -> R.string.planet_bonus_origin_vault
        else -> R.string.unknown_item
    }
