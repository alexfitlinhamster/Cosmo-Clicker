package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
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
import com.example.myapplication.Technology
import com.example.myapplication.Rarity
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum

@Composable
fun ShopBar(viewModel: GameViewModel, state: GameState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabColors = listOf(Color(0xFF48DFFC), Color(0xFFAA77FF), Color(0xFFFFC857), Color(0xFF50D890))
    val accent = tabColors[selectedTab]
    Card(
        modifier = modifier.widthIn(max = 720.dp).fillMaxWidth().fillMaxHeight(0.80f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF07111F)),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.32f))
    ) {
        Column(
            Modifier
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = .12f), Color(0xFF07111F), Color(0xFF090D1A))))
                .padding(18.dp)
        ) {
            SpaceSheetHeader(stringResource(R.string.case_shop_title), stringResource(R.string.case_shop_subtitle), onClose)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(R.string.shop_tab_upgrades, R.string.shop_tab_cases, R.string.shop_tab_planets, R.string.shop_tab_systems).forEachIndexed { index, title ->
                    SpaceTab(stringResource(title), selectedTab == index, { selectedTab = index }, Modifier.weight(1f), accent = tabColors[index], iconSheetIndex = index)
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                when (selectedTab) {
                    0 -> items(viewModel.clickItems, key = { it.id }) { upgrade ->
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
                            iconIndex = viewModel.clickItems.indexOf(upgrade),
                            sheetDrawable = R.drawable.shop_upgrades_minimal_sheet_v1,
                            accent = tabColors[0],
                            onBuy = { viewModel.buyClickUpgrade(upgrade.id) }
                        )
                    }
                    1 -> item { MysteryCaseRow(viewModel, state) }
                    2 -> items(viewModel.planets.toList(), key = { it.first }) { (id, planet) ->
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
                    else -> items(listOf("autoclick", "flight", "spawn", "magnet")) { id ->
                        val level = viewModel.utilityUpgradeLevel(id)
                        val max = viewModel.utilityUpgradeMaxLevel(id)
                        val cost = viewModel.utilityUpgradeCost(id, level)
                        ClickUpgradeRow(
                            name = stringResource(when (id) { "autoclick" -> R.string.upgrade_autoclicker; "flight" -> R.string.upgrade_flight_slots; "spawn" -> R.string.upgrade_spawn_speed; else -> R.string.upgrade_magnet_radius }),
                            meta = if (id == "autoclick") stringResource(R.string.autoclicker_rate, level, max) else stringResource(R.string.utility_level, level, max),
                            cost = cost.toLong(),
                            enabled = level < max && state.totalDebris >= cost,
                            iconRes = when (id) {
                                "autoclick" -> R.drawable.upgrade_neural_matrix_v2
                                "flight" -> R.drawable.upgrade_flight_slots_v2
                                "spawn" -> R.drawable.upgrade_spawn_speed_v2
                                else -> R.drawable.upgrade_magnet_v2
                            },
                            iconIndex = when (id) { "autoclick" -> 8; "flight" -> 9; "spawn" -> 10; else -> 11 },
                            sheetDrawable = R.drawable.shop_ui_minimal_sheet_v1,
                            sheetColumns = 4,
                            sheetRows = 4,
                            accent = tabColors[3],
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
    iconIndex: Int? = null,
    sheetDrawable: Int = R.drawable.shop_upgrades_minimal_sheet_v1,
    sheetColumns: Int = 3,
    sheetRows: Int = 3,
    accent: Color = AppColors.Primary,
    onBuy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(accent.copy(.095f), Color(0xFF0A1424))), RoundedCornerShape(14.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(11.dp),
            color = Color(0xFF091526),
            border = null
        ) {
            if (iconIndex != null) GeneratedSheetIcon(sheetDrawable, iconIndex, 42.dp, Modifier.clip(RoundedCornerShape(10.dp)), sheetColumns, sheetRows)
            else Image(painterResource(iconRes), null, Modifier.padding(5.dp), contentScale = ContentScale.Fit)
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
        Box(
            modifier = Modifier.width(116.dp).height(42.dp)
                .alpha(if (enabled) 1f else .38f)
                .clickable(enabled = enabled, onClick = onBuy),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(R.drawable.ui_shop_upgrade_button_v3), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Row(verticalAlignment = Alignment.CenterVertically) {
                GeneratedSheetIcon(R.drawable.shop_ui_minimal_sheet_v1, 4, 16.dp, columns = 4, rows = 4)
                Spacer(Modifier.width(4.dp))
                Text(formatNum(cost.toDouble()), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun DroneHangarPanel(viewModel: GameViewModel, state: GameState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    SpacePanel(
        stringResource(R.string.drone_hangar_title),
        stringResource(R.string.drone_hangar_subtitle),
        onClose,
        modifier,
        backgroundRes = R.drawable.bg_hangar_minimal_v1
    ) {
        item {
            val active = state.activeFleetCounts.values.sum()
            val owned = state.fleetCounts.values.sum()
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Surface.copy(alpha = .90f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.24f))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.hangar_overview), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    HangarCapacityLine(stringResource(R.string.drones_in_flight), active, viewModel.activeDroneCapacity(state), AppColors.Primary)
                    Text(stringResource(R.string.drones_in_storage_count, owned), color = AppColors.Secondary, fontSize = 11.sp)
                }
            }
        }
        items(viewModel.fleetItems.chunked(2), key = { row -> row.first().id }) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { drone ->
                    CompactHangarDroneCard(drone, viewModel, state, Modifier.weight(1f))
                }
                repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
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
    val hoverFrames = if (drone.rarity == Rarity.LEGENDARY) 16 else 8
    val hoverMotion = rememberInfiniteTransition(label = "hangar_hover_${drone.id}")
    val hoverPhase by hoverMotion.animateFloat(
        initialValue = 0f,
        targetValue = hoverFrames.toFloat(),
        animationSpec = infiniteRepeatable(tween(1_600, easing = LinearEasing)),
        label = "hangar_hover_phase"
    )
    val frame = hoverPhase.toInt().coerceIn(0, hoverFrames - 1)
    val half = hoverFrames / 2
    val hoverOffset = if (frame <= half) -frame else -(hoverFrames - frame)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (active > 0) AppColors.Primary.copy(alpha = .15f) else AppColors.Surface.copy(alpha = .88f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (active > 0) AppColors.Primary.copy(alpha = .38f) else AppColors.Outline.copy(alpha = .7f))
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(74.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(drone.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(68.dp).offset(y = (hoverOffset * 0.55f).dp),
                    contentScale = ContentScale.Fit,
                    alpha = if (discovered) 1f else .18f
                )
                if (!discovered) Icon(
                    painter = painterResource(R.drawable.ui_lock_control_v2),
                    contentDescription = stringResource(R.string.locked),
                    tint = Color.White.copy(alpha = .85f),
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(if (discovered) drone.name else stringResource(R.string.locked), color = if (discovered) Color.White else AppColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$active / $count", color = if (active > 0) AppColors.Primary else AppColors.TextMuted, fontSize = 11.sp)
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CosmicHangarAction(
                    text = stringResource(if (active > 0) R.string.send_to_storage else R.string.send_to_flight),
                    iconRes = if (active > 0) R.drawable.ic_action_recall else R.drawable.ic_action_launch,
                    enabled = canDeploy,
                    primary = true,
                    onClick = { if (active > 0) viewModel.recallDrone(drone.id) else viewModel.deployDrone(drone.id) }
                )
                CosmicHangarAction(
                    text = stringResource(R.string.sell),
                    iconRes = R.drawable.ic_action_sell,
                    enabled = count > 0,
                    primary = false,
                    onClick = { viewModel.sellFleet(drone.id) }
                )
            }
        }
    }
}

@Composable
private fun CosmicHangarAction(
    text: String,
    iconRes: Int,
    enabled: Boolean,
    primary: Boolean,
    onClick: () -> Unit
) {
    val accent = if (primary) AppColors.Primary else AppColors.Danger
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (primary) 40.dp else 34.dp)
            .alpha(if (enabled) 1f else .34f)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (primary) accent.copy(alpha = .20f) else AppColors.SurfaceRaised.copy(alpha = .72f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accent.copy(alpha = if (primary) .58f else .38f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(if (primary) 18.dp else 16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                color = accent,
                fontSize = if (primary) 10.sp else 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
    val totalAchievements = AchievementEngine.definitions.size
    val claimedCount = state.claimedAchievementIds.size
    val completion = claimedCount.toFloat() / totalAchievements.coerceAtLeast(1)
    Card(
        modifier = modifier.widthIn(max = 720.dp).fillMaxWidth().fillMaxHeight(0.76f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline.copy(alpha = .65f))
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(painterResource(R.drawable.bg_achievements_archive_v1), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .58f)
            Box(Modifier.fillMaxSize().background(Color(0xA608101C)))
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            SpaceSheetHeader(stringResource(R.string.achievements), stringResource(R.string.achievements_subtitle), onClose)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(AppColors.Warning.copy(alpha = .18f), AppColors.Primary.copy(alpha = .08f))), RoundedCornerShape(16.dp))
                    .border(1.dp, AppColors.Warning.copy(alpha = .25f), RoundedCornerShape(16.dp))
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_achievement_medal), null, Modifier.size(38.dp), tint = Color.Unspecified)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.achievements), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("$claimedCount / $totalAchievements", color = AppColors.Warning, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    LinearProgressIndicator(
                        progress = { completion },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = AppColors.Warning,
                        trackColor = Color.White.copy(alpha = .10f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
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
                    .background(
                        when { claimed -> Color.White.copy(alpha = .035f); unlocked -> AppColors.Warning.copy(alpha = .10f); else -> AppColors.Surface.copy(alpha = .82f) },
                        RoundedCornerShape(15.dp)
                    )
                    .border(1.dp, if (unlocked && !claimed) AppColors.Warning.copy(alpha = .42f) else AppColors.Outline.copy(alpha = .34f), RoundedCornerShape(15.dp))
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_achievement_medal),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(30.dp).alpha(if (claimed) .42f else 1f)
                )
                Spacer(Modifier.width(9.dp))
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        stringResource(achievementTitle(achievement.id)),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (achievement.rewardPrestigePoints == 0) {
                            Icon(painterResource(R.drawable.ic_currency_debris_v2), null, Modifier.size(14.dp), tint = Color.Unspecified)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(reward, color = if (unlocked && !claimed) AppColors.Warning else AppColors.TextMuted, fontSize = 9.sp)
                    }
                    Button(
                        onClick = { viewModel.claimAchievement(achievement.id) },
                        enabled = unlocked && !claimed,
                        modifier = Modifier.fillMaxWidth().height(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Warning.copy(alpha = .18f), contentColor = AppColors.Warning),
                        style = CosmicButtonStyle.Reward
                    ) {
                        Text(
                            stringResource(if (claimed) R.string.achievement_claimed else if (unlocked) R.string.achievement_claim else R.string.achievement_locked),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
fun PrestigeShopPanel(viewModel: GameViewModel, state: GameState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    SpacePanel(
        stringResource(R.string.prestige_shop),
        stringResource(R.string.prestige_shop_subtitle),
        onClose,
        modifier,
        backgroundRes = R.drawable.bg_prestige_shop_minimal_v2
    ) {
        item {
            Surface(modifier = Modifier.fillMaxWidth().height(68.dp), shape = RoundedCornerShape(16.dp), color = Color(0xCC091426), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8A77FF).copy(.28f))) {
                Box(Modifier.fillMaxSize()) {
                    Image(painterResource(R.drawable.ic_prestige_hologram_v2), null, Modifier.align(Alignment.CenterEnd).offset(x = 12.dp).size(94.dp).alpha(.10f), contentScale = ContentScale.Fit)
                    Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_prestige_hologram_v2), null, Modifier.size(24.dp), tint = Color.Unspecified)
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text(stringResource(R.string.prestige_balance), color = AppColors.TextMuted, fontSize = 9.sp)
                            Text("${state.prestigePoints}", color = Color(0xFFB8A7FF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        items(Technology.entries, key = { it.name }) { technology ->
            val owned = technology in state.technologies
            val name = when (technology) {
                Technology.POWER_CORE -> R.string.technology_power_core
                Technology.OFFLINE_AI -> R.string.technology_offline_ai
                Technology.LUCK_MATRIX -> R.string.technology_luck_matrix
            }
            val description = when (technology) {
                Technology.POWER_CORE -> R.string.technology_power_core_desc
                Technology.OFFLINE_AI -> R.string.technology_offline_ai_desc
                Technology.LUCK_MATRIX -> R.string.technology_luck_matrix_desc
            }
            val icon = when (technology) {
                Technology.POWER_CORE -> R.drawable.ic_product_power_core
                Technology.OFFLINE_AI -> R.drawable.ic_product_offline_ai
                Technology.LUCK_MATRIX -> R.drawable.ic_product_luck_matrix
            }
            val accent = when (technology) {
                Technology.POWER_CORE -> Color(0xFFFF9D3D)
                Technology.OFFLINE_AI -> Color(0xFF55D9FF)
                Technology.LUCK_MATRIX -> Color(0xFFB987FF)
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xD90A1425),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = if (owned) .12f else .32f))
            ) {
                Column(Modifier.background(Brush.horizontalGradient(listOf(accent.copy(.09f), Color.Transparent))).padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(12.dp), color = accent.copy(alpha = .11f), border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(.22f))) {
                            Image(painterResource(icon), null, Modifier.padding(8.dp), contentScale = ContentScale.Fit, alpha = if (owned) .42f else 1f)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(name), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp)
                            Text(stringResource(description), color = AppColors.TextMuted, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_nav_prestige_minimal), null, Modifier.size(18.dp), tint = Color.Unspecified)
                            Spacer(Modifier.width(6.dp))
                            Text("${technology.cost}", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        val canBuy = !owned && state.prestigePoints >= technology.cost
                        Surface(
                            modifier = Modifier.height(34.dp).widthIn(min = 82.dp)
                                .alpha(if (owned || canBuy) 1f else .38f)
                                .clickable(enabled = canBuy) { viewModel.buyTechnology(technology) },
                            shape = RoundedCornerShape(11.dp),
                            color = Color.Transparent,
                            border = null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (!owned) Image(painterResource(R.drawable.ui_action_frame_v2), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, alpha = if (canBuy) .72f else .30f)
                                Text(stringResource(if (owned) R.string.prestige_owned else R.string.prestige_buy), color = if (owned) AppColors.TextMuted else accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
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
            .background(AppColors.Surface.copy(alpha = .82f), RoundedCornerShape(13.dp))
            .border(1.dp, AppColors.Outline.copy(alpha = .42f), RoundedCornerShape(13.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_nav_stats_minimal), null, Modifier.size(22.dp), tint = Color.Unspecified)
        Spacer(Modifier.width(9.dp))
        Text(label, modifier = Modifier.weight(1f), color = Color.White.copy(alpha = .88f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(12.dp))
        Text(value, color = AppColors.Primary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun SpacePanel(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    modifier: Modifier,
    backgroundRes: Int? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Card(
        modifier = modifier.widthIn(max = 720.dp).fillMaxWidth().fillMaxHeight(0.80f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Box(Modifier.fillMaxSize()) {
            if (backgroundRes != null) {
                Image(
                    painter = painterResource(backgroundRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = .72f
                )
                Box(Modifier.fillMaxSize().background(Color(0xB807101D)))
            }
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                SpaceSheetHeader(title, subtitle, onClose)
                Spacer(Modifier.height(14.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), content = content)
            }
        }
    }
}

@Composable fun HangarLauncherButton(onClick: () -> Unit) = LauncherIcon(R.drawable.ic_nav_hangar_minimal, R.string.open_hangar, onClick)
@Composable fun AchievementsLauncherButton(onClick: () -> Unit) = LauncherIcon(R.drawable.ic_achievement_medal, R.string.open_achievements, onClick)

@Composable
private fun LauncherIcon(icon: Int, description: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(48.dp).clickable(onClick = onClick),
        shape = CircleShape,
        color = AppColors.SurfaceRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(alpha = .24f))
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
            modifier = Modifier.padding(11.dp),
            tint = Color.Unspecified
        )
    }
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
