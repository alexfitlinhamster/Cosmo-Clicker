package com.example.myapplication.ui

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
import com.example.myapplication.FloatingTextData
import com.example.myapplication.GameEvent
import com.example.myapplication.GameEventType
import com.example.myapplication.GameViewModel
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
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.gameState.collectAsState()
    val scope = rememberCoroutineScope()
    val soundManager = remember { SoundManager() }
    val floatingTextId = remember { AtomicLong(0L) }
    var floatingTexts by remember { mutableStateOf(listOf<FloatingTextData>()) }
    var isShopOpen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showEventInfo by remember { mutableStateOf<GameEvent?>(null) }

    // Состояние стартового экрана
    var showStartScreen by remember { mutableStateOf(true) }
    val startScreenOffset = remember { Animatable(0f) }
    val startScreenAlpha = remember { Animatable(1f) }

    DisposableEffect(soundManager) {
        onDispose { soundManager.close() }
    }

    val fleetMap = remember(viewModel.fleetItems) {
        viewModel.fleetItems.associateBy { it.id }
    }

    // Логика выбора фона в зависимости от активного ивента
    val backgroundRes = remember<Int>(state.activeEvent?.type) {
        when (state.activeEvent?.type) {
            GameEventType.STORM -> R.drawable.background_storm
            GameEventType.ASTEROID -> R.drawable.background_asteroid
            GameEventType.BLACK_HOLE -> R.drawable.background_storm
            GameEventType.SOLAR_FLARE -> R.drawable.background_storm
            GameEventType.CYBER_VIRUS -> R.drawable.background_space
            else -> R.drawable.background_space
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
            contentScale = ContentScale.Crop
        )

        // Затемнение для читаемости элементов
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f))
        )

        // Звезды
        repeat(GameConstants.StarCount) {
            Star()
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
                    EventBanner(event) {
                        showEventInfo = event
                    }
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
                        GameEventType.METEOR_SHOWER -> Unit
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
        } else {
            ShopLauncherButton(
                onClick = { isShopOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
            )
        }

        // ОВЕРЛЕЙ ОТКРЫТИЯ КЕЙСА
        CaseOpeningOverlay(
            isOpening = state.isOpeningCase,
            lastDroppedDrone = state.lastDroppedDroneId?.let { fleetMap[it] },
            onFinishOpening = { viewModel.finishOpeningCase() },
            onClearReward = { viewModel.clearReward() }
        )

        if (showSettings) {
            SettingsScreen(
                selectedLanguage = selectedLanguage,
                onLanguageSelected = onLanguageSelected,
                onBack = { showSettings = false }
            )
        }

        showEventInfo?.let { event ->
            EventInfoDialog(event = event, onDismiss = { showEventInfo = null })
        }

        // СТАРТОВЫЙ ЭКРАН
        if (showStartScreen) {
            val promptTransition = rememberInfiniteTransition(label = "start_prompt")
            val promptOffset by promptTransition.animateFloat(
                initialValue = 0f,
                targetValue = -10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "start_prompt_offset"
            )
            val promptAlpha by promptTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "start_prompt_alpha"
            )

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

                Text(
                    text = stringResource(R.string.tap_to_continue),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 56.dp)
                        .offset(y = promptOffset.dp)
                        .graphicsLayer { alpha = promptAlpha }
                        .background(
                            color = Color.Black.copy(alpha = 0.62f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 22.dp, vertical = 12.dp)
                )
            }
        }
    }
}

private const val MAX_FLOATING_TEXTS = 40
