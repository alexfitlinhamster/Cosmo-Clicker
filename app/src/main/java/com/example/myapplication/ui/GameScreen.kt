package com.example.myapplication.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.FloatingTextData
import com.example.myapplication.GameEventType
import com.example.myapplication.GameViewModel
import com.example.myapplication.R
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    selectedLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.gameState.collectAsState()
    val scope = rememberCoroutineScope()
    var floatingTexts by remember { mutableStateOf(listOf<FloatingTextData>()) }
    var isShopCollapsed by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }

    val fleetMap = remember(viewModel.fleetItems) {
        viewModel.fleetItems.associateBy { it.id }
    }

    // Логика выбора фона в зависимости от активного ивента
    val backgroundRes = remember<Int>(state.activeEvent?.type) {
        when (state.activeEvent?.type) {
            GameEventType.STORM -> R.drawable.fonkosmo2
            GameEventType.ASTEROID -> R.drawable.fonkosmo3
            GameEventType.PIRATES -> R.drawable.fonkosmo4
            else -> R.drawable.fonkosmo
        }
    }

    fun addFloatingText(text: String, x: Float, y: Float) {
        val id = System.currentTimeMillis()
        floatingTexts = floatingTexts + FloatingTextData(id, text, x, y)
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

        state.scavengeTargets.forEach { target ->
            key(target.id) {
                DebrisTarget(target)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                state = state,
                dps = viewModel.calculateDPS(),
                onSettingsClick = { showSettings = true }
            )
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                state.activeEvent?.let { event ->
                    EventBanner(event)
                }

                PlanetButton(
                    planetId = state.currentPlanetId,
                    planetConfig = viewModel.planets[state.currentPlanetId] ?: viewModel.planets.values.first(),
                    modifier = Modifier.align(Alignment.Center)
                ) { x, y ->
                    val value = viewModel.onPlanetClick()
                    addFloatingText("+${formatNum(value)}", x, y)
                }

                state.drones.forEach { drone ->
                    key(drone.id) {
                        ScavengingDrone(drone, fleetMap)
                    }
                }

                state.activeEvent?.let { event ->
                    when (event.type) {
                        GameEventType.ASTEROID -> {
                            Asteroid(event) { viewModel.onAsteroidClick() }
                        }
                        GameEventType.PIRATES -> {
                            PirateTarget(event, state.pirateTapsLeft) { viewModel.onPirateClick() }
                        }
                        else -> {}
                    }
                }

                floatingTexts.forEach { data ->
                    key(data.id) {
                        FloatingText(data)
                    }
                }
            }

            ShopBar(
                viewModel = viewModel,
                state = state,
                isCollapsed = isShopCollapsed,
                onToggleCollapse = { isShopCollapsed = !isShopCollapsed }
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
    }
}
