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
import com.example.myapplication.EconomyBalance
import com.example.myapplication.GameResourceRegistry
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum

@Composable
fun Header(
    state: GameState,
    dps: Double,
    nextPlanetIndex: Int?,
    nextPlanetPrice: Double?,
    nextPlanetImageRes: Int?,
    onAchievementsClick: () -> Unit,
    onPrestigeShopClick: () -> Unit,
    onRouteClick: () -> Unit,
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

        if (nextPlanetIndex != null && nextPlanetPrice != null) {
            val progress = EconomyBalance.planetUnlockProgress(state.totalDebris, nextPlanetPrice)
            val requiredDrone = EconomyBalance.requiredDroneIdForPlanet(nextPlanetIndex)
            val requiredPrestige = EconomyBalance.requiredPrestigeForPlanet(nextPlanetIndex)
            val goalIcon = when {
                requiredDrone != null && requiredDrone !in state.discoveredDroneIds ->
                    GameResourceRegistry.drone(requiredDrone.removePrefix("drone_").toIntOrNull() ?: 1)
                state.lifetimeStats.prestiges < requiredPrestige -> R.drawable.ic_prestige_core
                else -> nextPlanetImageRes
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .background(Color(0xFF10182A).copy(alpha = .88f), RoundedCornerShape(10.dp))
                    .border(1.dp, AppColors.Primary.copy(alpha = .28f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onRouteClick)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(48.dp).background(AppColors.Primary.copy(.13f), CircleShape), contentAlignment = Alignment.Center) {
                    if (goalIcon != null) Image(painterResource(goalIcon), null, Modifier.size(42.dp), contentScale = ContentScale.Fit)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(R.string.next_goal_planet, nextPlanetIndex, EconomyBalance.MAX_PLANET_INDEX),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(
                            R.string.next_goal_remaining,
                            formatNum((nextPlanetPrice - state.totalDebris).coerceAtLeast(0.0))
                        ),
                        color = AppColors.Secondary,
                        fontSize = 10.sp
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    color = AppColors.Primary,
                    trackColor = Color.White.copy(alpha = .10f)
                )
                val requiredFleet = EconomyBalance.requiredActiveDronesForPlanet(nextPlanetIndex)
                if (requiredDrone != null && requiredDrone !in state.discoveredDroneIds) {
                    Text(
                        stringResource(R.string.next_goal_drone, requiredDrone.removePrefix("drone_")),
                        color = AppColors.Warning,
                        fontSize = 10.sp
                    )
                } else if (state.lifetimeStats.prestiges < requiredPrestige) {
                    Text(
                        stringResource(R.string.next_goal_prestige, state.lifetimeStats.prestiges, requiredPrestige),
                        color = AppColors.Warning,
                        fontSize = 10.sp
                    )
                } else if (requiredFleet > 0) {
                    Text(
                        stringResource(R.string.next_goal_fleet, state.activeFleetCounts.values.sum(), requiredFleet),
                        color = if (state.activeFleetCounts.values.sum() >= requiredFleet) AppColors.Primary else AppColors.Warning,
                        fontSize = 10.sp
                    )
                }
                }
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
