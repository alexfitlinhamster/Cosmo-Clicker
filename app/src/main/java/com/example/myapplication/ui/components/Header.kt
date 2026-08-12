package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.GameState
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum

@Composable
fun Header(
    state: GameState,
    dps: Double,
    onAchievementsClick: () -> Unit,
    onPrestigeShopClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(R.drawable.ui_button_statistics_v2),
                contentDescription = stringResource(R.string.open_statistics),
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(9.dp)).clickable(onClick = onAchievementsClick),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(10.dp))
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatNum(state.totalDebris),
                    modifier = Modifier.alignByBaseline(),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AppColors.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.debris),
                    modifier = Modifier.alignByBaseline(),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            Image(
                painter = painterResource(R.drawable.ui_button_settings_v3),
                contentDescription = stringResource(R.string.settings),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { onSettingsClick() },
                contentScale = ContentScale.Fit
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.per_second, formatNum(dps)),
                modifier = Modifier.weight(1f),
                color = AppColors.Secondary,
                fontSize = 14.sp
            )
            Row(
                modifier = Modifier
                    .width(72.dp)
                    .height(36.dp)
                    .background(Color(0xFF10182A).copy(alpha = .92f), RoundedCornerShape(10.dp))
                    .border(1.dp, AppColors.Warning.copy(alpha = .65f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onPrestigeShopClick)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painterResource(R.drawable.ic_prestige_core), stringResource(R.string.open_prestige_shop), Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = state.prestigePoints.toString(),
                    modifier = Modifier.weight(1f),
                    color = AppColors.Warning,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (state.isHotelDebtActive) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.hotel_debt), color = AppColors.Danger, fontSize = 12.sp)
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
