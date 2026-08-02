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
    LEGENDARY(Color(0xFFFF9800), 1, 250_000, 1_000_000),
    VOID(Color(0xFF6200EA), 0, 1_250_000, 5_000_000);

    fun canCollect(targetRarity: Rarity): Boolean = targetRarity.ordinal <= ordinal
}

enum class SkillType(val id: String) {
    TIME_WARP("time_warp"),
    VOID_ENERGY("void_energy")
}

enum class QuestType {
    COLLECT_DEBRIS, // Collect X debris
    CLICK_PLANET,  // Click planet X times
    BUY_UPGRADE,   // Buy X click upgrades
    OPEN_CASE      // Open X cases
}

data class Quest(
    val id: String,
    val type: QuestType,
    val description: String,
    val target: Double,
    val progress: Double,
    val rewardDebris: Double = 0.0,
    val rewardCases: Int = 0,
    val rewardDroneId: String? = null,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false
)

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
    val eventTapsLeft: Int = 0,
    val drones: List<DroneData> = emptyList(),
    val scavengeTargets: List<ScavengeTarget> = emptyList(),
    val isOpeningCase: Boolean = false,
    val casesPurchased: Int = 0,
    val lastDroppedDroneId: String? = null,
    val infectedDroneId: Long? = null,
    val activeQuests: List<Quest> = emptyList(),
    val completedQuestIds: Set<String> = emptySet(),
    val questBonuses: Map<String, Double> = emptyMap(),
    val activeEffects: Map<String, Long> = emptyMap(), // Type ID to expiresAt
    val lastOfflineReward: Double = 0.0,
    val prestigePoints: Int = 0,
    val technologies: Set<Technology> = emptySet(),
    val dailyQuestDay: Long = -1L,
    val sessionStats: SessionStats = SessionStats()
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

enum class DroneState { IDLE, MOVING_TO_DEBRIS, RETURNING, BROKEN, INFECTED, SUCKED_IN, JAMMED }

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

enum class GameEventType { STORM, ASTEROID, METEOR_SHOWER, BLACK_HOLE, SOLAR_FLARE, CYBER_VIRUS }

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
