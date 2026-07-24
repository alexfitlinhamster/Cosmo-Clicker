package com.example.myapplication.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.GameEvent
import com.example.myapplication.GameEventType
import com.example.myapplication.R
import com.example.myapplication.ui.GameConstants
import com.example.myapplication.ui.theme.AppColors

@Composable
fun EventBanner(event: GameEvent, onClick: () -> Unit) {
    val color = when (event.type) {
        GameEventType.STORM -> AppColors.Warning
        GameEventType.BLACK_HOLE, GameEventType.CYBER_VIRUS -> AppColors.Danger
        GameEventType.SOLAR_FLARE -> Color(0xFFFF5722)
        else -> AppColors.Danger
    }
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = stringResource(
                when (event.type) {
                    GameEventType.STORM -> R.string.event_space_storm
                    GameEventType.ASTEROID -> R.string.event_gold_asteroid
                    GameEventType.METEOR_SHOWER -> R.string.event_debris_shower
                    GameEventType.BLACK_HOLE -> R.string.event_black_hole
                    GameEventType.SOLAR_FLARE -> R.string.event_solar_flare
                    GameEventType.CYBER_VIRUS -> R.string.event_cyber_virus
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
            .background(Color.Red, RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("!", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BlackHoleComponent(event: GameEvent, tapsLeft: Int, onClick: () -> Unit) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp

    Box(
        modifier = Modifier
            .offset(
                x = (event.x * (screenWidth - 150)).dp,
                y = (event.y * (screenHeight - GameConstants.GameAreaHeightOffset - 150)).dp
            )
            .size(150.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.dira),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        Text("$tapsLeft", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

@Composable
fun EventInfoDialog(event: GameEvent, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    when (event.type) {
                        GameEventType.STORM -> R.string.event_space_storm
                        GameEventType.ASTEROID -> R.string.event_gold_asteroid
                        GameEventType.METEOR_SHOWER -> R.string.event_debris_shower
                        GameEventType.BLACK_HOLE -> R.string.event_black_hole
                        GameEventType.SOLAR_FLARE -> R.string.event_solar_flare
                        GameEventType.CYBER_VIRUS -> R.string.event_cyber_virus
                    }
                ),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(
                    when (event.type) {
                        GameEventType.STORM -> R.string.event_desc_storm
                        GameEventType.ASTEROID -> R.string.event_desc_asteroid
                        GameEventType.METEOR_SHOWER -> R.string.event_desc_debris_shower
                        GameEventType.BLACK_HOLE -> R.string.event_desc_black_hole
                        GameEventType.SOLAR_FLARE -> R.string.event_desc_solar_flare
                        GameEventType.CYBER_VIRUS -> R.string.event_desc_cyber_virus
                    }
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun TextButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        content()
    }
}
