package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.GameState
import com.example.myapplication.GameViewModel
import com.example.myapplication.R
import com.example.myapplication.ui.GameConstants
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.formatNum
import kotlinx.coroutines.delay
import kotlin.math.pow

@Composable
fun ShopBar(
    viewModel: GameViewModel,
    state: GameState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(R.string.tab_planets, R.string.tab_fleet, R.string.tab_click)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(GameConstants.ShopExpandedHeight),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(AppColors.WhiteAlpha20, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabs.forEachIndexed { index, title ->
                        Button(
                            onClick = { selectedTab = index },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == index) AppColors.Primary.copy(alpha = 0.15f) else Color.Transparent,
                                contentColor = if (selectedTab == index) AppColors.Primary else Color.Gray
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedTab == index) AppColors.Primary else Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(title), fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> {
                            items(viewModel.planets.toList(), key = { it.first }) { (id, config) ->
                                val active = state.currentPlanetId == id
                                val owned = state.ownedPlanets.contains(id)
                                PlanetRow(
                                    name = localizedPlanetName(id),
                                    desc = localizedPlanetDescription(id),
                                    bonus = localizedPlanetBonus(id),
                                    price = config.price.toLong(),
                                    active = active,
                                    owned = owned,
                                    canBuy = state.totalDebris >= config.price && !active,
                                    iconRes = config.imageRes,
                                    spriteIndex = config.spriteIndex,
                                    showLock = !owned
                                ) { viewModel.buyPlanet(id) }
                            }
                        }
                        1 -> {
                            item {
                                MysteryCaseRow(viewModel, state)
                            }
                            items(viewModel.fleetItems, key = { it.id }) { item ->
                                val count = state.fleetCounts[item.id] ?: 0
                                if (count > 0) {
                                    ShopRow(
                                        name = item.name, 
                                        meta = stringResource(R.string.fleet_meta, rarityLabel(item.rarity), count),
                                        cost = 0,
                                        canBuy = false,
                                        canSell = true,
                                        iconRes = item.iconRes,
                                        spriteIndex = item.spriteIndex,
                                        onBuy = { },
                                        onSell = { viewModel.sellFleet(item.id) }
                                    )
                                }
                            }
                            item {
                                Button(
                                    onClick = { viewModel.takeHotelDebt() },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    enabled = !state.isHotelDebtActive,
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger.copy(alpha = 0.1f), contentColor = AppColors.Danger),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Danger)
                                ) {
                                    Text(stringResource(if (state.isHotelDebtActive) R.string.debt_active else R.string.take_hotel_debt))
                                }
                            }
                        }
                        2 -> {
                            items(viewModel.clickItems, key = { it.id }) { item ->
                                val lvl = state.clickLevels[item.id] ?: 0
                                val cost = (item.base * 1.15.pow(lvl.toDouble())).toLong()
                                ShopRow(
                                    name = localizedUpgradeName(item.id),
                                    meta = stringResource(R.string.click_meta, formatNum(item.value), lvl),
                                    cost = cost,
                                    canBuy = state.totalDebris >= cost,
                                    canSell = false,
                                    iconRes = item.iconRes,
                                    showLock = lvl == 0,
                                    onBuy = { viewModel.buyClickUpgrade(item.id) }
                                )
                            }
                        }
                    }
            }
        }
    }
}

@Composable
fun ShopLauncherButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ui_button_shop),
        contentDescription = stringResource(R.string.open_shop),
        modifier = modifier
            .size(76.dp)
            .clickable(onClick = onClick),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun MysteryCaseRow(viewModel: GameViewModel, state: GameState) {
    val totalDrones = state.fleetCounts.values.sum()
    val caseCost = viewModel.calculateCaseCost(state.casesPurchased)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .border(1.dp, AppColors.Primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(AppColors.WhiteAlpha05, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.case_01),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.mystery_case), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.random_drone_count, totalDrones), color = Color.Gray, fontSize = 11.sp)
                Text(stringResource(R.string.case_price_growth), color = AppColors.Primary, fontSize = 9.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = { viewModel.startOpeningCase() },
            enabled = state.totalDebris >= caseCost && totalDrones < 5,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(formatNum(caseCost), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ShopRow(
    name: String, 
    meta: String, 
    cost: Long, 
    canBuy: Boolean, 
    canSell: Boolean,
    iconRes: Int, 
    spriteIndex: Int = -1, 
    showLock: Boolean = false,
    onBuy: () -> Unit,
    onSell: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).background(AppColors.WhiteAlpha05, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (spriteIndex >= 0) {
                    val columns = 6
                    val rows = 5
                    val row = spriteIndex / columns
                    val col = spriteIndex % columns
                    val iconSize = 24.dp
                    
                    Box(modifier = Modifier.size(iconSize)) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .requiredSize(iconSize * columns, iconSize * rows)
                                .offset(x = -iconSize * col, y = -iconSize * row)
                                .scale(1.2f)
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (showLock) {
                    Image(
                        painter = painterResource(R.drawable.ui_button_lock),
                        contentDescription = stringResource(R.string.locked),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                if (name.isNotEmpty()) {
                    Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Text(meta, color = Color.Gray, fontSize = 11.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canSell) {
                Button(
                    onClick = onSell,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger.copy(alpha = 0.2f), contentColor = AppColors.Danger),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp).width(60.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(stringResource(R.string.sell), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (canBuy) {
                Button(
                    onClick = onBuy,
                    enabled = canBuy,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(formatNum(cost.toDouble()), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PlanetRow(
    name: String, 
    desc: String, 
    bonus: String,
    price: Long, 
    active: Boolean, 
    owned: Boolean,
    canBuy: Boolean, 
    iconRes: Int, 
    spriteIndex: Int = -1, 
    showLock: Boolean,
    onClick: () -> Unit
) {
    val isLocked = price < 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(if (active) AppColors.Primary.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconSize = 40.dp
            Box(modifier = Modifier.size(iconSize).clip(CircleShape)) {
                if (spriteIndex >= 0) {
                    val columns = 4
                    val rows = 3
                    val row = spriteIndex / columns
                    val col = spriteIndex % columns
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .requiredSize(iconSize * columns, iconSize * rows)
                            .offset(x = -iconSize * col, y = -iconSize * row)
                            .scale(1.8f) 
                    )
                } else {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop, 
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(2.2f) // Увеличили зум до 2.2x, чтобы убрать белые скобки
                            .clip(CircleShape)
                            .let { if(isLocked) it.alpha(0.3f) else it }
                    )
                }
                if (showLock) {
                    Image(
                        painter = painterResource(R.drawable.ui_button_lock),
                        contentDescription = stringResource(R.string.locked),
                        modifier = Modifier.align(Alignment.Center).size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if(isLocked) "???" else name, color = if(isLocked) Color.Gray else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = Color.Gray, fontSize = 11.sp)
                Text(bonus, color = AppColors.Primary, fontSize = 10.sp, lineHeight = 12.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onClick,
            enabled = (canBuy || owned) && !isLocked,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (active || isLocked) Color.Gray else AppColors.Primary, 
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            val btnText = when {
                isLocked -> stringResource(R.string.locked)
                active -> stringResource(R.string.active)
                owned -> stringResource(R.string.select)
                price == 0L -> stringResource(R.string.free)
                else -> formatNum(price.toDouble())
            }
            Text(btnText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun localizedPlanetName(id: String): String = stringResource(planetNameResource(id))

internal fun planetNameResource(id: String): Int =
    when (id) {
        "p1" -> R.string.planet_azurea
        "p2" -> R.string.planet_canyon_prime
        "p3" -> R.string.planet_nebula_echo
        "p4" -> R.string.planet_crystal_hearth
        "p5" -> R.string.planet_dune_horizon
        "p6" -> R.string.planet_volt_nova
        "p7" -> R.string.planet_gas_giant
        "p8" -> R.string.planet_jungle_core
        "p9" -> R.string.planet_magma_s15
        "p10" -> R.string.planet_red_dust
        "p11" -> R.string.planet_mech_world
        "p12" -> R.string.planet_luna_silvis
        "p13" -> R.string.planet_abyss_ocean
        "p14" -> R.string.planet_ring_oasis
        "p15" -> R.string.planet_sky_haven
        "p16" -> R.string.planet_toxic_waste
        "p17" -> R.string.planet_pink_nebula
        "p18" -> R.string.planet_cloud_city
        "p19" -> R.string.planet_rocky_bastion
        "p20" -> R.string.planet_foggy_void
        else -> R.string.unknown_item
    }

@Composable
private fun localizedUpgradeName(id: String): String = stringResource(
    when (id) {
        "magnet" -> R.string.upgrade_plasma_magnet
        "torch" -> R.string.upgrade_weld_torch
        "wrench" -> R.string.upgrade_quantum_wrench
        "harvester" -> R.string.upgrade_debris_harvester
        "beacon" -> R.string.upgrade_signal_beacon
        else -> R.string.unknown_item
    }
)

@Composable
private fun localizedPlanetDescription(id: String): String =
    stringResource(planetDescriptionResource(id))

internal fun planetDescriptionResource(id: String): Int =
    when (id) {
        "p1" -> R.string.planet_desc_azurea
        "p2" -> R.string.planet_desc_canyon_prime
        "p3" -> R.string.planet_desc_nebula_echo
        "p4" -> R.string.planet_desc_crystal_hearth
        "p5" -> R.string.planet_desc_dune_horizon
        "p6" -> R.string.planet_desc_volt_nova
        "p7" -> R.string.planet_desc_gas_giant
        "p8" -> R.string.planet_desc_jungle_core
        "p9" -> R.string.planet_desc_magma_s15
        "p10" -> R.string.planet_desc_red_dust
        "p11" -> R.string.planet_desc_mech_world
        "p12" -> R.string.planet_desc_luna_silvis
        "p13" -> R.string.planet_desc_abyss_ocean
        "p14" -> R.string.planet_desc_ring_oasis
        "p15" -> R.string.planet_desc_sky_haven
        "p16" -> R.string.planet_desc_toxic_waste
        "p17" -> R.string.planet_desc_pink_nebula
        "p18" -> R.string.planet_desc_cloud_city
        "p19" -> R.string.planet_desc_rocky_bastion
        "p20" -> R.string.planet_desc_foggy_void
        else -> R.string.unknown_item
    }

@Composable
private fun localizedPlanetBonus(id: String): String = stringResource(planetBonusResource(id))

internal fun planetBonusResource(id: String): Int =
    when (id) {
        "p1" -> R.string.planet_bonus_azurea
        "p2" -> R.string.planet_bonus_canyon_prime
        "p3" -> R.string.planet_bonus_nebula_echo
        "p4" -> R.string.planet_bonus_crystal_hearth
        "p5" -> R.string.planet_bonus_dune_horizon
        "p6" -> R.string.planet_bonus_volt_nova
        "p7" -> R.string.planet_bonus_gas_giant
        "p8" -> R.string.planet_bonus_jungle_core
        "p9" -> R.string.planet_bonus_magma_s15
        "p10" -> R.string.planet_bonus_red_dust
        "p11" -> R.string.planet_bonus_mech_world
        "p12" -> R.string.planet_bonus_luna_silvis
        "p13" -> R.string.planet_bonus_abyss_ocean
        "p14" -> R.string.planet_bonus_ring_oasis
        "p15" -> R.string.planet_bonus_sky_haven
        "p16" -> R.string.planet_bonus_toxic_waste
        "p17" -> R.string.planet_bonus_pink_nebula
        "p18" -> R.string.planet_bonus_cloud_city
        "p19" -> R.string.planet_bonus_rocky_bastion
        "p20" -> R.string.planet_bonus_foggy_void
        else -> R.string.unknown_item
    }
