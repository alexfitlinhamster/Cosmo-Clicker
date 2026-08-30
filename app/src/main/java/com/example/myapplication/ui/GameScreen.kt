package com.example.myapplication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.example.myapplication.ui.components.Button
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.myapplication.EconomyBalance
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
    soundEnabled: Boolean = true,
    onSoundEnabledChanged: (Boolean) -> Unit = {},
    reducedMotion: Boolean = false,
    onReducedMotionChanged: (Boolean) -> Unit = {},
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.gameState.collectAsState()
    val combo by viewModel.combo.collectAsState()
    val autoClickBlockSeconds by viewModel.autoClickBlockSeconds.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val soundManager = remember(context) { SoundManager(context) }
    LaunchedEffect(soundManager, soundEnabled) {
        if (soundEnabled) soundManager.resumeBackgroundMusic() else soundManager.pauseBackgroundMusic()
    }
    val floatingTextId = remember { AtomicLong(0L) }
    val floatingTexts = remember { mutableStateListOf<FloatingTextData?>().apply { repeat(MAX_FLOATING_TEXTS) { add(null) } } }
    var nextFloatingTextSlot by remember { mutableIntStateOf(0) }
    var lastFloatingTextMillis by remember { mutableLongStateOf(0L) }
    var isShopOpen by remember { mutableStateOf(false) }
    var isHangarOpen by remember { mutableStateOf(false) }
    var isAchievementsOpen by remember { mutableStateOf(false) }
    var isStatisticsOpen by remember { mutableStateOf(false) }
    var isQuestOpen by remember { mutableStateOf(false) }
    var isPrestigeShopOpen by remember { mutableStateOf(false) }
    var isGalaxyRouteOpen by rememberSaveable { mutableStateOf(false) }
    // Settings must survive Activity recreation when the app locale changes.
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var unlockedPlanetId by remember { mutableStateOf<String?>(null) }

    // Состояние стартового экрана
    var showStartScreen by rememberSaveable { mutableStateOf(true) }
    val startScreenOffset = remember { Animatable(0f) }
    val startScreenAlpha = remember { Animatable(1f) }

    BackHandler(
        enabled = showSettings ||
            (!showStartScreen && state.lastOfflineReward > 0.0) ||
            isShopOpen ||
            isHangarOpen ||
            isAchievementsOpen ||
            isStatisticsOpen ||
            isPrestigeShopOpen ||
            isGalaxyRouteOpen ||
            isQuestOpen
    ) {
        when {
            showSettings -> showSettings = false
            !showStartScreen && state.lastOfflineReward > 0.0 -> viewModel.clearOfflineReward()
            isShopOpen -> isShopOpen = false
            isHangarOpen -> isHangarOpen = false
            isAchievementsOpen -> isAchievementsOpen = false
            isStatisticsOpen -> isStatisticsOpen = false
            isPrestigeShopOpen -> isPrestigeShopOpen = false
            isGalaxyRouteOpen -> isGalaxyRouteOpen = false
            isQuestOpen -> isQuestOpen = false
        }
    }

    DisposableEffect(soundManager) {
        onDispose { soundManager.close() }
    }

    LaunchedEffect(state.activeEvent?.startedAt) {
        if (soundEnabled && state.activeEvent != null) soundManager.playEventStart()
    }

    LaunchedEffect(state.eventChainResult) {
        state.eventChainResult?.let {
            if (soundEnabled) {
                if (it.success) soundManager.playEventSuccess() else soundManager.playEventFailure()
            }
            delay(1_200L)
            viewModel.clearEventChainResult()
        }
    }

    var previousOwnedPlanets by remember { mutableStateOf(state.ownedPlanets) }
    LaunchedEffect(state.ownedPlanets) {
        if (state.ownedPlanets.size > previousOwnedPlanets.size) {
            if (soundEnabled) soundManager.playPlanetUnlock()
            unlockedPlanetId = (state.ownedPlanets - previousOwnedPlanets)
                .maxByOrNull(EconomyBalance::planetIndex)
        }
        previousOwnedPlanets = state.ownedPlanets
    }

    LaunchedEffect(unlockedPlanetId) {
        if (unlockedPlanetId != null) {
            delay(3_500L)
            unlockedPlanetId = null
        }
    }

    var previousClaimedAchievements by remember { mutableStateOf(state.claimedAchievementIds) }
    LaunchedEffect(state.claimedAchievementIds) {
        if (state.claimedAchievementIds.size > previousClaimedAchievements.size) {
            if (soundEnabled) soundManager.playAchievementClaimed()
        }
        previousClaimedAchievements = state.claimedAchievementIds
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    viewModel.resumeSimulation()
                    if (soundEnabled) soundManager.resumeBackgroundMusic()
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
            if (soundEnabled) soundManager.resumeBackgroundMusic()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.pauseSimulation()
            soundManager.pauseBackgroundMusic()
        }
    }

    val fleetMap = viewModel.fleetById
    val nextPlanetIndex = EconomyBalance.nextPlanetIndex(state.ownedPlanets)
    val nextPlanetPrice = nextPlanetIndex?.let { viewModel.planets["p$it"]?.price }
    val nextPlanetImageRes = nextPlanetIndex?.let { viewModel.planets["p$it"]?.imageRes }

    // Логика выбора фона в зависимости от активного ивента
    val backgroundRes = R.drawable.background_cosmic_game_v2
    val eventTint = when (state.activeEvent?.type) {
        GameEventType.STORM, GameEventType.BLACK_HOLE -> Color(0xFF5A3D8F)
        GameEventType.SOLAR_FLARE -> Color(0xFF9A512F)
        GameEventType.CYBER_VIRUS, GameEventType.PIRATE_RAID -> Color(0xFF71384C)
        else -> Color.Transparent
    }

    fun addFloatingText(text: String, x: Float, y: Float) {
        val now = System.currentTimeMillis()
        if (now - lastFloatingTextMillis < FLOATING_TEXT_THROTTLE_MS) return
        lastFloatingTextMillis = now
        val id = floatingTextId.incrementAndGet()
        val slot = nextFloatingTextSlot
        nextFloatingTextSlot = (nextFloatingTextSlot + 1) % MAX_FLOATING_TEXTS
        floatingTexts[slot] = FloatingTextData(id, text, x, y)
        scope.launch {
            delay(GameConstants.FloatingTextDuration)
            if (floatingTexts[slot]?.id == id) floatingTexts[slot] = null
        }
    }

    val spaceMotion = if (!reducedMotion) rememberInfiniteTransition(label = "space_background") else null
    val starTwinklePhase = spaceMotion?.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2.0).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4_800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "star_twinkle_phase"
    )?.value ?: 0f
    val cosmicParticlePhase = spaceMotion?.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cosmic_particle_phase"
    )?.value ?: 0f
    // A single clock drives every rotor. Previously each drone owned an infinite
    // transition, multiplying animation work as the active fleet grew.
    val sharedRotorPhase = spaceMotion?.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shared_drone_rotor_phase"
    )?.value ?: 0f

    Box(modifier = Modifier.fillMaxSize()) {
        // ДИНАМИЧЕСКИЙ ФОН
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        if (!reducedMotion) CosmicParticleTrails(cosmicParticlePhase)

        // Затемнение для читаемости элементов
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .background(eventTint.copy(alpha = if (eventTint == Color.Transparent) 0f else 0.10f))
        )

        // Звезды
        repeat(GameConstants.StarCount) { index ->
            Star(index = index, twinklePhase = starTwinklePhase, reduceMotion = reducedMotion)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                state = state,
                dps = viewModel.calculateDPS(),
                nextPlanetIndex = nextPlanetIndex,
                nextPlanetPrice = nextPlanetPrice,
                nextPlanetImageRes = nextPlanetImageRes,
                onAchievementsClick = { isAchievementsOpen = true },
                onPrestigeShopClick = { isPrestigeShopOpen = true },
                onRouteClick = { isGalaxyRouteOpen = true },
                onSettingsClick = { showSettings = true }
            )
            
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                state.scavengeTargets.forEach { target ->
                    key(target.id) {
                        DebrisTarget(
                            target,
                            maxWidth,
                            maxHeight,
                            onClick = null
                        )
                    }
                }

                state.activeEvent?.let { event ->
                    EventBanner(event, state.eventTapsLeft)
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
                        modifier = Modifier.align(Alignment.Center),
                        reducedMotion = reducedMotion
                    ) { x, y ->
                        val value = viewModel.onPlanetClick(x, y)
                        if (value > 0.0) {
                            if (soundEnabled) soundManager.playClick()
                            val planetWidthFraction = GameConstants.PlanetSize.value / maxWidth.value
                            val planetHeightFraction = GameConstants.PlanetSize.value / maxHeight.value
                            addFloatingText(
                                "+${formatNum(value)}",
                                (0.5f + (x - 0.5f) * planetWidthFraction).coerceIn(0f, 1f),
                                (0.5f + (y - 0.5f) * planetHeightFraction).coerceIn(0f, 1f)
                            )
                        }
                    }

                state.drones.forEachIndexed { index, drone ->
                    key(drone.id) {
                        ScavengingDrone(drone, fleetMap, maxWidth, maxHeight, sharedRotorPhase) {
                            viewModel.onDroneClick(it)
                        }
                    }
                }

                state.activeEvent?.let { event ->
                    Image(
                        painter = painterResource(R.drawable.bg_events_minimal_v2),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().zIndex(-1f),
                        contentScale = ContentScale.Crop
                    )
                    when (event.type) {
                        GameEventType.BLACK_HOLE -> {
                            BlackHoleComponent(
                                event,
                                state.eventTapsLeft,
                                state.stormSequence,
                                state.stormProgress,
                                state.stormRound,
                                maxWidth,
                                maxHeight,
                                viewModel::onBlackHoleNodeClick
                            )
                        }
                        GameEventType.PIRATE_RAID -> {
                            PirateAmbushComponent(event, state.stormSequence, state.stormProgress, state.stormRound, maxWidth, maxHeight, viewModel::onPirateTargetClick, viewModel::resolvePirateRaid)
                        }
                        GameEventType.STORM -> {
                            StormNodeChallenge(event, state.stormSequence, state.stormProgress, state.stormRound, maxWidth, maxHeight, viewModel::onStormNodeClick)
                        }
                        GameEventType.SOLAR_FLARE -> {
                            SolarFlareProtocol(event, state.stormSequence, state.stormProgress, state.stormRound, maxWidth, maxHeight, viewModel::onSolarChannelClick)
                        }
                        // The meteor shower is represented only by falling debris.
                        // There is no separate four-target interception mini-game.
                        GameEventType.METEOR_SHOWER -> Unit
                        GameEventType.DISTRESS_SIGNAL -> DistressSignalScanner(
                            event = event,
                            sequence = state.stormSequence,
                            progress = state.stormProgress,
                            phase = state.stormRound,
                            gameAreaWidth = maxWidth,
                            gameAreaHeight = maxHeight,
                            onNodeClick = viewModel::onDistressSignalNode,
                            onSalvage = { viewModel.respondToDistressSignal(DistressChoice.SALVAGE) },
                            onRescue = { viewModel.respondToDistressSignal(DistressChoice.RESCUE) }
                        )
                        GameEventType.ABANDONED_STATION -> AbandonedStationAccess(
                            event = event,
                            sequence = state.stormSequence,
                            progress = state.stormProgress,
                            phase = state.stormRound,
                            gameAreaWidth = maxWidth,
                            gameAreaHeight = maxHeight,
                            onRelayClick = viewModel::onStationRelayClick,
                            onSafeRoute = { viewModel.respondToAbandonedStation(StationChoice.SAFE_ROUTE) },
                            onReactorCore = { viewModel.respondToAbandonedStation(StationChoice.REACTOR_CORE) }
                        )
                        GameEventType.TRADING_SHIP -> TradingShipComponent(
                            event = event,
                            hullLeft = state.eventTapsLeft,
                            gameAreaWidth = maxWidth,
                            gameAreaHeight = maxHeight
                        )
                        GameEventType.CYBER_VIRUS -> CyberVirusField(
                            event = event,
                            remaining = state.eventTapsLeft,
                            gameAreaWidth = maxWidth,
                            gameAreaHeight = maxHeight,
                            onResolved = viewModel::resolveCyberVirus
                        )
                    }
                }

                floatingTexts.filterNotNull().forEach { data ->
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
        } else if (isStatisticsOpen) {
            StatisticsPanel(
                viewModel = viewModel,
                state = state,
                onClose = { isStatisticsOpen = false },
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
                onClaimDailyReward = viewModel::claimDailyReward,
                onClose = { isQuestOpen = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xB80A1322))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameNavigationButton(
                    icon = R.drawable.ic_nav_quests_minimal,
                    label = R.string.goals,
                    description = R.string.goals,
                    onClick = { isQuestOpen = true },
                    modifier = Modifier.weight(1f)
                )
                GameNavigationButton(
                    icon = R.drawable.ic_nav_shop_minimal,
                    label = R.string.navigation_shop,
                    description = R.string.open_shop,
                    onClick = { isShopOpen = true },
                    modifier = Modifier.weight(1f)
                )
                GameNavigationButton(
                    icon = R.drawable.ic_nav_hangar_minimal,
                    label = R.string.navigation_hangar,
                    description = R.string.open_hangar,
                    onClick = { isHangarOpen = true },
                    modifier = Modifier.weight(1f)
                )
                GameNavigationButton(
                    icon = R.drawable.ic_nav_stats_minimal,
                    label = R.string.statistics,
                    description = R.string.open_statistics,
                    onClick = { isStatisticsOpen = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (autoClickBlockSeconds > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 112.dp, start = 20.dp, end = 20.dp)
                    .fillMaxWidth()
                    .heightIn(min = 68.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ui_autoclick_warning_v1),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                Text(
                    stringResource(R.string.autoclicker_detected, autoClickBlockSeconds),
                    modifier = Modifier.padding(horizontal = 44.dp, vertical = 14.dp),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        unlockedPlanetId?.let { planetId ->
            val planet = viewModel.planets[planetId]
            if (planet != null) {
                PlanetUnlockBanner(
                    planetIndex = EconomyBalance.planetIndex(planetId),
                    planetName = planet.name,
                    incomeBonusPercent = ((EconomyBalance.planetIncomeMultiplier(planetId) - 1.0) * 100).toInt(),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 112.dp, start = 20.dp, end = 20.dp)
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
            onOpeningPulse = { frame ->
                if (soundEnabled) soundManager.playCaseOpeningPulse(frame, state.openingCaseType ?: com.example.myapplication.CaseType.COMMON)
            },
            reduceMotion = reducedMotion
        )

        if (showSettings) {
            SettingsScreen(
                selectedLanguage = selectedLanguage,
                onLanguageSelected = onLanguageSelected,
                soundEnabled = soundEnabled,
                onSoundEnabledChanged = onSoundEnabledChanged,
                reducedMotion = reducedMotion,
                onReducedMotionChanged = onReducedMotionChanged,
                onAchievements = {
                    showSettings = false
                    isAchievementsOpen = true
                },
                onResetGame = {
                    viewModel.resetGame()
                    showSettings = false
                },
                onBack = { showSettings = false }
            )
        }

        // СТАРТОВЫЙ ЭКРАН
        if (!showStartScreen && state.lastOfflineReward > 0.0) {
            SpaceDialog(
                title = stringResource(R.string.offline_reward_title),
                onDismiss = viewModel::clearOfflineReward,
                modifier = Modifier.widthIn(max = 360.dp),
                backgroundRes = R.drawable.bg_offline_reward_space_v1,
                content = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(132.dp)
                                .background(AppColors.Primary.copy(alpha = .10f), CircleShape)
                                .border(2.dp, AppColors.Primary.copy(alpha = .62f), CircleShape)
                                .padding(5.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.offline_drone_reward_v1),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "+${formatNum(state.lastOfflineReward)}",
                            color = AppColors.Primary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            stringResource(R.string.debris),
                            color = AppColors.Secondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.offline_time_away, formatOfflineDuration(state.lastOfflineSeconds)),
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = viewModel::clearOfflineReward,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp)
                    ) {
                        Text(
                            stringResource(R.string.collect_reward),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            )
        }

        if (isGalaxyRouteOpen) {
            GalaxyRouteDialog(viewModel, state) { isGalaxyRouteOpen = false }
        }

        if (showStartScreen) {
            val promptTransition = if (!reducedMotion) rememberInfiniteTransition(label = "start_prompt") else null
            val animatedPromptOffset = promptTransition?.animateFloat(
                initialValue = 0f,
                targetValue = -10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "start_prompt_offset"
            )?.value ?: 0f
            val animatedPromptAlpha = promptTransition?.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "start_prompt_alpha"
            )?.value ?: 1f
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
                            if (soundEnabled) soundManager.playClick()
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
                    painter = painterResource(id = R.drawable.play_fon_game_v2),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentScale = ContentScale.Crop
                )

                CosmicParticleTrails(cosmicParticlePhase)

                repeat(18) { index ->
                    Star(index = index + 100, twinklePhase = starTwinklePhase, reduceMotion = reducedMotion)
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 44.dp)
                        .offset(y = promptOffset.dp)
                        .graphicsLayer { alpha = promptAlpha },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.icon_game),
                        contentDescription = null,
                        modifier = Modifier.size(86.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.4.sp
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(86.dp), contentAlignment = Alignment.Center) {
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
    Surface(
        modifier = modifier
            .size(52.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = AppColors.SurfaceRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(alpha = .24f))
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_nav_quests_minimal),
            contentDescription = stringResource(R.string.quests),
            modifier = Modifier.padding(13.dp),
            tint = Color.Unspecified
        )
    }
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
        modifier = modifier
            .height(66.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
            modifier = Modifier.size(38.dp),
            tint = Color.Unspecified
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = stringResource(label),
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private const val MAX_FLOATING_TEXTS = 6
private const val FLOATING_TEXT_THROTTLE_MS = 120L

@Composable
private fun CosmicParticleTrails(phase: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val trails = listOf(
            Triple(0.00f, 0.12f, Color(0xFF8DEBFF)),
            Triple(0.38f, 0.46f, Color(0xFFD8B5FF)),
            Triple(0.73f, 0.72f, Color(0xFFFFE8A3))
        )
        trails.forEachIndexed { index, (delay, startY, color) ->
            val progress = ((phase - delay + 1f) % 1f) * 3f
            if (progress in 0f..1f) {
                val x = size.width * (-0.15f + progress * 1.30f)
                val y = size.height * (startY + progress * 0.18f)
                val length = size.minDimension * (0.10f + index * 0.018f)
                val alpha = (1f - kotlin.math.abs(progress - 0.5f) * 2f).coerceIn(0f, 1f)
                drawLine(
                    color = color.copy(alpha = alpha * 0.52f),
                    start = Offset(x - length, y - length * 0.28f),
                    end = Offset(x, y),
                    strokeWidth = 2.2f + index,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.9f),
                    radius = 2.5f + index * 0.5f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

private fun formatOfflineDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val hours = safe / 3_600L
    val minutes = (safe % 3_600L) / 60L
    return if (hours > 0L) "${hours}h ${minutes}m" else "${minutes.coerceAtLeast(1L)}m"
}
