package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.DroneData
import com.example.myapplication.FleetConfig
import com.example.myapplication.ui.GameConstants
import com.example.myapplication.ui.theme.AppColors

@Composable
fun FleetIcon(item: FleetConfig, iconSize: Dp) {
    Box(
        modifier = Modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect for all drones
        Box(
            modifier = Modifier
                .size(iconSize * 0.8f)
                .shadow(10.dp, CircleShape, ambientColor = AppColors.Primary, spotColor = AppColors.Primary)
        )

        if (item.spriteIndex >= 0) {
            val columns = 6
            val rows = 5
            val row = item.spriteIndex / columns
            val col = item.spriteIndex % columns
            val zoom = 1.3f
            
            Box(modifier = Modifier.size(iconSize)) {
                Image(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .requiredSize(iconSize * columns * zoom, iconSize * rows * zoom)
                        .offset(
                            x = -iconSize * col * zoom - (iconSize * (zoom - 1f) / 2f),
                            y = -iconSize * row * zoom - (iconSize * (zoom - 1f) / 2f)
                        )
                )
            }
        } else {
            // For individual drone files
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
fun ScavengingDrone(drone: DroneData, fleetItems: Map<String, FleetConfig>) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    
    val droneSize = remember(drone.type) {
        when {
            drone.type.startsWith("drone_") -> {
                val num = drone.type.removePrefix("drone_").toIntOrNull() ?: 0
                if (num > 20) 48.dp else if (num > 10) 38.dp else 28.dp
            }
            else -> 28.dp
        }
    }

    val fleetItem = fleetItems[drone.type]

    Box(
        modifier = Modifier
            .offset(
                x = (drone.x * screenWidth).dp - (droneSize / 2),
                y = (drone.y * (screenHeight - GameConstants.GameAreaHeightOffset)).dp - (droneSize / 2)
            )
            .size(droneSize),
        contentAlignment = Alignment.Center
    ) {
        if (fleetItem != null) {
            FleetIcon(fleetItem, droneSize)
        } else {
            // Placeholder if not found
            Box(modifier = Modifier.size(droneSize).background(Color.Red, RoundedCornerShape(2.dp)))
        }
        
        if (drone.hasCargo) {
            Box(
                modifier = Modifier
                    .size(droneSize / 3)
                    .background(AppColors.CargoColor, RoundedCornerShape(1.dp)) 
                    .align(Alignment.BottomCenter)
                    .offset(y = droneSize / 10)
            )
        }
    }
}
