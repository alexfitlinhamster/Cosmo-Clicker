package com.example.myapplication.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.BossType
import com.example.myapplication.FeatureEngine
import com.example.myapplication.GameState
import com.example.myapplication.GameViewModel
import com.example.myapplication.R
import com.example.myapplication.StationModule
import com.example.myapplication.WeeklyRule
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum

@Composable
fun FeatureHub(viewModel: GameViewModel, state: GameState, onClose: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF091426),
            border = BorderStroke(1.dp, AppColors.Primary.copy(alpha = .35f))
        ) {
            Column(Modifier.padding(16.dp)) {
                SpaceSheetHeader(stringResource(R.string.feature_hub_title), stringResource(R.string.feature_hub_subtitle), onClose)
                Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(R.string.weekly_galaxy, R.string.titans, R.string.station).forEachIndexed { index, title ->
                        SpaceTab(stringResource(title), tab == index, { tab = index }, Modifier.weight(1f))
                    }
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    when (tab) {
                        0 -> WeeklyGalaxyPanel(state, viewModel)
                        1 -> TitanPanel(state, viewModel)
                        else -> StationPanel(state, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyGalaxyPanel(state: GameState, viewModel: GameViewModel) {
    val galaxy = state.weeklyGalaxy
    val rule = when (galaxy.rule) {
        WeeklyRule.CLICKS_ONLY -> R.string.weekly_rule_clicks
        WeeklyRule.FRAGILE_DRONES -> R.string.weekly_rule_fragile
        WeeklyRule.VOLATILE_MARKET -> R.string.weekly_rule_market
    }
    FeatureCard {
        Text(stringResource(rule), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.weekly_rule_hint), color = Color.White.copy(alpha = .58f), fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        LinearProgressIndicator(
            progress = { (galaxy.progress / galaxy.target).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)),
            color = AppColors.Primary,
            trackColor = Color.White.copy(alpha = .08f)
        )
        Text("${formatNum(galaxy.progress)} / ${formatNum(galaxy.target)}", color = Color.White.copy(alpha = .72f), fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = if (galaxy.progress >= galaxy.target) viewModel::claimWeeklyGalaxyReward else viewModel::toggleWeeklyGalaxy,
            enabled = !galaxy.rewardClaimed,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(when {
            galaxy.rewardClaimed -> R.string.reward_claimed
            galaxy.progress >= galaxy.target -> R.string.claim_rare_reward
            galaxy.active -> R.string.leave_galaxy
            else -> R.string.enter_galaxy
        })) }
        Text(stringResource(R.string.weekly_reward), color = AppColors.Secondary, fontSize = 12.sp)
    }
}

@Composable
private fun TitanPanel(state: GameState, viewModel: GameViewModel) {
    val battle = state.titanBattle
    val type = battle?.type ?: BossType.entries[(FeatureEngine.weekKey() % BossType.entries.size).toInt()]
    val art = when (type) {
        BossType.ASTEROID_TITAN -> R.drawable.boss_asteroid_titan
        BossType.PIRATE_DREADNOUGHT -> R.drawable.boss_pirate_dreadnought
        BossType.MECHANICAL_COLOSSUS -> R.drawable.boss_mechanical_colossus
    }
    FeatureCard {
        Image(painterResource(art), null, Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(when (type) {
            BossType.ASTEROID_TITAN -> R.string.boss_asteroid
            BossType.PIRATE_DREADNOUGHT -> R.string.boss_pirate
            BossType.MECHANICAL_COLOSSUS -> R.string.boss_colossus
        }), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        if (battle != null) {
            LinearProgressIndicator(
                progress = { (battle.health / battle.maxHealth).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(8.dp)),
                color = AppColors.Danger,
                trackColor = Color.White.copy(alpha = .08f)
            )
            Text(stringResource(R.string.boss_hp, formatNum(battle.health), formatNum(battle.maxHealth)), color = Color.White.copy(alpha = .7f), fontSize = 12.sp)
            Text(stringResource(R.string.boss_fight_hint), color = AppColors.Secondary, fontSize = 12.sp)
        } else {
            Text(stringResource(R.string.titan_wins, state.titanWins), color = Color.White.copy(alpha = .58f), fontSize = 12.sp)
            Button(viewModel::startTitanBattle, Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_battle)) }
        }
    }
}

@Composable
private fun StationPanel(state: GameState, viewModel: GameViewModel) {
    StationModule.entries.forEach { module ->
        val level = state.stationLevels[module] ?: 0
        val name = when (module) {
            StationModule.HANGAR -> R.string.station_hangar
            StationModule.LABORATORY -> R.string.station_lab
            StationModule.REACTOR -> R.string.station_reactor
            StationModule.TRADE_HUB -> R.string.station_trade
        }
        FeatureCard(Modifier.padding(bottom = 9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(name), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.station_level, level, 5), color = Color.White.copy(alpha = .5f), fontSize = 11.sp)
                    Text(stringResource(when (module) {
                        StationModule.HANGAR -> R.string.station_hangar_bonus
                        StationModule.LABORATORY -> R.string.station_lab_bonus
                        StationModule.REACTOR -> R.string.station_reactor_bonus
                        StationModule.TRADE_HUB -> R.string.station_trade_bonus
                    }), color = AppColors.Secondary, fontSize = 11.sp)
                }
                Button(onClick = { viewModel.upgradeStation(module) }, enabled = level < 5 && state.totalDebris >= FeatureEngine.stationCost(module, level)) {
                    Text(if (level >= 5) stringResource(R.string.station_max) else formatNum(FeatureEngine.stationCost(module, level)))
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().background(Color.White.copy(alpha = .045f), RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        content = content
    )
}

@Composable
fun CommandCenterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(48.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp), color = Color(0xDD10243D),
        border = BorderStroke(1.dp, AppColors.Primary.copy(alpha = .45f))
    ) { Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.command_center_short), color = Color.White, fontWeight = FontWeight.SemiBold)
    } }
}
