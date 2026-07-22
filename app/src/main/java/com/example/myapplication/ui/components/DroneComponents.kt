package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.DroneData
import com.example.myapplication.DroneState
import com.example.myapplication.FleetConfig
import com.example.myapplication.ui.GameConstants
import com.example.myapplication.ui.theme.AppColors

@Composable
fun FleetIcon(item: FleetConfig, iconSize: Dp) {
    val rarityColor = remember(item.rarity) { item.rarity.color }
    
    Box(
        modifier = Modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(iconSize * 0.8f)
                .shadow(12.dp, CircleShape, ambientColor = rarityColor, spotColor = rarityColor)
        )

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
    onDroneClick: (Long) -> Unit = {}
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    
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
                x = (drone.x * screenWidth).dp - (droneSize / 2),
                y = (drone.y * (screenHeight - GameConstants.GameAreaHeightOffset)).dp - (droneSize / 2)
            )
            .size(droneSize)
            .let { 
                if (isInfected) it.background(Color.Red.copy(alpha = 0.3f), CircleShape)
                else it
            }
            .clickable { onDroneClick(drone.id) },
        contentAlignment = Alignment.Center
    ) {
        if (fleetItem != null) {
            FleetIcon(fleetItem, droneSize)
        } else {
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

        if (isInfected) {
            Text("👾", fontSize = 12.sp, modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}
