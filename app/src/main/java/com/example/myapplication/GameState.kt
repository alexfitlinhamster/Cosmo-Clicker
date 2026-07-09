package com.example.myapplication

data class GameState(
    val totalDebris: Double = 50.0,
    val clickLevels: Map<String, Int> = emptyMap(),
    val fleetCounts: Map<String, Int> = emptyMap(),
    val currentPlanetId: String = "earth_2",
    val isHotelDebtActive: Boolean = false,
    val currentHotelDebt: Double = 0.0,
    val activeEvent: GameEvent? = null,
    val eventMultiplier: Double = 1.0,
    val pirateTapsLeft: Int = 0,
    val drones: List<DroneData> = emptyList(),
    val scavengeTargets: List<ScavengeTarget> = emptyList()
)

data class DroneData(
    val id: Long,
    val x: Float,
    val y: Float,
    val targetId: Long? = null,
    val state: DroneState = DroneState.IDLE,
    val hasCargo: Boolean = false,
    val type: String = "drone"
)

enum class DroneState { IDLE, MOVING_TO_DEBRIS, RETURNING }

data class ScavengeTarget(
    val id: Long,
    val x: Float,
    val y: Float
)

enum class GameEventType { STORM, ASTEROID, PIRATES }

data class GameEvent(
    val type: GameEventType,
    val title: String,
    val expiresAt: Long,
    val x: Float = 0.5f,
    val y: Float = 0.5f
)
