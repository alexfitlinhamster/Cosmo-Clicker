package com.example.myapplication.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.FloatingTextData
import com.example.myapplication.ui.GameConstants

@Composable
fun FloatingText(data: FloatingTextData) {
    val alpha by animateFloatAsState(
        targetValue = 0f, 
        animationSpec = tween(GameConstants.FloatingTextDuration.toInt()), 
        label = ""
    )
    val offset by animateIntOffsetAsState(
        targetValue = IntOffset(0, -100),
        animationSpec = tween(GameConstants.FloatingTextDuration.toInt()),
        label = ""
    )
    
    Box(
        modifier = Modifier
            .offset { offset }
            .alpha(alpha)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = data.text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.shadow(4.dp, CircleShape)
        )
    }
}
