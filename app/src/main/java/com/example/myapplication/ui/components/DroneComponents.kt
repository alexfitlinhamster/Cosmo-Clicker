package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.DroneData
import com.example.myapplication.DroneState
import com.example.myapplication.FleetConfig
import com.example.myapplication.R

@Composable
fun FleetIcon(item: FleetConfig, iconSize: Dp) {
    val rarityColor = remember(item.rarity) { item.rarity.color }
    
    Box(
        modifier = Modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        if (item.spriteIndex >= 0) {
            val columns = 6
            val rows = 5
            val row = item.spriteIndex / columns
            val col = item.spriteIndex % columns

            Box(modifier = Modifier.size(iconSize)) {
                Image(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .requiredSize(iconSize * columns, iconSize * rows)
                        .graphicsLayer {
                            translationX = -this.size.width * (col.toFloat() / columns.toFloat())
                            translationY = -this.size.height * (row.toFloat() / rows.toFloat())
                        }
                        .scale(1.15f)
                )
            }
        } else {
            Image(
                painter = painterResource(id = item.iconRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun ScavengingDrone(
    drone: DroneData,
    fleetItems: Map<String, FleetConfig>,
    gameAreaWidth: Dp,
    gameAreaHeight: Dp,
    rotorPhase: Float = 0f,
    onDroneClick: (Long) -> Unit = {}
) {
    val fleetItem = fleetItems[drone.type]
    val isInfected = drone.state == DroneState.INFECTED
    
    val droneSize = remember(drone.type, fleetItem) {
        if (fleetItem != null) {
            when(fleetItem.rarity.name) {
                "LEGENDARY" -> 48.dp
                "MYTHIC" -> 42.dp
                "EPIC" -> 36.dp
                else -> 28.dp
            }
        } else 28.dp
    }

    Box(
        modifier = Modifier
            .offset(
                x = gameAreaWidth * drone.x - (droneSize / 2),
                y = gameAreaHeight * drone.y - (droneSize / 2)
            )
            .size(droneSize)
            .let { 
                if (isInfected) it
                    .background(Color.Red.copy(alpha = 0.35f), CircleShape)
                    .border(2.dp, Color.Red, CircleShape)
                    .shadow(12.dp, CircleShape, spotColor = Color.Red)
                else it
            }
            .clickable { onDroneClick(drone.id) },
        contentAlignment = Alignment.Center
    ) {
        if (fleetItem != null) {
            FleetIcon(fleetItem, droneSize)
            droneRotorColor(fleetItem.id)?.let { rotorColor ->
                DroneRotors(rotorColor = rotorColor, phase = rotorPhase, modifier = Modifier.fillMaxSize())
            }
        } else {
            Box(modifier = Modifier.size(droneSize).background(Color.Red, RoundedCornerShape(2.dp)))
        }
        
        if (drone.hasCargo) {
            Image(
                painter = painterResource(R.drawable.cargo_crate_space_v2),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(droneSize * 0.42f)
                    .align(Alignment.BottomCenter)
                    .offset(y = droneSize * 0.14f)
            )
        }

        if (isInfected) {
            Text("!", color = Color.White, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

/** Four-frame rotor overlay. The frame is supplied by one shared game clock. */
@Composable
private fun DroneRotors(rotorColor: Color, phase: Float, modifier: Modifier = Modifier) {
    val rotorFrame = (phase * 4f).toInt().coerceIn(0, 3)
    Canvas(modifier) {
        val angle = (rotorFrame and 3) * 45f
        val bladeRadius = size.minDimension * 0.13f
        val stroke = (size.minDimension * 0.045f).coerceAtLeast(1f)
        val hubs = listOf(
            androidx.compose.ui.geometry.Offset(size.width * 0.27f, size.height * 0.23f),
            androidx.compose.ui.geometry.Offset(size.width * 0.73f, size.height * 0.23f)
        )
        hubs.forEach { hub ->
            rotate(angle, hub) {
                drawLine(
                    color = rotorColor.copy(alpha = 0.92f),
                    start = hub.copy(x = hub.x - bladeRadius),
                    end = hub.copy(x = hub.x + bladeRadius),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = rotorColor.copy(alpha = 0.68f),
                    start = hub.copy(y = hub.y - bladeRadius * 0.65f),
                    end = hub.copy(y = hub.y + bladeRadius * 0.65f),
                    strokeWidth = stroke * 0.65f,
                    cap = StrokeCap.Round
                )
            }
            drawCircle(rotorColor.copy(alpha = 0.95f), radius = stroke * 0.75f, center = hub)
        }
    }
}

/** Only sprites that visibly contain propellers receive the animated overlay. */
private fun droneRotorColor(id: String): Color? = when (id) {
    "drone_1" -> Color(0xFFE9EDF2)
    "drone_3" -> Color(0xFF76B82A)
    "drone_5" -> Color(0xFFD85B16)
    "drone_7" -> Color(0xFFE0A21E)
    "drone_8" -> Color(0xFF8E36C7)
    "drone_10" -> Color(0xFFB72A22)
    "drone_14" -> Color(0xFFC89A64)
    "drone_15" -> Color(0xFF146B8F)
    "drone_17" -> Color(0xFF3E9B55)
    "drone_21" -> Color(0xFFBE2370)
    "drone_22" -> Color(0xFF258FC4)
    "drone_27" -> Color(0xFF3B2469)
    "drone_28" -> Color(0xFFE5B64D)
    else -> null
}
