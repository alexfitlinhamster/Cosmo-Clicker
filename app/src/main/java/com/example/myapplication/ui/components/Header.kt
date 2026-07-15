package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.GameState
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum

@Composable
fun Header(state: GameState, dps: Double) {
    Column(
        modifier = Modifier
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatNum(state.totalDebris),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "debris", color = Color.Gray, fontSize = 14.sp)
        }
        Text(
            text = "+${formatNum(dps)} / sec",
            color = AppColors.Secondary,
            fontSize = 14.sp
        )

        if (state.isHotelDebtActive) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Hotel Debt", color = AppColors.Danger, fontSize = 12.sp)
                    Text(formatNum(state.currentHotelDebt), color = AppColors.Danger, fontSize = 12.sp)
                }
                LinearProgressIndicator(
                    progress = { (state.currentHotelDebt / 1000000.0).toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = AppColors.Danger,
                    trackColor = Color.White.copy(alpha = 0.1f),
                )
            }
        }
    }
}
