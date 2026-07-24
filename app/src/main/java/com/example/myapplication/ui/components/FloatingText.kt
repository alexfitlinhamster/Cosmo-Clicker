package com.example.myapplication.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.FloatingTextData
import com.example.myapplication.ui.GameConstants

@Composable
fun FloatingText(data: FloatingTextData, gameAreaWidth: Dp, gameAreaHeight: Dp) {
    var animateAway by remember(data.id) { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (animateAway) 0f else 1f,
        animationSpec = tween(GameConstants.FloatingTextDuration.toInt()),
        label = "floating_text_alpha"
    )
    val rise by animateFloatAsState(
        targetValue = if (animateAway) -70f else 0f,
        animationSpec = tween(GameConstants.FloatingTextDuration.toInt()),
        label = "floating_text_rise"
    )

    LaunchedEffect(data.id) {
        animateAway = true
    }

    Text(
        text = data.text,
        color = data.color,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        modifier = Modifier
            .offset(
                x = gameAreaWidth * data.x,
                y = gameAreaHeight * data.y + rise.dp
            )
            .graphicsLayer { translationX = -size.width / 2f }
            .alpha(alpha)
            .shadow(4.dp, CircleShape)
    )
}
