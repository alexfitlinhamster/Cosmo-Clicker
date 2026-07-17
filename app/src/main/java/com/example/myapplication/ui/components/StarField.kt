package com.example.myapplication.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ScavengeTarget
import com.example.myapplication.ui.GameConstants
import kotlin.random.Random

@Composable
fun Star() {
    val x = remember { Random.nextFloat() }
    val y = remember { Random.nextFloat() }
    val size = remember { Random.nextFloat() * 2 + 1 }
    val alpha by rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000 + Random.nextInt(2000)), RepeatMode.Reverse),
        label = ""
    )
    Box(
        modifier = Modifier
            .offset(
                x = (x * LocalConfiguration.current.screenWidthDp).dp,
                y = (y * LocalConfiguration.current.screenHeightDp).dp
            )
            .size(size.dp)
            .background(Color.White.copy(alpha = alpha), CircleShape)
    )
}

@Composable
fun DebrisTarget(target: ScavengeTarget) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    
    val targetSize = (26 + target.rarity.ordinal * 3).dp

    Box(
        modifier = Modifier
            .offset(
                x = (target.x * screenWidth).dp - 15.dp,
                y = (target.y * (screenHeight - GameConstants.GameAreaHeightOffset)).dp - 15.dp
            )
            .size(targetSize)
            .background(target.rarity.color.copy(alpha = 0.18f), CircleShape)
            .border(1.dp, target.rarity.color, CircleShape)
            .alpha(0.9f),
        contentAlignment = Alignment.Center
    ) {
        Text("\u2699", color = target.rarity.color, fontSize = 16.sp)
    }
}
