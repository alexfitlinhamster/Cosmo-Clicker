package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.myapplication.ScavengeTarget
import com.example.myapplication.R
import kotlin.random.Random
import kotlin.math.sin

@Composable
fun Star(index: Int, twinklePhase: Float, reduceMotion: Boolean = false) {
    val x = remember { Random.nextFloat() }
    val y = remember { Random.nextFloat() }
    val size = remember { Random.nextFloat() * 2 + 1 }
    // All stars share one animation clock from GameScreen instead of creating
    // a separate infinite transition for every star.
    val twinkle = ((sin(twinklePhase + index * 1.73f) + 1f) * .5f)
    val alpha = if (reduceMotion) 0.55f else 0.2f + twinkle * 0.8f
    val windowSize = LocalWindowInfo.current.containerSize
    Box(
        modifier = Modifier
            .offset { IntOffset((x * windowSize.width).toInt(), (y * windowSize.height).toInt()) }
            .size(size.dp)
            .background(Color.White.copy(alpha = alpha), CircleShape)
    )
}

@Composable
fun DebrisTarget(target: ScavengeTarget, gameAreaWidth: Dp, gameAreaHeight: Dp, onClick: (() -> Unit)? = null) {
    val targetSize = if (target.isMeteor) 38.dp else (30 + target.rarity.ordinal * 2).dp

    Box(
        modifier = Modifier
            .offset(
                x = gameAreaWidth * target.x - (targetSize / 2),
                y = gameAreaHeight * target.y - (targetSize / 2)
            )
            .size(targetSize)
            .alpha(0.88f)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (target.isMeteor) {
            Image(
                painter = painterResource(R.drawable.event_meteor_minimal_v2),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(targetSize - 2.dp)
                    .rotate(35f)
            )
        } else {
            Image(
                painter = painterResource(debrisDrawable(target.imageIndex)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(targetSize - 4.dp)
                    .rotate(if (target.isFalling) 25f else ((target.id % 8) * 45).toFloat())
            )
        }
    }
}

internal fun debrisDrawable(index: Int): Int = when (index) {
    1 -> R.drawable.debris_01
    2 -> R.drawable.debris_02
    3 -> R.drawable.debris_03
    4 -> R.drawable.debris_04
    5 -> R.drawable.debris_05
    6 -> R.drawable.debris_06
    7 -> R.drawable.debris_07
    8 -> R.drawable.debris_08
    9 -> R.drawable.debris_09
    10 -> R.drawable.debris_10
    11 -> R.drawable.debris_11
    12 -> R.drawable.debris_12
    13 -> R.drawable.debris_13
    14 -> R.drawable.debris_14
    else -> R.drawable.debris_01
}
