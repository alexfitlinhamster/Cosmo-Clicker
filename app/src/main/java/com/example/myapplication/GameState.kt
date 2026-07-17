package com.example.myapplication

import androidx.compose.ui.graphics.Color

enum class Rarity(val label: String, val color: Color, val spawnWeight: Int, val multiplier: Double) {
    COMMON("Обычный", Color(0xFFB0BEC5), 60, 1.0),
    UNCOMMON("Необычный", Color(0xFF4CAF50), 25, 2.5),
    RARE("Редкий", Color(0xFF2196F3), 10, 6.0),
    EPIC("Эпический", Color(0xFF9C27B0), 4, 15.0),
    LEGENDARY("Легендарный", Color(0xFFFF9800), 1, 40.0)
}

data class GameState(
    val totalDebris: Double = 50.0,
    val clickLevels: Map<String, Int> = emptyMap(),
    val fleetCounts: Map<String, Int> = emptyMap(),
    val currentPlanetId: String = "p1",
    val ownedPlanets: Set<String> = setOf("p1"),
    val isHotelDebtActive: Boolean = false,
    val currentHotelDebt: Double = 0.0,
    val activeEvent: GameEvent? = null,
    val eventMultiplier: Double = 1.0,
    val pirateTapsLeft: Int = 0,
    val drones: List<DroneData> = emptyList(),
    val scavengeTargets: List<ScavengeTarget> = emptyList(),
    val isOpeningCase: Boolean = false,
    val lastDroppedDroneId: String? = null 
)

data class DroneData(
    val id: Long,
    val x: Float,
    val y: Float,
    val targetId: Long? = null,
    val state: DroneState = DroneState.IDLE,
    val hasCargo: Boolean = false,
    val type: String = "drone",
    val cargoRarity: Rarity? = null // Какую редкость несет сейчас
)

enum class DroneState { IDLE, MOVING_TO_DEBRIS, RETURNING }

data class ScavengeTarget(
    val id: Long,
    val x: Float,
    val y: Float,
    val rarity: Rarity = Rarity.COMMON,
    val expiresAt: Long = 0
)

enum class GameEventType { STORM, ASTEROID, PIRATES }

data class GameEvent(
    val type: GameEventType,
    val title: String,
    val expiresAt: Long,
    val x: Float = 0.5f,
    val y: Float = 0.5f
)

data class FloatingTextData(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val color: Color = Color.White
)
