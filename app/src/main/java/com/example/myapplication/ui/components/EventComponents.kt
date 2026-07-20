package com.example.myapplication.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.GameEvent
import com.example.myapplication.GameEventType
import com.example.myapplication.R
import com.example.myapplication.ui.GameConstants
import com.example.myapplication.ui.theme.AppColors
import kotlin.random.Random

@Composable
fun EventBanner(event: GameEvent) {
    val color = if (event.type == GameEventType.STORM) AppColors.Warning else AppColors.Danger
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = stringResource(
                when (event.type) {
                    GameEventType.STORM -> R.string.event_space_storm
                    GameEventType.ASTEROID -> R.string.event_gold_asteroid
                    GameEventType.PIRATES -> R.string.event_pirates
                    GameEventType.METEOR_SHOWER -> R.string.event_debris_shower
                }
            ),
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun DebrisShower(event: GameEvent) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp - GameConstants.GameAreaHeightOffset
    val pieces = remember(event.expiresAt) {
        List(DEBRIS_SHOWER_PIECES) { index ->
            val random = Random(event.expiresAt + index)
            DebrisShowerPiece(
                drawableIndex = index % DEBRIS_DRAWABLE_COUNT + 1,
                startX = random.nextFloat(),
                size = random.nextInt(24, 49),
                durationMs = random.nextInt(1_800, 3_601),
                initialOffsetMs = random.nextInt(0, 3_600),
                rotation = random.nextInt(-180, 181).toFloat()
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        pieces.forEach { piece ->
            val transition = rememberInfiniteTransition(label = "debris-shower")
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(piece.durationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(piece.initialOffsetMs, StartOffsetType.FastForward)
                ),
                label = "debris-flight"
            )
            Image(
                painter = painterResource(debrisDrawable(piece.drawableIndex)),
                contentDescription = null,
                modifier = Modifier
                    .offset(
                        x = (piece.startX * screenWidth + progress * 80f - 40f).dp,
                        y = (-piece.size + progress * (screenHeight + piece.size * 2)).dp
                    )
                    .size(piece.size.dp)
                    .rotate(piece.rotation + progress * 360f)
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

private data class DebrisShowerPiece(
    val drawableIndex: Int,
    val startX: Float,
    val size: Int,
    val durationMs: Int,
    val initialOffsetMs: Int,
    val rotation: Float
)

private const val DEBRIS_SHOWER_PIECES = 12
private const val DEBRIS_DRAWABLE_COUNT = 6

@Composable
fun Asteroid(event: GameEvent, onClick: () -> Unit) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    
    Box(
        modifier = Modifier
            .offset(
                x = (event.x * (screenWidth - 60)).dp,
                y = (event.y * (screenHeight - GameConstants.GameAreaHeightOffset)).dp
            )
            .size(50.dp)
            .shadow(10.dp, RoundedCornerShape(4.dp), spotColor = Color.Red)
            .background(Color.Red, RoundedCornerShape(4.dp)) // Red cube placeholder as requested
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("!", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PirateTarget(event: GameEvent, tapsLeft: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .padding(top = 100.dp)
                .size(80.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .shadow(10.dp, CircleShape, spotColor = Color.Red)
                .border(2.dp, Color.Red, CircleShape)
                .clickable { onClick() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🏴‍☠️", fontSize = 32.sp)
            Text("${5 - tapsLeft}/5", color = Color.White, fontSize = 10.sp)
        }
    }
}
