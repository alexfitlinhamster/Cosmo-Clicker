package com.example.myapplication

import androidx.compose.ui.graphics.Color

enum class Rarity(val color: Color, val spawnWeight: Int, val debrisReward: Double) {
    COMMON(Color(0xFFB0BEC5), 60, 10.0),
    UNCOMMON(Color(0xFF4CAF50), 25, 25.0),
    RARE(Color(0xFF2196F3), 10, 60.0),
    EPIC(Color(0xFF9C27B0), 4, 150.0),
    LEGENDARY(Color(0xFFFF9800), 1, 400.0);

    fun canCollect(targetRarity: Rarity): Boolean = targetRarity.ordinal <= ordinal
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
    val cargoRarity: Rarity? = null,
    val patrolTargetX: Float? = null,
    val patrolTargetY: Float? = null
)

enum class DroneState { IDLE, MOVING_TO_DEBRIS, RETURNING }

data class ScavengeTarget(
    val id: Long,
    val x: Float,
    val y: Float,
    val rarity: Rarity = Rarity.COMMON,
    val expiresAt: Long = 0,
    val imageIndex: Int = 1,
    val isFalling: Boolean = false,
    val velocityX: Float = 0f,
    val velocityY: Float = 0f
)

enum class GameEventType { STORM, ASTEROID, PIRATES, METEOR_SHOWER }

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
