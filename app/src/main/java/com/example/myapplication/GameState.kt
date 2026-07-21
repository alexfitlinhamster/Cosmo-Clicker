package com.example.myapplication

import androidx.compose.ui.graphics.Color

enum class Rarity(
    val color: Color,
    val spawnWeight: Int,
    val minReward: Long,
    val maxReward: Long
) {
    COMMON(Color(0xFFB0BEC5), 60, 1, 5_000),
    UNCOMMON(Color(0xFF4CAF50), 25, 5_001, 10_000),
    RARE(Color(0xFF2196F3), 10, 10_001, 20_000),
    EPIC(Color(0xFF9C27B0), 4, 20_001, 50_000),
    LEGENDARY(Color(0xFFFF9800), 1, 250_000, 1_000_000);

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
    val cargoReward: Double = 0.0,
    val patrolTargetX: Float? = null,
    val patrolTargetY: Float? = null,
    val disabledUntil: Long = 0L
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
    val velocityY: Float = 0f,
    val isMeteor: Boolean = false,
    val reward: Double = 0.0
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
