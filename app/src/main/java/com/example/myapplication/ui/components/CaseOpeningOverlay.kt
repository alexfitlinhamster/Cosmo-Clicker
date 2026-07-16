package com.example.myapplication.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.FleetConfig
import com.example.myapplication.ui.theme.AppColors
import kotlinx.coroutines.delay

@Composable
fun CaseOpeningOverlay(
    isOpening: Boolean,
    lastDroppedDrone: FleetConfig?,
    onFinishOpening: () -> Unit,
    onClearReward: () -> Unit
) {
    var currentFrame by remember { mutableStateOf(1) }
    val context = LocalContext.current

    // Анимация прокрутки кейса
    LaunchedEffect(isOpening) {
        if (isOpening) {
            repeat(4) { 
                for (frame in 1..8) {
                    currentFrame = frame
                    delay(80)
                }
            }
            onFinishOpening()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ЭКРАН АНИМАЦИИ ОТКРЫТИЯ
        AnimatedVisibility(
            visible = isOpening,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                val resId = context.resources.getIdentifier("keis$currentFrame", "drawable", context.packageName)
                if (resId != 0) {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = null,
                        modifier = Modifier.size(250.dp)
                    )
                }
            }
        }

        // ЭКРАН НАГРАДЫ
        AnimatedVisibility(
            visible = lastDroppedDrone != null,
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { onClearReward() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "UNLOCKED!",
                        color = Color.Yellow,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    lastDroppedDrone?.let { drone ->
                        FleetIcon(item = drone, iconSize = 180.dp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            drone.name,
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(64.dp))
                    Button(
                        onClick = { onClearReward() },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                    ) {
                        Text("COLLECT", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
