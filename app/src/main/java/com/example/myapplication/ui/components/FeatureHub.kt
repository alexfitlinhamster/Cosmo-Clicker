package com.example.myapplication.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.BossType
import com.example.myapplication.ChallengeId
import com.example.myapplication.FeatureEngine
import com.example.myapplication.GameState
import com.example.myapplication.GameViewModel
import com.example.myapplication.R
import com.example.myapplication.StationModule
import com.example.myapplication.WeeklyRule
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                    listOf(R.string.challenges, R.string.station).forEachIndexed { index, title ->
                        SpaceTab(stringResource(title), tab == index, { tab = index }, Modifier.weight(1f))
                    }
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    when (tab) {
                        0 -> ChallengePanel(state, viewModel)
                        else -> StationPanel(state, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengePanel(state: GameState, viewModel: GameViewModel) {
    val battle = state.titanBattle
    if (battle != null) {
        val config = FeatureEngine.challenge(battle.challengeId)
        val secondsLeft = ((battle.expiresAt - System.currentTimeMillis()).coerceAtLeast(0L) + 999L) / 1_000L
        val scope = rememberCoroutineScope()
        val entrance = remember(battle.expiresAt) { Animatable(0.55f) }
        val hitScale = remember(battle.expiresAt) { Animatable(1f) }
        var lastDamage by remember(battle.expiresAt) { mutableDoubleStateOf(0.0) }
        var damageEvent by remember(battle.expiresAt) { mutableIntStateOf(0) }
        var showDamage by remember(battle.expiresAt) { mutableStateOf(false) }

        LaunchedEffect(battle.expiresAt) {
            entrance.animateTo(1f, spring(dampingRatio = 0.58f, stiffness = 180f))
        }
        LaunchedEffect(damageEvent) {
            if (damageEvent > 0) {
                showDamage = true
                delay(420)
                showDamage = false
            }
        }

        val attackBoss: () -> Unit = {
            lastDamage = viewModel.onPlanetClick()
            damageEvent++
            scope.launch {
                hitScale.snapTo(0.92f)
                hitScale.animateTo(1f, tween(130, easing = FastOutSlowInEasing))
            }
            Unit
        }
        FeatureCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF030711)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painterResource(challengeArt(battle.challengeId)),
                    contentDescription = stringResource(challengeName(battle.challengeId)),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = entrance.value * hitScale.value
                            scaleY = entrance.value * hitScale.value
                            alpha = entrance.value.coerceIn(0f, 1f)
                        }
                        .clickable(onClick = attackBoss),
                    contentScale = ContentScale.Fit
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.12f))
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = showDamage,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { -it / 2 }
                ) {
                    Text(
                        text = "−${formatNum(lastDamage)}",
                        color = Color(0xFFFFE082),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    stringResource(R.string.tap_boss_to_attack),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = .68f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(stringResource(challengeName(battle.challengeId)), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.challenge_time_left, secondsLeft), color = if (secondsLeft <= 10) AppColors.Danger else AppColors.Secondary, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { (battle.health / battle.maxHealth).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(8.dp)),
                color = AppColors.Danger,
                trackColor = Color.White.copy(alpha = .08f)
            )
            Text(stringResource(R.string.boss_hp, formatNum(battle.health), formatNum(battle.maxHealth)), color = Color.White.copy(alpha = .72f), fontSize = 12.sp)
            ChallengeAbilityStatus(battle)
            Button(onClick = attackBoss, modifier = Modifier.fillMaxWidth().height(58.dp)) {
                Text(stringResource(R.string.attack_boss), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.challenge_fleet_attacks), color = Color.White.copy(alpha = .55f), fontSize = 11.sp)
            Text(stringResource(R.string.challenge_reward, formatNum(config.rewardDebris), config.rewardPrestige), color = AppColors.Secondary, fontSize = 12.sp)
        }
        return
    }

    Text(stringResource(R.string.challenges_intro), color = Color.White.copy(alpha = .7f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
    FeatureEngine.challenges.forEachIndexed { index, challenge ->
        val unlocked = FeatureEngine.isChallengeUnlocked(state, challenge.id)
        val completed = challenge.id in state.completedChallengeIds
        FeatureCard(Modifier.padding(bottom = 12.dp)) {
            Image(
                painterResource(challengeArt(challenge.id)),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(challengeName(challenge.id)), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.challenge_number, index + 1), color = AppColors.Secondary, fontSize = 11.sp)
                }
                Text(if (completed) "✓" else "★".repeat(index + 3), color = if (completed) AppColors.Primary else AppColors.Danger, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(challengeDescription(challenge.id)), color = Color.White.copy(alpha = .62f), fontSize = 12.sp)
            Text(stringResource(challengeTrait(challenge.id)), color = AppColors.Warning, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.challenge_stats, formatNum(challenge.health), challenge.durationMillis / 1_000L, formatNum(challenge.rewardDebris), challenge.rewardPrestige), color = AppColors.Secondary, fontSize = 11.sp)
            Button(
                onClick = { viewModel.startTitanBattle(challenge.id) },
                enabled = unlocked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(when {
                    !unlocked -> R.string.challenge_locked
                    completed -> R.string.challenge_again
                    else -> R.string.start_challenge
                }))
            }
        }
    }
}

private fun challengeArt(id: ChallengeId): Int = when (id) {
    ChallengeId.VOID_LEVIATHAN -> R.drawable.challenge_void_leviathan
    ChallengeId.SOLAR_DEVOURER -> R.drawable.challenge_solar_devourer
    ChallengeId.DREADNOUGHT_EMPRESS -> R.drawable.challenge_dreadnought_empress
    ChallengeId.NEBULA_DRAGON -> R.drawable.challenge_nebula_dragon
}

private fun challengeName(id: ChallengeId): Int = when (id) {
    ChallengeId.VOID_LEVIATHAN -> R.string.challenge_void_leviathan
    ChallengeId.SOLAR_DEVOURER -> R.string.challenge_solar_devourer
    ChallengeId.DREADNOUGHT_EMPRESS -> R.string.challenge_dreadnought_empress
    ChallengeId.NEBULA_DRAGON -> R.string.challenge_nebula_dragon
}

private fun challengeDescription(id: ChallengeId): Int = when (id) {
    ChallengeId.VOID_LEVIATHAN -> R.string.challenge_void_description
    ChallengeId.SOLAR_DEVOURER -> R.string.challenge_solar_description
    ChallengeId.DREADNOUGHT_EMPRESS -> R.string.challenge_dreadnought_description
    ChallengeId.NEBULA_DRAGON -> R.string.challenge_dragon_description
}

private fun challengeTrait(id: ChallengeId): Int = when (id) {
    ChallengeId.VOID_LEVIATHAN -> R.string.challenge_void_trait
    ChallengeId.SOLAR_DEVOURER -> R.string.challenge_solar_trait
    ChallengeId.DREADNOUGHT_EMPRESS -> R.string.challenge_dreadnought_trait
    ChallengeId.NEBULA_DRAGON -> R.string.challenge_dragon_trait
}

@Composable
private fun ChallengeAbilityStatus(battle: com.example.myapplication.TitanBattle) {
    when {
        battle.shieldCharges > 0 -> Text(
            stringResource(R.string.challenge_shield_status, battle.shieldCharges),
            color = AppColors.Warning,
            fontWeight = FontWeight.Bold
        )
        battle.minions > 0 -> Text(
            stringResource(R.string.challenge_minion_status, battle.minions),
            color = AppColors.Warning,
            fontWeight = FontWeight.Bold
        )
        battle.challengeId == ChallengeId.NEBULA_DRAGON -> Text(
            stringResource(R.string.challenge_dragon_regenerating),
            color = AppColors.Danger,
            fontWeight = FontWeight.Bold
        )
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
    Image(
        painter = painterResource(R.drawable.ui_button_command_center),
        contentDescription = stringResource(R.string.command_center),
        modifier = modifier
            .size(60.dp)
            .clickable(onClick = onClick),
        contentScale = ContentScale.Fit
    )
}
