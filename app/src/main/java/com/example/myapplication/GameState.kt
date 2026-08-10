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
    VOID_ENERGY("void_energy"),
    TRADE_POWER("trade_power"),
    TRADE_LUCK("trade_luck"),
    TRADE_CLICK_BOOST("trade_click_boost"),
    TRADE_FLEET_BOOST("trade_fleet_boost")
}

enum class QuestType {
    COLLECT_DEBRIS, // Collect X debris
    CLICK_PLANET,  // Click planet X times
    BUY_UPGRADE,   // Buy X click upgrades
    OPEN_CASE,     // Open X cases
    COMPLETE_EVENT,
    OBTAIN_DRONE,
    OBTAIN_RARE_DRONE
}

enum class QuestCadence { DAILY, WEEKLY }
enum class QuestDifficulty { EASY, MEDIUM, HARD }

enum class CaseType(val priceMultiplier: Double, val premiumChance: Int) {
    COMMON(1.0, 5),
    RARE(5.0, 17),
    LEGENDARY(20.0, 45)
}

enum class WeeklyRule { CLICKS_ONLY, FRAGILE_DRONES, VOLATILE_MARKET }
enum class StationModule { HANGAR, LABORATORY, REACTOR, TRADE_HUB }

data class WeeklyGalaxy(
    val weekKey: Long = -1L,
    val rule: WeeklyRule = WeeklyRule.CLICKS_ONLY,
    val active: Boolean = false,
    val progress: Double = 0.0,
    val target: Double = 500.0,
    val rewardClaimed: Boolean = false
)

data class Quest(
    val id: String,
    val type: QuestType,
    val description: String,
    val target: Double,
    val progress: Double,
    val rewardDebris: Double = 0.0,
    val rewardCases: Int = 0,
    val rewardDroneId: String? = null,
    val targetDroneId: String? = null,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val cadence: QuestCadence = QuestCadence.DAILY,
    val difficulty: QuestDifficulty = QuestDifficulty.EASY,
    val rewardPrestigePoints: Int = 0
)

data class GameState(
    val totalDebris: Double = 50.0,
    val clickLevels: Map<String, Int> = emptyMap(),
    val fleetCounts: Map<String, Int> = emptyMap(),
    val activeFleetCounts: Map<String, Int> = emptyMap(),
    val discoveredDroneIds: Set<String> = emptySet(),
    val droneParts: Map<String, Int> = emptyMap(),
    val claimedCollectionMilestones: Set<String> = emptySet(),
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
    val openingCaseType: CaseType? = null,
    val pendingCaseOpenings: Int = 0,
    val caseBundleRewards: Map<String, Int> = emptyMap(),
    val showCaseBundleSummary: Boolean = false,
    val casesPurchased: Int = 0,
    val lastDroppedDroneId: String? = null,
    val infectedDroneId: Long? = null,
    val activeQuests: List<Quest> = emptyList(),
    val completedQuestIds: Set<String> = emptySet(),
    val questBonuses: Map<String, Double> = emptyMap(),
    val activeEffects: Map<String, Long> = emptyMap(), // Type ID to expiresAt
    val lastOfflineReward: Double = 0.0,
    val lastOfflineSeconds: Long = 0L,
    val prestigePoints: Int = 0,
    val technologies: Set<Technology> = emptySet(),
    val dailyQuestDay: Long = -1L,
    val weeklyQuestWeek: Long = -1L,
    val dailyQuestsCompletedAt: Long = -1L,
    val weeklyQuestsCompletedAt: Long = -1L,
    val sessionStats: SessionStats = SessionStats(),
    val pendingEventChain: PendingEventChain? = null,
    val eventChainResult: EventChainResult? = null,
    val lifetimeStats: LifetimeStats = LifetimeStats(),
    val unlockedAchievementIds: Set<String> = emptySet(),
    val claimedAchievementIds: Set<String> = emptySet(),
    val eventLog: List<EventLogEntry> = emptyList(),
    val weeklyGalaxy: WeeklyGalaxy = WeeklyGalaxy(),
    val stationLevels: Map<StationModule, Int> = emptyMap()
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

enum class GameEventType {
    STORM, ASTEROID, METEOR_SHOWER, BLACK_HOLE, SOLAR_FLARE, CYBER_VIRUS,
    DISTRESS_SIGNAL, ABANDONED_STATION, PIRATE_RAID, TRADING_SHIP
}

enum class DistressChoice { SALVAGE, RESCUE }
enum class StationChoice { SAFE_ROUTE, REACTOR_CORE }
enum class TradeOffer {
    POWER_CORE,
    LUCK_SCANNER,
    CLICK_AMPLIFIER,
    FLEET_OVERDRIVE,
    DEBRIS_CARGO,
    COMMON_CASE,
    RARE_CASE,
    LEGENDARY_CASE,
    RANDOM_DRONE
}

data class PendingEventChain(
    val resolvesAt: Long,
    val success: Boolean,
    val reward: Double,
    val eventType: GameEventType = GameEventType.DISTRESS_SIGNAL,
    val failurePenalty: Double = 0.0
)

data class EventChainResult(
    val success: Boolean,
    val reward: Double,
    val eventType: GameEventType = GameEventType.DISTRESS_SIGNAL,
    val loss: Double = 0.0
)

enum class EventLogOutcome { STARTED, COMPLETED, EXPIRED, CHOICE, SUCCESS, FAILURE }

data class EventLogEntry(
    val timestamp: Long,
    val eventType: GameEventType,
    val outcome: EventLogOutcome,
    val reward: Double = 0.0
)

data class GameEvent(
    val type: GameEventType,
    val expiresAt: Long,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val startedAt: Long = 0L,
    val reward: Double = 0.0
)

data class FloatingTextData(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val color: Color = Color.White
)
