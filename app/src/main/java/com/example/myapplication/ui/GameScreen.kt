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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    reduceMotion: Boolean,
    onReduceMotionChanged: (Boolean) -> Unit,
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.gameState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val soundManager = remember { SoundManager() }
    val floatingTextId = remember { AtomicLong(0L) }
    var floatingTexts by remember { mutableStateOf(listOf<FloatingTextData>()) }
    var isShopOpen by remember { mutableStateOf(false) }
    var isQuestOpen by remember { mutableStateOf(false) }
    var isFeatureHubOpen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showEventInfo by remember { mutableStateOf<GameEvent?>(null) }

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
            isQuestOpen
            || isFeatureHubOpen
    ) {
        when {
            showSettings -> showSettings = false
            showEventInfo != null -> showEventInfo = null
            state.eventChainResult != null -> viewModel.clearEventChainResult()
            state.lastOfflineReward > 0.0 -> viewModel.clearOfflineReward()
            isShopOpen -> isShopOpen = false
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

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.resumeSimulation()
                Lifecycle.Event.ON_STOP -> viewModel.pauseSimulation()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.resumeSimulation()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.pauseSimulation()
        }
    }

    val fleetMap = viewModel.fleetById

    // Логика выбора фона в зависимости от активного ивента
    val backgroundRes = remember<Int>(state.activeEvent?.type) {
        when (state.activeEvent?.type) {
            GameEventType.STORM -> R.drawable.background_storm_v2
            GameEventType.ASTEROID -> R.drawable.background_asteroid_v2
            GameEventType.BLACK_HOLE -> R.drawable.background_storm_v2
            GameEventType.SOLAR_FLARE -> R.drawable.background_storm_v2
            GameEventType.CYBER_VIRUS -> R.drawable.background_pirates_v2
            GameEventType.DISTRESS_SIGNAL -> R.drawable.background_space_v2
            GameEventType.ABANDONED_STATION -> R.drawable.background_space_v2
            GameEventType.PIRATE_RAID -> R.drawable.background_pirates_v2
            GameEventType.TRADING_SHIP -> R.drawable.background_space_v2
            else -> R.drawable.background_minimal_space
        }
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
                .background(Color.Black.copy(alpha = 0.2f))
        )

        // Звезды
        repeat(if (reduceMotion) GameConstants.ReducedStarCount else GameConstants.StarCount) {
            Star(reduceMotion)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                state = state,
                dps = viewModel.calculateDPS(),
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

                PlanetButton(
                    planetId = state.currentPlanetId,
                    planetConfig = viewModel.planets[state.currentPlanetId] ?: viewModel.planets.values.first(),
                    modifier = Modifier.align(Alignment.Center)
                ) { x, y ->
                    soundManager.playClick()
                    val value = viewModel.onPlanetClick()
                    addFloatingText("+${formatNum(value)}", x, y)
                }

                val now = System.currentTimeMillis()
                state.drones.filter { it.disabledUntil <= now }.forEach { drone ->
                    key(drone.id) {
                        ScavengingDrone(drone, fleetMap, maxWidth, maxHeight) {
                            viewModel.onDroneClick(it)
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
                        GameEventType.METEOR_SHOWER -> Unit
                        GameEventType.DISTRESS_SIGNAL -> Unit
                        GameEventType.ABANDONED_STATION -> Unit
                        GameEventType.TRADING_SHIP -> Unit
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
        } else if (isQuestOpen) {
            QuestPanel(
                state = state,
                onClaim = { viewModel.claimQuestReward(it) },
                onClose = { isQuestOpen = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuestLauncherButton(
                    onClick = { isQuestOpen = true }
                )
                ShopLauncherButton(
                    onClick = { isShopOpen = true }
                )
                CommandCenterButton(onClick = { isFeatureHubOpen = true })
            }
        }

        if (isFeatureHubOpen) {
            FeatureHub(viewModel, state, onClose = { isFeatureHubOpen = false })
        }

        // ОВЕРЛЕЙ ОТКРЫТИЯ КЕЙСА
        CaseOpeningOverlay(
            isOpening = state.isOpeningCase,
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
                AlertDialog(
                    onDismissRequest = { showEventInfo = null },
                    title = { Text(stringResource(R.string.event_trading_ship)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.event_desc_trading_ship))
                            Text(stringResource(R.string.trade_power_offer, formatNum(event.reward)))
                            Text(stringResource(R.string.trade_luck_offer, formatNum(event.reward * 0.75)))
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = state.totalDebris >= event.reward,
                            onClick = {
                                viewModel.buyTradeOffer(TradeOffer.POWER_CORE)
                                showEventInfo = null
                            }
                        ) { Text(stringResource(R.string.trade_buy_power)) }
                    },
                    dismissButton = {
                        TextButton(
                            enabled = state.totalDebris >= event.reward * 0.75,
                            onClick = {
                                viewModel.buyTradeOffer(TradeOffer.LUCK_SCANNER)
                                showEventInfo = null
                            }
                        ) { Text(stringResource(R.string.trade_buy_luck)) }
                    }
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
        painter = painterResource(R.drawable.ui_button_quest),
        contentDescription = stringResource(R.string.quests),
        modifier = modifier
            .size(76.dp)
            .clickable(onClick = onClick),
        contentScale = ContentScale.Fit
    )
}

private const val MAX_FLOATING_TEXTS = 40
