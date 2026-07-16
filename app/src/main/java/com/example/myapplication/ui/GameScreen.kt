package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.FloatingTextData
import com.example.myapplication.GameEventType
import com.example.myapplication.GameViewModel
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.gameState.collectAsState()
    val scope = rememberCoroutineScope()
    var floatingTexts by remember { mutableStateOf(listOf<FloatingTextData>()) }
    var isShopCollapsed by remember { mutableStateOf(true) }

    val fleetMap = remember(viewModel.fleetItems) {
        viewModel.fleetItems.associateBy { it.id }
    }

    fun addFloatingText(text: String, x: Float, y: Float) {
        val id = System.currentTimeMillis()
        floatingTexts = floatingTexts + FloatingTextData(id, text, x, y)
        scope.launch {
            delay(GameConstants.FloatingTextDuration)
            floatingTexts = floatingTexts.filter { it.id != id }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AppColors.BackgroundStart, AppColors.BackgroundMid, AppColors.BackgroundStart)
                )
            )
    ) {
        repeat(GameConstants.StarCount) {
            Star()
        }

        state.scavengeTargets.forEach { target ->
            key(target.id) {
                DebrisTarget(target)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Header(state, viewModel.calculateDPS())
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                state.activeEvent?.let { event ->
                    EventBanner(event)
                }

                // ВЕРНУЛИ ПЛАНЕТУ В ЦЕНТР
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

        // ОВЕРЛЕЙ ОТКРЫТИЯ КЕЙСА (ПОЯВЛЯЕТСЯ ПОВЕРХ ВСЕГО)
        CaseOpeningOverlay(
            isOpening = state.isOpeningCase,
            lastDroppedDrone = state.lastDroppedDroneId?.let { fleetMap[it] },
            onFinishOpening = { viewModel.finishOpeningCase() },
            onClearReward = { viewModel.clearReward() }
        )
    }
}
