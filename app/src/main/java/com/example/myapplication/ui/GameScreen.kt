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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
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

@Composable
fun GameScreen(
    selectedLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    backgroundMusicEnabled: Boolean,
    onBackgroundMusicChanged: (Boolean) -> Unit,
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.gameState.collectAsState()
    val combo by viewModel.combo.collectAsState()
    val autoClickBlockSeconds by viewModel.autoClickBlockSeconds.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val soundManager = remember(context) { SoundManager(context) }
    LaunchedEffect(backgroundMusicEnabled) {
        if (backgroundMusicEnabled) soundManager.resumeBackgroundMusic() else soundManager.pauseBackgroundMusic()
    }
    val floatingTextId = remember { AtomicLong(0L) }
    var floatingTexts by remember { mutableStateOf(listOf<FloatingTextData>()) }
    var isShopOpen by remember { mutableStateOf(false) }
    var isHangarOpen by remember { mutableStateOf(false) }
    var isAchievementsOpen by remember { mutableStateOf(false) }
    var isQuestOpen by remember { mutableStateOf(false) }
    var isPrestigeShopOpen by remember { mutableStateOf(false) }
    // Settings must survive Activity recreation when the app locale changes.
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showEventInfo by remember { mutableStateOf<GameEvent?>(null) }

    // Состояние стартового экрана
    var showStartScreen by rememberSaveable { mutableStateOf(true) }
    val startScreenOffset = remember { Animatable(0f) }
    val startScreenAlpha = remember { Animatable(1f) }

    BackHandler(
        enabled = showSettings ||
            showEventInfo != null ||
            state.eventChainResult != null ||
            (!showStartScreen && state.lastOfflineReward > 0.0) ||
            isShopOpen ||
            isHangarOpen ||
            isAchievementsOpen ||
            isPrestigeShopOpen ||
            isQuestOpen
    ) {
        when {
            showSettings -> showSettings = false
            showEventInfo != null -> showEventInfo = null
            state.eventChainResult != null -> viewModel.clearEventChainResult()
            !showStartScreen && state.lastOfflineReward > 0.0 -> viewModel.clearOfflineReward()
            isShopOpen -> isShopOpen = false
            isHangarOpen -> isHangarOpen = false
            isAchievementsOpen -> isAchievementsOpen = false
            isPrestigeShopOpen -> isPrestigeShopOpen = false
            isQuestOpen -> isQuestOpen = false
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

    DisposableEffect(lifecycleOwner, viewModel, backgroundMusicEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    viewModel.resumeSimulation()
                    if (backgroundMusicEnabled) soundManager.resumeBackgroundMusic()
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
            if (backgroundMusicEnabled) soundManager.resumeBackgroundMusic()
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
        repeat(GameConstants.StarCount) { index ->
            Star(index = index, twinklePhase = 0f)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                state = state,
                dps = viewModel.calculateDPS(),
                onAchievementsClick = { isAchievementsOpen = true },
                onPrestigeShopClick = { isPrestigeShopOpen = true },
                onSettingsClick = { showSettings = true }
            )
            
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                state.scavengeTargets.forEach { target ->
                    key(target.id) {
                        DebrisTarget(target, maxWidth, maxHeight)
                    }
                }

                state.activeEvent?.let { event ->
                    EventBanner(event, state.eventTapsLeft) {
                        showEventInfo = event
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

                PlanetButton(
                        planetId = state.currentPlanetId,
                        planetConfig = viewModel.planets[state.currentPlanetId] ?: viewModel.planets.values.first(),
                        modifier = Modifier.align(Alignment.Center)
                    ) { x, y ->
                        soundManager.playClick()
                        val value = viewModel.onPlanetClick()
                        val planetWidthFraction = GameConstants.PlanetSize.value / maxWidth.value
                        val planetHeightFraction = GameConstants.PlanetSize.value / maxHeight.value
                        addFloatingText(
                            "+${formatNum(value)}",
                            (0.5f + (x - 0.5f) * planetWidthFraction).coerceIn(0f, 1f),
                            (0.5f + (y - 0.5f) * planetHeightFraction).coerceIn(0f, 1f)
                        )
                    }

                state.drones.forEachIndexed { index, drone ->
                    key(drone.id) {
                        ScavengingDrone(drone, fleetMap, maxWidth, maxHeight) {
                            val cyberEvent = state.activeEvent?.takeIf { active ->
                                active.type == GameEventType.CYBER_VIRUS && state.infectedDroneId == it
                            }
                            if (cyberEvent != null) showEventInfo = cyberEvent else viewModel.onDroneClick(it)
                        }
                    }
                }

                state.activeEvent?.let { event ->
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
                        GameEventType.STORM, GameEventType.SOLAR_FLARE -> {
                            EventChallengeComponent(event, maxWidth, maxHeight) {
                                viewModel.onEventChallengeClick()
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
        } else if (isPrestigeShopOpen) {
            PrestigeShopPanel(
                viewModel = viewModel,
                state = state,
                onClose = { isPrestigeShopOpen = false },
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
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameNavigationButton(
                    icon = R.drawable.ui_button_quest_v3,
                    label = R.string.quests,
                    description = R.string.quests,
                    onClick = { isQuestOpen = true },
                    modifier = Modifier.weight(1f)
                )
                GameNavigationButton(
                    icon = R.drawable.ui_button_shop_v3,
                    label = R.string.navigation_shop,
                    description = R.string.open_shop,
                    onClick = { isShopOpen = true },
                    modifier = Modifier.weight(1f)
                )
                GameNavigationButton(
                    icon = R.drawable.ui_button_hangar_v2,
                    label = R.string.navigation_hangar,
                    description = R.string.open_hangar,
                    onClick = { isHangarOpen = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (autoClickBlockSeconds > 0) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 112.dp, start = 20.dp, end = 20.dp),
                shape = RoundedCornerShape(14.dp),
                color = AppColors.Danger.copy(alpha = 0.94f),
                shadowElevation = 8.dp
            ) {
                Text(
                    stringResource(R.string.autoclicker_detected, autoClickBlockSeconds),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ОВЕРЛЕЙ ОТКРЫТИЯ КЕЙСА
        CaseOpeningOverlay(
            isOpening = state.isOpeningCase,
            caseType = state.openingCaseType ?: com.example.myapplication.CaseType.COMMON,
            lastDroppedDrone = state.lastDroppedDroneId?.let { fleetMap[it] },
            remainingCases = state.pendingCaseOpenings + if (state.isOpeningCase) 1 else 0,
            bundleRewards = state.caseBundleRewards.mapNotNull { (id, count) -> fleetMap[id]?.let { it to count } },
            showBundleSummary = state.showCaseBundleSummary,
            onFinishOpening = { viewModel.finishOpeningCase() },
            onOpenAll = { viewModel.openAllPendingCases() },
            onClearReward = { viewModel.clearReward() },
            onClearBundleSummary = { viewModel.clearCaseBundleSummary() },
            reduceMotion = false
        )

        if (showSettings) {
            SettingsScreen(
                selectedLanguage = selectedLanguage,
                onLanguageSelected = onLanguageSelected,
                backgroundMusicEnabled = backgroundMusicEnabled,
                onBackgroundMusicChanged = onBackgroundMusicChanged,
                onResetGame = {
                    viewModel.resetGame()
                    showSettings = false
                },
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
            } else if (event.type == GameEventType.BLACK_HOLE) {
                BlackHoleEventDialog(
                    event = event,
                    tapsLeft = state.eventTapsLeft,
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
        if (!showStartScreen && state.lastOfflineReward > 0.0) {
            SpaceDialog(
                title = stringResource(R.string.offline_reward_title),
                onDismiss = viewModel::clearOfflineReward,
                content = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(R.drawable.drone_collection_art),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(130.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "+${formatNum(state.lastOfflineReward)}",
                            color = AppColors.Primary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(stringResource(R.string.debris), color = AppColors.Secondary, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.offline_time_away, formatOfflineDuration(state.lastOfflineSeconds)),
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                actions = {
                    Button(onClick = viewModel::clearOfflineReward, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.collect_reward))
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
            val promptOffset = animatedPromptOffset
            val promptAlpha = animatedPromptAlpha

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
                    Box(
                        modifier = Modifier.fillMaxWidth().height(104.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ui_start_button_v2),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                        Text(
                            text = stringResource(R.string.tap_to_continue),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 48.dp),
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuestLauncherButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ui_button_quest_v3),
        contentDescription = stringResource(R.string.quests),
        modifier = modifier
            .size(60.dp)
            .clip(RoundedCornerShape(11.dp))
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
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(11.dp)),
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

private const val MAX_FLOATING_TEXTS = 24

private fun formatOfflineDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val hours = safe / 3_600L
    val minutes = (safe % 3_600L) / 60L
    return if (hours > 0L) "${hours}h ${minutes}m" else "${minutes.coerceAtLeast(1L)}m"
}
