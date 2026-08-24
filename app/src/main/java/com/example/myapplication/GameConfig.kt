package com.example.myapplication

import androidx.compose.ui.graphics.Color

data class ItemConfig(
    val id: String,
    val name: String,
    val base: Double,
    val value: Double,
    val iconRes: Int
)

data class FleetConfig(
    val id: String,
    val name: String,
    val base: Double,
    val iconRes: Int,
    val spriteIndex: Int = -1,
    val rarity: Rarity = Rarity.COMMON
)

data class PlanetConfig(
    val name: String,
    val price: Double,
    val desc: String,
    val color: Color,
    val imageRes: Int,
    val spriteIndex: Int = -1
)
