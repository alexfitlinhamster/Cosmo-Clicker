package com.example.myapplication.ui.components

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
