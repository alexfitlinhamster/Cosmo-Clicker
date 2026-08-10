package com.example.myapplication.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.example.myapplication.FleetConfig
import com.example.myapplication.CaseType
import com.example.myapplication.GameResourceRegistry
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import kotlinx.coroutines.delay

@Composable
fun CaseOpeningOverlay(
    isOpening: Boolean,
    caseType: CaseType,
    lastDroppedDrone: FleetConfig?,
    remainingCases: Int = 1,
    bundleRewards: List<Pair<FleetConfig, Int>> = emptyList(),
    showBundleSummary: Boolean = false,
    onFinishOpening: () -> Unit,
    onOpenAll: () -> Unit,
    onClearReward: () -> Unit,
    onClearBundleSummary: () -> Unit,
    reduceMotion: Boolean = false
) {
    var hasClickedToOpen by remember { mutableStateOf(false) }
    var currentFrame by remember { mutableIntStateOf(1) }
    var displayedCaseType by remember { mutableStateOf(caseType) }

    // Сброс состояния при закрытии
    LaunchedEffect(isOpening) {
        if (isOpening) {
            displayedCaseType = caseType
        } else {
            hasClickedToOpen = false
            currentFrame = 1
        }
    }

    // Анимация открытия запускается ТОЛЬКО после клика
    LaunchedEffect(hasClickedToOpen) {
        if (hasClickedToOpen) {
            if (reduceMotion) {
                currentFrame = 8
                delay(100)
                onFinishOpening()
                return@LaunchedEffect
            }
            // Проигрываем анимацию 1..8 один раз
            for (frame in 1..8) {
                currentFrame = frame
                delay(CASE_FRAME_DURATION_MS)
            }
            delay(CASE_REVEAL_HOLD_MS)
            onFinishOpening()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -20f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = ""
    )
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = ""
    )
    val openingShake by infiniteTransition.animateFloat(
        initialValue = -9f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(
            animation = tween(75, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "caseOpeningShake"
    )
    Box(modifier = Modifier.fillMaxSize()) {
        // ЭКРАН КЕЙСА (ОЖИДАНИЕ ИЛИ АНИМАЦИЯ)
        AnimatedVisibility(visible = isOpening, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!hasClickedToOpen) {
                        Text(
                            stringResource(R.string.case_tap_to_open),
                            color = AppColors.Primary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.graphicsLayer { alpha = textAlpha }
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                    }

                    Box(
                        modifier = Modifier
                            .offset(y = if (!hasClickedToOpen) bounceOffset.dp else 0.dp)
                            .graphicsLayer {
                                translationX = if (hasClickedToOpen && !reduceMotion) openingShake else 0f
                            }
                            .size(250.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!hasClickedToOpen) hasClickedToOpen = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = GameResourceRegistry.caseFrame(displayedCaseType, currentFrame)),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(18.dp)
                        )
                        if (remainingCases > 1) {
                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                                shape = CircleShape,
                                color = Color(0xFF10284A),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.75f)),
                                shadowElevation = 10.dp
                            ) {
                                Text(
                                    text = "×$remainingCases",
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                                    color = AppColors.Primary,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                    if (!hasClickedToOpen && remainingCases > 1) {
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = onOpenAll,
                            modifier = Modifier.fillMaxWidth(0.62f).heightIn(min = 46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = Color.Black),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(stringResource(R.string.open_all_cases, remainingCases), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // ЭКРАН НАГРАДЫ (ПОСЛЕ АНИМАЦИИ)
        AnimatedVisibility(
            visible = lastDroppedDrone != null,
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center
            ) {
                lastDroppedDrone?.let { drone ->
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            rarityLabel(drone.rarity),
                            color = drone.rarity.color,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            stringResource(R.string.unlocked),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Box(contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(200.dp).shadow(60.dp, CircleShape, ambientColor = drone.rarity.color, spotColor = drone.rarity.color))
                            FleetIcon(item = drone, iconSize = 180.dp)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            drone.name,
                            modifier = Modifier.fillMaxWidth(),
                            color = drone.rarity.color,
                            fontSize = 30.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(36.dp))
                        Button(
                            onClick = { onClearReward() },
                            colors = ButtonDefaults.buttonColors(containerColor = drone.rarity.color),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Text(stringResource(R.string.collect), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = showBundleSummary, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center
            ) {
                Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.case_results_title), color = AppColors.Primary, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.case_results_total, bundleRewards.sumOf { it.second }), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(Modifier.height(18.dp))
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(bundleRewards, key = { _, reward -> reward.first.id }) { index, (drone, count) ->
                            var visible by remember(drone.id, showBundleSummary) { mutableStateOf(false) }
                            LaunchedEffect(showBundleSummary) {
                                if (showBundleSummary) {
                                    if (!reduceMotion) delay(index * 180L)
                                    visible = true
                                }
                            }
                            AnimatedVisibility(visible, enter = fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 3 }) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF0D1B31),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, drone.rarity.color.copy(alpha = 0.5f))
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        FleetIcon(item = drone, iconSize = 58.dp)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(drone.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text(rarityLabel(drone.rarity), color = drone.rarity.color, fontSize = 10.sp)
                                        }
                                        Text("×$count", color = drone.rarity.color, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onClearBundleSummary, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.collect), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private const val CASE_FRAME_DURATION_MS = 280L
private const val CASE_REVEAL_HOLD_MS = 450L
