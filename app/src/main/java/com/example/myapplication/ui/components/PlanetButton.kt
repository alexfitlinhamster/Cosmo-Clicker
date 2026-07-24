package com.example.myapplication.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.myapplication.PlanetConfig
import com.example.myapplication.ui.GameConstants
import kotlinx.coroutines.delay

@Composable
fun PlanetButton(planetId: String, planetConfig: PlanetConfig, modifier: Modifier, onClick: (Float, Float) -> Unit) {
    var scaleVal by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(targetValue = scaleVal, label = "")
    val isLocked = planetConfig.price < 0
    val containerSize = GameConstants.PlanetSize

    Box(
        modifier = modifier
            .size(containerSize)
            .scale(animatedScale)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (!isLocked) {
                    scaleVal = 0.92f
                    onClick(0.5f, 0.5f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        LaunchedEffect(scaleVal) { if (scaleVal < 1f) { delay(80); scaleVal = 1f } }

        // Обрезка
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (planetConfig.spriteIndex >= 0) {
                val columns = 4
                val rows = 3
                val row = planetConfig.spriteIndex / columns
                val col = planetConfig.spriteIndex % columns
                
                Image(
                    painter = painterResource(id = planetConfig.imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .requiredSize(containerSize * columns, containerSize * rows)
                        .offset(x = -containerSize * col, y = -containerSize * row)
                )
            } else {
                Image(
                    painter = painterResource(id = planetConfig.imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .let { if (isLocked) it.alpha(0.5f) else it }
                )
            }
        }
    }
}
