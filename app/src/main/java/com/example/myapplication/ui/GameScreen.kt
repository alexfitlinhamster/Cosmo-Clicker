package com.example.myapplication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.myapplication.FloatingTextData
import com.example.myapplication.ChallengeId
import com.example.myapplication.GameEvent
import com.example.myapplication.GameEventType
import com.example.myapplication.GameViewModel
import com.example.myapplication.DistressChoice
import com.example.myapplication.StationChoice
import com.example.myapplication.TradeOffer
import com.example.myapplication.R
import com.example.myapplication.SoundManager
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameScreen(
    selectedLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    reduceMotion: Boolean,
    onReduceMotionChanged: (Boolean) -> Unit,
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.gameState.collectAsState()
    val combo by viewModel.combo.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val soundManager = remember(context) { SoundManager(context) }
    val floatingTextId = remember { AtomicLong(0L) }
    var floatingTexts by remember { mutableStateOf(listOf<FloatingTextData>()) }
    var isShopOpen by remember { mutableStateOf(false) }
    var isHangarOpen by remember { mutableStateOf(false) }
    var isAchievementsOpen by remember { mutableStateOf(false) }
    var isQuestOpen by remember { mutableStateOf(false) }
    var isFeatureHubOpen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showEventInfo by remember { mutableStateOf<GameEvent?>(null) }
    var activeBattleId by remember { mutableStateOf<ChallengeId?>(null) }
    var winsAtBattleStart by remember { mutableIntStateOf(0) }
    var battleResultWon by remember { mutableStateOf<Boolean?>(null) }

    // Состояние стартового экрана
    var showStartScreen by remember { mutableStateOf(true) }
    val startScreenOffset = remember { Animatable(0f) }
    val startScreenAlpha = remember { Animatable(1f) }

    BackHandler(
        enabled = showSettings ||
            showEventInfo != null ||
            state.eventChainResult != null ||
            state.lastOfflineReward > 0.0 ||
            isShopOpen ||
            isHangarOpen ||
            isAchievementsOpen ||
            isQuestOpen
            || isFeatureHubOpen
    ) {
        when {
            showSettings -> showSettings = false
            showEventInfo != null -> showEventInfo = null
            state.eventChainResult != null -> viewModel.clearEventChainResult()
            state.lastOfflineReward > 0.0 -> viewModel.clearOfflineReward()
            isShopOpen -> isShopOpen = false
            isHangarOpen -> isHangarOpen = false
            isAchievementsOpen -> isAchievementsOpen = false
            isQuestOpen -> isQuestOpen = false
            isFeatureHubOpen -> isFeatureHubOpen = false
        }
    }

    DisposableEffect(soundManager) {
        onDispose { soundManager.close() }
    }

    LaunchedEffect(state.activeEvent?.startedAt) {
        if (state.activeEvent != null) soundManager.playEventStart()
    }

    LaunchedEffect(state.eventChainResult) {
        state.eventChainResult?.let {
            if (it.success) soundManager.playEventSuccess() else soundManager.playEventFailure()
        }
    }

    LaunchedEffect(state.titanBattle?.expiresAt) {
        val battle = state.titanBattle
        if (battle != null) {
            if (activeBattleId == null) {
                activeBattleId = battle.challengeId
                winsAtBattleStart = state.titanWins
            }
            isFeatureHubOpen = false
        } else if (activeBattleId != null) {
            battleResultWon = state.titanWins > winsAtBattleStart
            activeBattleId = null
        }
    }

    var previousOwnedPlanets by remember { mutableStateOf(state.ownedPlanets) }
    LaunchedEffect(state.ownedPlanets) {
        if (state.ownedPlanets.size > previousOwnedPlanets.size) {
            soundManager.playPlanetUnlock()
        }
        previousOwnedPlanets = state.ownedPlanets
    }

    var previousClaimedAchievements by remember { mutableStateOf(state.claimedAchievementIds) }
    LaunchedEffect(state.claimedAchievementIds) {
        if (state.claimedAchievementIds.size > previousClaimedAchievements.size) {
            soundManager.playAchievementClaimed()
        }
        previousClaimedAchievements = state.claimedAchievementIds
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    viewModel.resumeSimulation()
                    soundManager.resumeBackgroundMusic()
                }
                Lifecycle.Event.ON_STOP -> {
                    viewModel.pauseSimulation()
                    soundManager.pauseBackgroundMusic()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.resumeSimulation()
            soundManager.resumeBackgroundMusic()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.pauseSimulation()
            soundManager.pauseBackgroundMusic()
        }
    }

    val fleetMap = viewModel.fleetById

    // Логика выбора фона в зависимости от активного ивента
    val backgroundRes = R.drawable.background_cosmic_game
    val eventTint = when (state.activeEvent?.type) {
        GameEventType.STORM, GameEventType.BLACK_HOLE -> Color(0xFF5A3D8F)
        GameEventType.ASTEROID -> Color(0xFF6D5848)
        GameEventType.SOLAR_FLARE -> Color(0xFF9A512F)
        GameEventType.CYBER_VIRUS, GameEventType.PIRATE_RAID -> Color(0xFF71384C)
        else -> Color.Transparent
    }

    fun addFloatingText(text: String, x: Float, y: Float) {
        val id = floatingTextId.incrementAndGet()
        floatingTexts = floatingTexts
            .takeLast(MAX_FLOATING_TEXTS - 1) + FloatingTextData(id, text, x, y)
        scope.launch {
            delay(GameConstants.FloatingTextDuration)
            floatingTexts = floatingTexts.filter { it.id != id }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ДИНАМИЧЕСКИЙ ФОН
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // Затемнение для читаемости элементов
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .background(eventTint.copy(alpha = if (eventTint == Color.Transparent) 0f else 0.10f))
        )

        // Звезды
        repeat(if (reduceMotion) GameConstants.ReducedStarCount else GameConstants.StarCount) {
            Star(reduceMotion)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                state = state,
                dps = viewModel.calculateDPS(),
                onAchievementsClick = { isAchievementsOpen = true },
                onSettingsClick = { showSettings = true }
            )
            
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (state.titanBattle == null) {
                    state.scavengeTargets.forEach { target ->
                        key(target.id) {
                            DebrisTarget(target, maxWidth, maxHeight)
                        }
                    }
                }

                if (state.titanBattle == null) {
                    state.activeEvent?.let { event ->
                        EventBanner(event, state.eventTapsLeft) {
                            showEventInfo = event
                        }
                    }
                }
                state.pendingEventChain?.let { pending ->
                    EventChainPendingBanner(
                        pending = pending,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                if (combo > 1) {
                    ComboIndicator(
                        combo = combo,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-150).dp)
                    )
                }

                state.titanBattle?.let { battle ->
                    CommandChallengeBattle(
                        battle = battle,
                        modifier = Modifier.fillMaxSize()
                    ) { minionId ->
                        soundManager.playClick()
                        val damage = if (minionId == null) viewModel.onPlanetClick() else viewModel.attackBossMinion(minionId)
                        addFloatingText("−${formatNum(damage)}", 0.5f, 0.43f)
                    }
                } ?: PlanetButton(
                        planetId = state.currentPlanetId,
                        planetConfig = viewModel.planets[state.currentPlanetId] ?: viewModel.planets.values.first(),
                        modifier = Modifier.align(Alignment.Center)
                    ) { x, y ->
                        soundManager.playClick()
                        val value = viewModel.onPlanetClick()
                        addFloatingText("+${formatNum(value)}", x, y)
                    }

                val now = System.currentTimeMillis()
                val combatDrones = state.drones.filter { it.disabledUntil <= now }
                combatDrones.forEachIndexed { index, drone ->
                    key(drone.id) {
                        if (state.titanBattle != null) {
                            CombatDrone(
                                drone = drone,
                                index = index,
                                droneCount = combatDrones.size,
                                battle = state.titanBattle!!,
                                fleetItems = fleetMap,
                                gameAreaWidth = maxWidth,
                                gameAreaHeight = maxHeight
                            )
                        } else {
                            ScavengingDrone(drone, fleetMap, maxWidth, maxHeight) {
                                val cyberEvent = state.activeEvent?.takeIf { active ->
                                    active.type == GameEventType.CYBER_VIRUS && state.infectedDroneId == it
                                }
                                if (cyberEvent != null) showEventInfo = cyberEvent else viewModel.onDroneClick(it)
                            }
                        }
                    }
                }

                state.activeEvent?.takeIf { state.titanBattle == null }?.let { event ->
                    when (event.type) {
                        GameEventType.ASTEROID -> {
                            Asteroid(event, maxWidth, maxHeight) { viewModel.onAsteroidClick() }
                        }
                        GameEventType.BLACK_HOLE -> {
                            BlackHoleComponent(event, state.eventTapsLeft, maxWidth, maxHeight) {
                                viewModel.onBlackHoleClick()
                            }
                        }
                        GameEventType.PIRATE_RAID -> {
                            PirateRaidComponent(event, state.eventTapsLeft, maxWidth, maxHeight) {
                                viewModel.onPirateRaidClick()
                            }
                        }
                        GameEventType.METEOR_SHOWER -> Unit
                        GameEventType.DISTRESS_SIGNAL -> Unit
                        GameEventType.ABANDONED_STATION -> Unit
                        GameEventType.TRADING_SHIP -> TradingShipComponent(event, maxWidth, maxHeight) {
                            showEventInfo = event
                        }
                        else -> {}
                    }
                }

                floatingTexts.forEach { data ->
                    key(data.id) {
                        FloatingText(data, maxWidth, maxHeight)
                    }
                }
            }

        }

        if (isShopOpen) {
            ShopBar(
                viewModel = viewModel,
                state = state,
                onClose = { isShopOpen = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else if (isHangarOpen) {
            DroneHangarPanel(
                viewModel = viewModel,
                state = state,
                onClose = { isHangarOpen = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else if (isAchievementsOpen) {
            AchievementsPanel(
                viewModel = viewModel,
                state = state,
                onClose = { isAchievementsOpen = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else if (isQuestOpen) {
            QuestPanel(
                state = state,
                onClaim = { viewModel.claimQuestReward(it) },
                onClose = { isQuestOpen = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameNavigationButton(
                    icon = R.drawable.ui_button_quest_v2,
                    label = R.string.quests,
                    description = R.string.quests,
                    onClick = { isQuestOpen = true },
                    modifier = Modifier.weight(1f)
                )
                GameNavigationButton(
                    icon = R.drawable.ui_button_shop_v2,
                    label = R.string.navigation_shop,
                    description = R.string.open_shop,
                    onClick = { isShopOpen = true },
                    modifier = Modifier.weight(1f)
                )
                GameNavigationButton(
                    icon = R.drawable.ui_button_hangar,
                    label = R.string.navigation_hangar,
                    description = R.string.open_hangar,
                    onClick = { isHangarOpen = true },
                    modifier = Modifier.weight(1f)
                )
                GameNavigationButton(
                    icon = R.drawable.ui_button_command_center,
                    label = R.string.command_center_short,
                    description = R.string.command_center,
                    onClick = { isFeatureHubOpen = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (isFeatureHubOpen) {
            FeatureHub(viewModel, state, onClose = { isFeatureHubOpen = false })
        }

        battleResultWon?.let { won ->
            AlertDialog(
                onDismissRequest = { battleResultWon = null },
                title = { Text(stringResource(if (won) R.string.challenge_victory_title else R.string.challenge_defeat_title)) },
                text = { Text(stringResource(if (won) R.string.challenge_victory_message else R.string.challenge_defeat_message)) },
                confirmButton = {
                    Button(onClick = { battleResultWon = null }) {
                        Text(stringResource(R.string.continue_button))
                    }
                }
            )
        }

        // ОВЕРЛЕЙ ОТКРЫТИЯ КЕЙСА
        CaseOpeningOverlay(
            isOpening = state.isOpeningCase,
            caseType = state.openingCaseType ?: com.example.myapplication.CaseType.COMMON,
            lastDroppedDrone = state.lastDroppedDroneId?.let { fleetMap[it] },
            onFinishOpening = { viewModel.finishOpeningCase() },
            onClearReward = { viewModel.clearReward() },
            reduceMotion = reduceMotion
        )

        if (showSettings) {
            SettingsScreen(
                selectedLanguage = selectedLanguage,
                onLanguageSelected = onLanguageSelected,
                reduceMotion = reduceMotion,
                onReduceMotionChanged = onReduceMotionChanged,
                onBack = { showSettings = false }
            )
        }

        showEventInfo?.let { event ->
            if (event.type == GameEventType.DISTRESS_SIGNAL) {
                DistressSignalDialog(
                    reward = event.reward,
                    onSalvage = {
                        viewModel.respondToDistressSignal(DistressChoice.SALVAGE)
                        showEventInfo = null
                    },
                    onRescue = {
                        viewModel.respondToDistressSignal(DistressChoice.RESCUE)
                        showEventInfo = null
                    },
                    onDismiss = { showEventInfo = null }
                )
            } else if (event.type == GameEventType.CYBER_VIRUS) {
                CyberVirusDialog(
                    event = event,
                    onResolved = {
                        viewModel.resolveCyberVirus(it)
                        showEventInfo = null
                    },
                    onDismiss = { showEventInfo = null }
                )
            } else if (event.type == GameEventType.ABANDONED_STATION) {
                AbandonedStationDialog(
                    reward = event.reward,
                    onSafeRoute = {
                        viewModel.respondToAbandonedStation(StationChoice.SAFE_ROUTE)
                        showEventInfo = null
                    },
                    onReactorCore = {
                        viewModel.respondToAbandonedStation(StationChoice.REACTOR_CORE)
                        showEventInfo = null
                    },
                    onDismiss = { showEventInfo = null }
                )
            } else if (event.type == GameEventType.TRADING_SHIP) {
                TradingShipMarket(
                    event = event,
                    totalDebris = state.totalDebris,
                    onBuy = {
                        viewModel.buyTradeOffer(it)
                        showEventInfo = null
                    },
                    onDismiss = { showEventInfo = null }
                )
            } else {
                EventInfoDialog(event = event, onDismiss = { showEventInfo = null })
            }
        }

        state.eventChainResult?.let { result ->
            EventChainResultDialog(result, viewModel::clearEventChainResult)
        }

        // СТАРТОВЫЙ ЭКРАН
        if (state.lastOfflineReward > 0.0) {
            AlertDialog(
                onDismissRequest = viewModel::clearOfflineReward,
                title = { Text(stringResource(R.string.offline_reward_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.offline_reward_message,
                            formatNum(state.lastOfflineReward)
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::clearOfflineReward) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            )
        }

        if (showStartScreen) {
            val promptTransition = rememberInfiniteTransition(label = "start_prompt")
            val animatedPromptOffset by promptTransition.animateFloat(
                initialValue = 0f,
                targetValue = -10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "start_prompt_offset"
            )
            val animatedPromptAlpha by promptTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "start_prompt_alpha"
            )
            val promptOffset = if (reduceMotion) 0f else animatedPromptOffset
            val promptAlpha = if (reduceMotion) 1f else animatedPromptAlpha

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = startScreenOffset.value
                        alpha = startScreenAlpha.value
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        scope.launch {
                            soundManager.playClick()
                            if (reduceMotion) {
                                showStartScreen = false
                                return@launch
                            }
                            // Анимация ухода вверх и исчезновения
                            launch {
                                startScreenOffset.animateTo(
                                    targetValue = -1000f,
                                    animationSpec = tween(durationMillis = 800)
                                )
                            }
                            launch {
                                startScreenAlpha.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 800)
                                )
                            }
                            delay(800)
                            showStartScreen = false
                        }
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.play_fon_game),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 44.dp)
                        .offset(y = promptOffset.dp)
                        .graphicsLayer { alpha = promptAlpha },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(76.dp),
                        shape = CircleShape,
                        color = AppColors.Primary,
                        border = androidx.compose.foundation.BorderStroke(
                            3.dp,
                            Color.White.copy(alpha = 0.85f)
                        ),
                        shadowElevation = 12.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "▶",
                                color = Color(0xFF071426),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.offset(x = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.tap_to_continue),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.68f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuestLauncherButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ui_button_quest_v2),
        contentDescription = stringResource(R.string.quests),
        modifier = modifier
            .size(60.dp)
            .clickable(onClick = onClick),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun GameNavigationButton(
    icon: Int,
    label: Int,
    description: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.height(80.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
            modifier = Modifier.size(60.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = stringResource(label),
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CommandChallengeBattle(
    battle: com.example.myapplication.TitanBattle,
    modifier: Modifier = Modifier,
    onAttack: (Int?) -> Unit
) {
    val entrance = remember(battle.expiresAt) { Animatable(0.35f) }
    var pressed by remember(battle.expiresAt) { mutableStateOf(false) }
    val hitScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = tween(90),
        label = "boss_hit"
    )
    val idleAnimation = rememberInfiniteTransition(label = "boss_idle")
    val idleOffset by idleAnimation.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "boss_float"
    )
    val idleScale by idleAnimation.animateFloat(
        initialValue = 0.995f,
        targetValue = 1.005f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "boss_breathe"
    )
    val secondsLeft = ((battle.expiresAt - System.currentTimeMillis()).coerceAtLeast(0L) + 999L) / 1_000L

    LaunchedEffect(battle.expiresAt) {
        entrance.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 140f))
    }
    LaunchedEffect(pressed) {
        if (pressed) {
            delay(85)
            pressed = false
        }
    }

    Box(modifier) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(challengeBattleName(battle.challengeId)), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.challenge_time_left, secondsLeft), color = if (secondsLeft <= 10) AppColors.Danger else AppColors.Secondary, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { (battle.health / battle.maxHealth).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp).padding(top = 4.dp),
                color = AppColors.Danger,
                trackColor = Color.White.copy(alpha = 0.14f)
            )
            Text("${formatNum(battle.health)} / ${formatNum(battle.maxHealth)}", color = Color.White.copy(alpha = .75f), fontSize = 11.sp)
            when {
                battle.shieldCharges > 0 -> Text(stringResource(R.string.challenge_shield_status, battle.shieldCharges), color = AppColors.Warning, fontWeight = FontWeight.Bold)
                battle.minions > 0 -> Text(stringResource(R.string.challenge_minion_status, battle.minions), color = AppColors.Warning, fontWeight = FontWeight.Bold)
                battle.challengeId == ChallengeId.NEBULA_DRAGON -> Text(stringResource(R.string.challenge_dragon_regenerating), color = AppColors.Danger, fontWeight = FontWeight.Bold)
            }
        }

        Image(
            painter = painterResource(challengeBattleArt(battle.challengeId)),
            contentDescription = stringResource(challengeBattleName(battle.challengeId)),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.86f)
                .height(285.dp)
                .graphicsLayer {
                    scaleX = entrance.value * hitScale * idleScale
                    scaleY = entrance.value * hitScale * idleScale
                    alpha = entrance.value
                    translationY = idleOffset
                    rotationZ = 0f
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    pressed = true
                    onAttack(null)
                },
            contentScale = ContentScale.Fit
        )

        if (battle.bossMinions.isNotEmpty()) {
            val orbit by idleAnimation.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(8_000, easing = androidx.compose.animation.core.LinearEasing)),
                label = "minion_orbit"
            )
            battle.bossMinions.forEachIndexed { index, minion ->
                key(minion.id) {
                    val angle = Math.toRadians((orbit + index * 360f / battle.bossMinions.size).toDouble())
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = (cos(angle) * 132).toFloat().dp, y = (sin(angle) * 105).toFloat().dp)
                            .width(64.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onAttack(minion.id) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(R.drawable.boss_dragon_minion),
                            contentDescription = stringResource(R.string.challenge_minion_robot),
                            modifier = Modifier.size(58.dp),
                            contentScale = ContentScale.Fit
                        )
                        LinearProgressIndicator(
                            progress = { (minion.health / minion.maxHealth).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(5.dp),
                            color = AppColors.Danger,
                            trackColor = Color.Black.copy(alpha = .65f)
                        )
                    }
                }
            }
        }

        Text(
            stringResource(R.string.tap_boss_to_attack),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 92.dp)
                .background(Color.Black.copy(alpha = .7f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun challengeBattleArt(id: ChallengeId): Int = when (id) {
    ChallengeId.VOID_LEVIATHAN -> R.drawable.boss_void_game
    ChallengeId.SOLAR_DEVOURER -> R.drawable.boss_solar_game
    ChallengeId.DREADNOUGHT_EMPRESS -> R.drawable.boss_dreadnought_game
    ChallengeId.NEBULA_DRAGON -> R.drawable.boss_dragon_game
}

private fun challengeBattleName(id: ChallengeId): Int = when (id) {
    ChallengeId.VOID_LEVIATHAN -> R.string.challenge_void_leviathan
    ChallengeId.SOLAR_DEVOURER -> R.string.challenge_solar_devourer
    ChallengeId.DREADNOUGHT_EMPRESS -> R.string.challenge_dreadnought_empress
    ChallengeId.NEBULA_DRAGON -> R.string.challenge_nebula_dragon
}

@Composable
private fun CombatDrone(
    drone: com.example.myapplication.DroneData,
    index: Int,
    droneCount: Int,
    battle: com.example.myapplication.TitanBattle,
    fleetItems: Map<String, com.example.myapplication.FleetConfig>,
    gameAreaWidth: androidx.compose.ui.unit.Dp,
    gameAreaHeight: androidx.compose.ui.unit.Dp
) {
    val animation = rememberInfiniteTransition(label = "combat_drone_${drone.id}")
    val orbit by animation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8_000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "combat_orbit_${drone.id}"
    )
    val shot by animation.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1_000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "combat_shot_${drone.id}"
    )
    val droneAngle = Math.toRadians((index * 360f / droneCount.coerceAtLeast(1) - 90f).toDouble())
    val droneX = gameAreaWidth * (0.5f + cos(droneAngle).toFloat() * 0.38f)
    val droneY = gameAreaHeight * (0.54f + sin(droneAngle).toFloat() * 0.34f)
    val targetIndex = if (battle.bossMinions.isEmpty()) 0 else index % battle.bossMinions.size
    val targetAngle = Math.toRadians((orbit + targetIndex * 360f / battle.bossMinions.size.coerceAtLeast(1)).toDouble())
    val targetX = gameAreaWidth * 0.5f + (cos(targetAngle) * 132).toFloat().dp
    val targetY = gameAreaHeight * 0.5f + (sin(targetAngle) * 105).toFloat().dp

    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        if (shot > 0.72f) {
            drawLine(
                color = Color(0xFF55E7FF).copy(alpha = ((shot - 0.72f) / 0.28f).coerceIn(0f, 1f)),
                start = androidx.compose.ui.geometry.Offset(droneX.toPx(), droneY.toPx()),
                end = androidx.compose.ui.geometry.Offset(targetX.toPx(), targetY.toPx()),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
    Box(
        modifier = Modifier
            .offset(x = droneX - 22.dp, y = droneY - 22.dp)
            .size(44.dp)
            .graphicsLayer {
                scaleX = 1f + shot * 0.08f
                scaleY = 1f + shot * 0.08f
            },
        contentAlignment = Alignment.Center
    ) {
        fleetItems[drone.type]?.let { FleetIcon(it, 44.dp) }
    }
}

private const val MAX_FLOATING_TEXTS = 40
