package com.example.myapplication.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.FleetConfig
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import kotlinx.coroutines.delay

@Composable
fun CaseOpeningOverlay(
    isOpening: Boolean,
    lastDroppedDrone: FleetConfig?,
    onFinishOpening: () -> Unit,
    onClearReward: () -> Unit
) {
    var hasClickedToOpen by remember { mutableStateOf(false) }
    var currentFrame by remember { mutableStateOf(1) }
    val context = LocalContext.current

    // Сброс состояния при закрытии
    LaunchedEffect(isOpening) {
        if (!isOpening) {
            hasClickedToOpen = false
            currentFrame = 1
        }
    }

    // Анимация открытия запускается ТОЛЬКО после клика
    LaunchedEffect(hasClickedToOpen) {
        if (hasClickedToOpen) {
            // Проигрываем анимацию 1..8 один раз
            for (frame in 1..8) {
                currentFrame = frame
                delay(200) // Задержка 200мс для торжественности
            }
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

    Box(modifier = Modifier.fillMaxSize()) {
        // ЭКРАН КЕЙСА (ОЖИДАНИЕ ИЛИ АНИМАЦИЯ)
        AnimatedVisibility(visible = isOpening, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)),
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
                            .size(250.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!hasClickedToOpen) hasClickedToOpen = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val resId = context.resources.getIdentifier("keis$currentFrame", "drawable", context.packageName)
                        if (resId != 0) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
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
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable { onClearReward() },
                contentAlignment = Alignment.Center
            ) {
                lastDroppedDrone?.let { drone ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        Text(drone.name, color = drone.rarity.color, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(64.dp))
                        Button(
                            onClick = { onClearReward() },
                            colors = ButtonDefaults.buttonColors(containerColor = drone.rarity.color)
                        ) {
                            Text(stringResource(R.string.collect), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
