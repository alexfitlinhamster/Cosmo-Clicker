package com.example.myapplication.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ScavengeTarget
import com.example.myapplication.R
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
    
    val targetSize = if (target.isMeteor) 40.dp else (26 + target.rarity.ordinal * 3).dp

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
        if (target.isMeteor) {
            Text("☄", color = Color(0xFFFF8A00), fontSize = 30.sp)
        } else {
            Image(
                painter = painterResource(debrisDrawable(target.imageIndex)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(targetSize - 4.dp)
                    .rotate(if (target.isFalling) 35f else (target.id % 360).toFloat())
            )
        }
    }
}

private fun debrisDrawable(index: Int): Int = when (index) {
    1 -> R.drawable.musor1
    2 -> R.drawable.musor2
    3 -> R.drawable.musor3
    4 -> R.drawable.musor4
    5 -> R.drawable.musor5
    else -> R.drawable.musor6
}
