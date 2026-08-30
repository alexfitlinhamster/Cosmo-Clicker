package com.example.myapplication.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.SpaceDesign
import com.example.myapplication.utils.formatNum
import kotlinx.coroutines.delay

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
    var previousDebris by remember { mutableDoubleStateOf(state.totalDebris) }
    var currencyPulse by remember { mutableStateOf(false) }
    LaunchedEffect(state.totalDebris) {
        if (state.totalDebris > previousDebris) {
            currencyPulse = true
            delay(150L)
            currencyPulse = false
        }
        previousDebris = state.totalDebris
    }
    val currencyScale by animateFloatAsState(
        targetValue = if (currencyPulse) 1.18f else 1f,
        animationSpec = spring(dampingRatio = .48f, stiffness = 520f),
        label = "currencyPulse"
    )
    Column(
        modifier = Modifier
            .padding(top = 44.dp, start = 12.dp, end = 12.dp, bottom = 10.dp)
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xA80A1322), Color.Transparent)))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HeaderAction(
                icon = R.drawable.ic_achievement_medal,
                description = R.string.open_achievements,
                onClick = onAchievementsClick
            )
            Spacer(Modifier.width(6.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(Color.Black.copy(alpha = .22f), RoundedCornerShape(SpaceDesign.ControlRadius))
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.ic_currency_debris_v2),
                    null,
                    Modifier.size(21.dp).scale(currencyScale),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatNum(state.totalDebris),
                    modifier = Modifier.weight(1f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AppColors.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = stringResource(R.string.debris), color = AppColors.TextMuted, fontSize = 9.sp, maxLines = 1)
            }
            Spacer(Modifier.width(6.dp))
            HeaderAction(
                icon = R.drawable.ic_nav_settings_minimal,
                description = R.string.settings,
                onClick = onSettingsClick
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 3.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_drone_energy_cell_v2), null, Modifier.size(25.dp), tint = Color.Unspecified)
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(stringResource(R.string.drone_income), color = AppColors.TextMuted, fontSize = 9.sp, lineHeight = 10.sp)
                    Text(stringResource(R.string.per_second, formatNum(dps)), color = AppColors.Secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .clickable(onClick = onPrestigeShopClick)
                    .padding(top = 1.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(R.drawable.ic_nav_prestige_minimal),
                        stringResource(R.string.open_prestige_shop),
                        Modifier.size(26.dp),
                        tint = Color.Unspecified
                    )
                    if (state.prestigePoints > 0) {
                        Text(
                            text = state.prestigePoints.toString(),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(AppColors.SurfaceRaised, CircleShape)
                                .padding(horizontal = 4.dp),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                Text(
                    stringResource(R.string.prestige),
                    color = AppColors.TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        if (nextPlanetIndex != null && nextPlanetPrice != null) {
            val requiredDrone = EconomyBalance.requiredDroneIdForPlanet(nextPlanetIndex)
            val requiredFleet = EconomyBalance.requiredActiveDronesForPlanet(nextPlanetIndex)
            val activeFleet = state.activeFleetCounts.values.sum()
            val needsDrone = requiredDrone != null && requiredDrone !in state.discoveredDroneIds
            val needsFleet = !needsDrone && activeFleet < requiredFleet
            val progress = when {
                needsDrone -> 0f
                needsFleet -> (activeFleet.toFloat() / requiredFleet.coerceAtLeast(1)).coerceIn(0f, 1f)
                else -> EconomyBalance.planetUnlockProgress(state.totalDebris, nextPlanetPrice)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .background(AppColors.Surface, RoundedCornerShape(SpaceDesign.ControlRadius))
                    .clickable(onClick = onRouteClick)
                    .padding(horizontal = 9.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(Modifier.size(32.dp), shape = CircleShape, color = AppColors.SurfaceRaised) {
                    Icon(painterResource(R.drawable.ic_goal_route_minimal), null, Modifier.padding(6.dp), tint = Color.Unspecified)
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(R.string.goal_step_to_planet, nextPlanetIndex),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.goal_open_route),
                        color = AppColors.Primary,
                        fontSize = 9.sp
                    )
                }
                Text(
                    when {
                        needsDrone -> stringResource(R.string.goal_action_open_case)
                        needsFleet -> stringResource(R.string.goal_action_deploy, requiredFleet, activeFleet)
                        else -> stringResource(R.string.goal_action_save, formatNum((nextPlanetPrice - state.totalDebris).coerceAtLeast(0.0)))
                    },
                    color = Color.White.copy(alpha = .82f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                    color = AppColors.Primary,
                    trackColor = Color.White.copy(alpha = .10f)
                )
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

@Composable
private fun HeaderAction(icon: Int, description: Int, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(Color.Black.copy(alpha = .18f), CircleShape)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
            modifier = Modifier.size(30.dp),
            tint = Color.Unspecified
        )
    }
}
