package com.example.myapplication

import android.app.Application
import androidx.compose.ui.graphics.Color
import com.example.myapplication.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.pow
import kotlin.math.sqrt
import java.util.concurrent.atomic.AtomicLong

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = GameRepository(application)
    private val storeActionLock = Any()
    private val lastStoreActionNanos = mutableMapOf<String, Long>()
    private val debrisId = AtomicLong(System.currentTimeMillis())
    private val simulationJobs = mutableListOf<Job>()
    internal var randomProvider: RandomProvider = KotlinRandomProvider

    private val droneNames = listOf(
        "Scrap-Bot", "Copper Cloud", "Rusty Rover", "Azure Ace", "Cobalt Collector",
        "Blue Beam", "Forest Phantom", "Jade Jumper", "Emerald Eye", "Crimson Crusher",
        "Ruby Reaper", "Solar Stinger", "Amber Apex", "Void Vulture", "Shadow Shifter",
        "Ghost Glider", "Plasma Prowler", "Neon Nibbler", "Cyber Cicada", "Titan Talon",
        "Iron Icarus", "Gold Guardian", "Gilded Golem", "Quartz Quill", "Silver Spectre",
        "Diamond Diver", "Onyx Orb", "Obsidian Owl", "Quantum Quark"
    )

    val clickItems = listOf(
        ItemConfig("magnet", "Plasma Magnet", 15.0, 1.0, R.drawable.upgrade_magnet_v2),
        ItemConfig("torch", "Weld Torch", 200.0, 10.0, R.drawable.upgrade_weld_torch_v2),
        ItemConfig("wrench", "Quantum Wrench", 5000.0, 100.0, R.drawable.upgrade_quantum_wrench_v2),
        ItemConfig("harvester", "Debris Harvester", 80000.0, 750.0, R.drawable.upgrade_debris_harvester_v2),
        ItemConfig("beacon", "Signal Beacon", 1500000.0, 6000.0, R.drawable.upgrade_signal_beacon_v2)
    )

    val fleetItems = (1..29).map { i ->
        val rarity = when {
            i <= 10 -> Rarity.COMMON
            i <= 18 -> Rarity.UNCOMMON
            i <= 24 -> Rarity.RARE
            i <= 27 -> Rarity.EPIC
            else -> Rarity.LEGENDARY
        }
        FleetConfig(
            id = "drone_$i",
            name = droneNames.getOrElse(i-1) { "Drone #$i" },
            base = 10.0 * 1.8.pow(i.toDouble() - 1),
            iconRes = GameResourceRegistry.drone(i),
            spriteIndex = -1,
            rarity = rarity
        )
    }
    val fleetById: Map<String, FleetConfig> = fleetItems.associateBy(FleetConfig::id)

    val planets = mapOf(
        "p1" to PlanetConfig("Azurea", 0.0, "Home planet", Color(0xFF2196F3), R.drawable.planet_1),
        "p2" to PlanetConfig("Canyon Prime", 10000.0, "Dry and windy world", Color(0xFFFFA726), R.drawable.planet_2),
        "p3" to PlanetConfig("Nebula Echo", 50000.0, "Glow of distant stars", Color(0xFF7E57C2), R.drawable.planet_3),
        "p4" to PlanetConfig("Crystal Hearth", 250000.0, "Fragile beauty", Color(0xFF26C6DA), R.drawable.planet_4),
        "p5" to PlanetConfig("Dune Horizon", 1250000.0, "Endless sands", Color(0xFFFFCC80), R.drawable.planet_5),
        "p6" to PlanetConfig("Volt Nova", 6250000.0, "World of electricity", Color(0xFFFFF176), R.drawable.planet_6),
        "p7" to PlanetConfig("Gas Giant G-7", 31250000.0, "Dense atmosphere", Color(0xFF9CCC65), R.drawable.planet_7),
        "p8" to PlanetConfig("Jungle Core", 156250000.0, "Wild nature", Color(0xFF43A047), R.drawable.planet_8),
        "p9" to PlanetConfig("Magma S-15", 781250000.0, "Burning abyss", Color(0xFFE53935), R.drawable.planet_9),
        "p10" to PlanetConfig("Red Dust", 3906250000.0, "Ancient ruins", Color(0xFFFF7043), R.drawable.planet_10),
        "p11" to PlanetConfig("Mech World X", 19531250000.0, "Factory complex", Color(0xFF78909C), R.drawable.planet_11),
        "p12" to PlanetConfig("Luna Silvis", 97656250000.0, "Night guardian", Color(0xFFBDBDBD), R.drawable.planet_12),
        "p13" to PlanetConfig("Abyss Ocean", 488281250000.0, "Deep sea", Color(0xFF1E88E5), R.drawable.planet_1),
        "p14" to PlanetConfig("Ring Oasis", 2441406250000.0, "Sky belt", Color(0xFFFFD54F), R.drawable.planet_2),
        "p15" to PlanetConfig("Sky Haven", 12207031250000.0, "Above clouds", Color(0xFFE1F5FE), R.drawable.planet_3),
        "p16" to PlanetConfig("Toxic Waste", 61035156250000.0, "Corrosive", Color(0xFF76FF03), R.drawable.planet_4),
        "p17" to PlanetConfig("Pink Nebula", 305175781250000.0, "Sweet shimmer", Color(0xFFF06292), R.drawable.planet_5),
        "p18" to PlanetConfig("Cloud City", 1525878906250000.0, "Floating", Color(0xFF81D4FA), R.drawable.planet_6),
        "p19" to PlanetConfig("Rocky Bastion", 7629394531250000.0, "Stone fortress", Color(0xFF8D6E63), R.drawable.planet_7),
        "p20" to PlanetConfig("Foggy Void", 38146972656250000.0, "Light disappears", Color(0xFF455A64), R.drawable.planet_8)
    ).mapValues { (id, config) -> config.copy(price = EconomyBalance.planetPrice(EconomyBalance.planetIndex(id))) }

    private val _gameState = MutableStateFlow(loadGameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    init {
        resumeSimulation()
        
        refreshTimedQuests()
    }

    private fun createDailyQuests(key: Long, planetId: String): List<Quest> = listOf(
        timedQuest("d2_click_250_$key", QuestType.CLICK_PLANET, 250, 15_000.0, planetId, QuestCadence.DAILY),
        timedQuest("d2_collect_50_$key", QuestType.COLLECT_DEBRIS, 50, 20_000.0, planetId, QuestCadence.DAILY),
        timedQuest("d2_upgrade_5_$key", QuestType.BUY_UPGRADE, 5, 25_000.0, planetId, QuestCadence.DAILY),
        timedQuest("d2_events_4_$key", QuestType.COMPLETE_EVENT, 4, 35_000.0, planetId, QuestCadence.DAILY),
        timedQuest("d2_cases_3_$key", QuestType.OPEN_CASE, 3, 50_000.0, planetId, QuestCadence.DAILY)
    )

    private fun createWeeklyQuests(key: Long, planetId: String): List<Quest> {
        val obtainableDrones = fleetItems.filter { it.rarity.spawnWeight > 0 }
        val targetDrone = obtainableDrones[Math.floorMod(key, obtainableDrones.size.toLong()).toInt()]
        return listOf(
        timedQuest("w2_click_7500_$key", QuestType.CLICK_PLANET, 7_500, 750_000.0, planetId, QuestCadence.WEEKLY),
        timedQuest("w2_collect_1000_$key", QuestType.COLLECT_DEBRIS, 1_000, 1_000_000.0, planetId, QuestCadence.WEEKLY),
        timedQuest("w2_upgrade_75_$key", QuestType.BUY_UPGRADE, 75, 1_250_000.0, planetId, QuestCadence.WEEKLY),
        timedQuest("w2_events_40_$key", QuestType.COMPLETE_EVENT, 40, 1_500_000.0, planetId, QuestCadence.WEEKLY),
        Quest(
            id = "w2_drone_${targetDrone.id}_$key",
            type = QuestType.OBTAIN_DRONE,
            description = getApplication<Application>().getString(R.string.quest_obtain_drone, targetDrone.name),
            target = 1.0,
            progress = 0.0,
            cadence = QuestCadence.WEEKLY,
            rewardPrestigePoints = 2,
            targetDroneId = targetDrone.id
        )
        )
    }

    private fun timedQuest(
        id: String,
        type: QuestType,
        target: Int,
        baseReward: Double,
        planetId: String,
        cadence: QuestCadence
    ) = Quest(
        id = id,
        type = type,
        description = questDescription(type, target),
        target = target.toDouble(),
        progress = 0.0,
        rewardDebris = EconomyBalance.scaledReward(baseReward, planetId),
        cadence = cadence
    )

    private fun questDescription(type: QuestType, target: Int): String =
        getApplication<Application>().getString(
            when (type) {
                QuestType.COLLECT_DEBRIS -> R.string.quest_collect_debris
                QuestType.CLICK_PLANET -> R.string.quest_click_planet
                QuestType.BUY_UPGRADE -> R.string.quest_buy_upgrade
                QuestType.OPEN_CASE -> R.string.quest_open_case
                QuestType.COMPLETE_EVENT -> R.string.quest_complete_event
                QuestType.OBTAIN_DRONE -> R.string.quest_obtain_any_drone
            },
            target
        )

    private fun loadGameState(): GameState {
        val clickLevels = mutableMapOf<String, Int>()
        clickItems.forEach { clickLevels[it.id] = prefs.getInt("click_${it.id}", 0).coerceAtLeast(0) }
        
        val rawFleetCounts = mutableMapOf<String, Int>()
        fleetItems.forEach { rawFleetCounts[it.id] = prefs.getInt("fleet_${it.id}", 0).coerceAtLeast(0) }
        val fleetCounts = rawFleetCounts.limitOwnedFleet()
        val activeFleetCounts = if (prefs.contains("activeFleetInitialized")) {
            fleetItems.associate { item ->
                item.id to prefs.getInt("activeFleet_${item.id}", 0)
                    .coerceIn(0, fleetCounts[item.id] ?: 0)
            }.limitActiveFleet()
        } else {
            fleetCounts.limitActiveFleet()
        }
        val migratedDiscoveries = fleetCounts.filterValues { it > 0 }.keys
        val discoveredDroneIds = prefs.getStringSet("discoveredDroneIds", migratedDiscoveries)
            .orEmpty().filterTo(mutableSetOf()) { it in fleetById }
        val droneParts = fleetItems.associate { it.id to prefs.getInt("droneParts_${it.id}", 0).coerceAtLeast(0) }

        val ownedPlanets = prefs.getStringSet("ownedPlanets", setOf("p1"))
            ?.filterTo(mutableSetOf()) { it in planets }
            .orEmpty()
            .plus("p1")

        // Load Quests
        val activeQuests = mutableListOf<Quest>()
        val questIds = prefs.getStringSet("activeQuestIds", emptySet()) ?: emptySet()
        questIds.forEach { id ->
            val type = prefs.getString("quest_${id}_type", null)
                ?.let { storedType -> QuestType.entries.firstOrNull { it.name == storedType } }
                ?: return@forEach
            val desc = prefs.getString("quest_${id}_desc", "") ?: ""
            val target = prefs.getFloat("quest_${id}_target", 0f).toDouble().finiteOr(0.0)
            if (target <= 0.0) return@forEach
            val progress = prefs.getFloat("quest_${id}_progress", 0f).toDouble()
                .finiteOr(0.0)
                .coerceIn(0.0, target)
            val rewardDebris = prefs.getFloat("quest_${id}_rewardDebris", 0f).toDouble()
                .finiteOr(0.0)
                .coerceAtLeast(0.0)
            val rewardCases = prefs.getInt("quest_${id}_rewardCases", 0)
            val isCompleted = prefs.getBoolean("quest_${id}_completed", false)
            val cadence = prefs.getString("quest_${id}_cadence", QuestCadence.DAILY.name)
                ?.let { value -> QuestCadence.entries.firstOrNull { it.name == value } }
                ?: QuestCadence.DAILY
            activeQuests.add(
                Quest(
                    id = id,
                    type = type,
                    description = desc,
                    target = target,
                    progress = progress,
                    rewardDebris = rewardDebris,
                    rewardCases = rewardCases.coerceAtLeast(0),
                    isCompleted = isCompleted,
                    cadence = cadence,
                    rewardPrestigePoints = prefs.getInt("quest_${id}_rewardPrestigePoints", 0)
                        .coerceAtLeast(0),
                    targetDroneId = prefs.getString("quest_${id}_targetDroneId", null)
                )
            )
        }

        val storedPlanetId = prefs.getString("currentPlanetId", "p1") ?: "p1"
        val currentPlanetId = storedPlanetId.takeIf { it in ownedPlanets && it in planets } ?: "p1"

        val loadedState = GameState(
            totalDebris = loadPreciseDouble("totalDebris", 50.0).finiteOr(50.0).coerceAtLeast(0.0),
            clickLevels = clickLevels,
            fleetCounts = fleetCounts,
            activeFleetCounts = activeFleetCounts,
            discoveredDroneIds = discoveredDroneIds,
            droneParts = droneParts,
            claimedCollectionMilestones = prefs.getStringSet("claimedCollectionMilestones", emptySet()).orEmpty(),
            currentPlanetId = currentPlanetId,
            ownedPlanets = ownedPlanets,
            isHotelDebtActive = prefs.getBoolean("isHotelDebtActive", false),
            currentHotelDebt = loadPreciseDouble("currentHotelDebt", 0.0).finiteOr(0.0).coerceAtLeast(0.0),
            casesPurchased = prefs.getInt("casesPurchased", fleetCounts.values.sum()).coerceAtLeast(0),
            activeQuests = activeQuests,
            completedQuestIds = prefs.getStringSet("completedQuestIds", emptySet()) ?: emptySet(),
            prestigePoints = prefs.getInt("prestigePoints", 0).coerceAtLeast(0),
            technologies = prefs.getStringSet("technologies", emptySet()).orEmpty()
                .mapNotNullTo(mutableSetOf()) { name -> Technology.entries.firstOrNull { it.name == name } },
            dailyQuestDay = prefs.getLong("dailyQuestDay", -1L),
            weeklyQuestWeek = prefs.getLong("weeklyQuestWeek", -1L),
            dailyQuestsCompletedAt = prefs.getLong("dailyQuestsCompletedAt", -1L),
            weeklyQuestsCompletedAt = prefs.getLong("weeklyQuestsCompletedAt", -1L),
            lifetimeStats = LifetimeStats(
                clicks = prefs.getLong("lifetimeClicks", 0L).coerceAtLeast(0L),
                casesOpened = prefs.getInt("lifetimeCasesOpened", 0).coerceAtLeast(0),
                eventsCompleted = prefs.getInt("lifetimeEventsCompleted", 0).coerceAtLeast(0),
                prestiges = prefs.getInt("lifetimePrestiges", 0).coerceAtLeast(0)
            ),
            unlockedAchievementIds = prefs.getStringSet("unlockedAchievementIds", emptySet()) ?: emptySet(),
            claimedAchievementIds = prefs.getStringSet("claimedAchievementIds", emptySet()) ?: emptySet(),
            eventLog = EventLogCodec.decode(prefs.getString("eventLog", null)),
            weeklyGalaxy = WeeklyGalaxy(
                weekKey = prefs.getLong("galaxyWeekKey", -1L),
                rule = prefs.getString("galaxyRule", WeeklyRule.CLICKS_ONLY.name)
                    ?.let { name -> WeeklyRule.entries.firstOrNull { it.name == name } } ?: WeeklyRule.CLICKS_ONLY,
                active = prefs.getBoolean("galaxyActive", false),
                progress = loadPreciseDouble("galaxyProgress", 0.0).coerceAtLeast(0.0),
                target = loadPreciseDouble("galaxyTarget", 500.0).coerceAtLeast(1.0),
                rewardClaimed = prefs.getBoolean("galaxyRewardClaimed", false)
            ),
            titanWins = prefs.getInt("titanWins", 0).coerceAtLeast(0),
            completedChallengeIds = prefs.getStringSet("completedChallengeIds", emptySet()).orEmpty()
                .mapNotNullTo(mutableSetOf()) { name -> ChallengeId.entries.firstOrNull { it.name == name } },
            stationLevels = StationModule.entries.associateWith { module ->
                prefs.getInt("station_${module.name}", 0).coerceIn(0, 5)
            }
        )
        val offline = OfflineProgressEngine.calculate(
            lastActiveAtMillis = prefs.getLong(LAST_ACTIVE_AT_KEY, 0L),
            nowMillis = System.currentTimeMillis(),
            fleetCounts = loadedState.activeFleetCounts,
            fleetRarities = fleetById.mapValues { it.value.rarity },
            rewardMultiplier = (if (Technology.OFFLINE_AI in loadedState.technologies) 2.0 else 1.0) *
                EconomyBalance.planetIncomeMultiplier(loadedState.currentPlanetId)
        )
        return FeatureEngine.refreshWeekly(loadedState).copy(
            totalDebris = loadedState.totalDebris + offline.reward,
            lastOfflineReward = offline.reward
        )
    }

    private fun saveGameState() {
        val state = _gameState.value
        prefs.edit {
            putInt(SAVE_VERSION_KEY, CURRENT_SAVE_VERSION)
            putLong(LAST_ACTIVE_AT_KEY, System.currentTimeMillis())
            putLong("totalDebrisBits", GameRules.encodeDouble(state.totalDebris))
            state.clickLevels.forEach { (id, lvl) -> putInt("click_$id", lvl) }
            state.fleetCounts.forEach { (id, count) -> putInt("fleet_$id", count) }
            putBoolean("activeFleetInitialized", true)
            state.activeFleetCounts.forEach { (id, count) -> putInt("activeFleet_$id", count) }
            putStringSet("discoveredDroneIds", state.discoveredDroneIds)
            state.droneParts.forEach { (id, parts) -> putInt("droneParts_$id", parts) }
            putStringSet("claimedCollectionMilestones", state.claimedCollectionMilestones)
            putString("currentPlanetId", state.currentPlanetId)
            putStringSet("ownedPlanets", state.ownedPlanets)
            putBoolean("isHotelDebtActive", state.isHotelDebtActive)
            putLong("currentHotelDebtBits", GameRules.encodeDouble(state.currentHotelDebt))
            putInt("casesPurchased", state.casesPurchased)
            putInt("prestigePoints", state.prestigePoints)
            putStringSet("technologies", state.technologies.map { it.name }.toSet())
            putLong("dailyQuestDay", state.dailyQuestDay)
            putLong("weeklyQuestWeek", state.weeklyQuestWeek)
            putLong("dailyQuestsCompletedAt", state.dailyQuestsCompletedAt)
            putLong("weeklyQuestsCompletedAt", state.weeklyQuestsCompletedAt)
            putLong("lifetimeClicks", state.lifetimeStats.clicks)
            putInt("lifetimeCasesOpened", state.lifetimeStats.casesOpened)
            putInt("lifetimeEventsCompleted", state.lifetimeStats.eventsCompleted)
            putInt("lifetimePrestiges", state.lifetimeStats.prestiges)
            putStringSet("unlockedAchievementIds", state.unlockedAchievementIds)
            putStringSet("claimedAchievementIds", state.claimedAchievementIds)
            putString("eventLog", EventLogCodec.encode(state.eventLog))
            putLong("galaxyWeekKey", state.weeklyGalaxy.weekKey)
            putString("galaxyRule", state.weeklyGalaxy.rule.name)
            putBoolean("galaxyActive", state.weeklyGalaxy.active)
            putLong("galaxyProgressBits", GameRules.encodeDouble(state.weeklyGalaxy.progress))
            putLong("galaxyTargetBits", GameRules.encodeDouble(state.weeklyGalaxy.target))
            putBoolean("galaxyRewardClaimed", state.weeklyGalaxy.rewardClaimed)
            putInt("titanWins", state.titanWins)
            putStringSet("completedChallengeIds", state.completedChallengeIds.map { it.name }.toSet())
            state.stationLevels.forEach { (module, level) -> putInt("station_${module.name}", level) }
            
            // Save Quests
            val activeQuestIds = state.activeQuests.map { it.id }.toSet()
            putStringSet("activeQuestIds", activeQuestIds)
            state.activeQuests.forEach { q ->
                putString("quest_${q.id}_type", q.type.name)
                putString("quest_${q.id}_desc", q.description)
                putFloat("quest_${q.id}_target", q.target.toFloat())
                putFloat("quest_${q.id}_progress", q.progress.toFloat())
                putFloat("quest_${q.id}_rewardDebris", q.rewardDebris.toFloat())
                putInt("quest_${q.id}_rewardCases", q.rewardCases)
                putBoolean("quest_${q.id}_completed", q.isCompleted)
                putString("quest_${q.id}_cadence", q.cadence.name)
                putInt("quest_${q.id}_rewardPrestigePoints", q.rewardPrestigePoints)
                q.targetDroneId?.let { putString("quest_${q.id}_targetDroneId", it) }
            }
            putStringSet("completedQuestIds", state.completedQuestIds)
            
        }
    }

    private fun loadPreciseDouble(key: String, defaultValue: Double): Double {
        val preciseKey = "${key}Bits"
        return if (prefs.contains(preciseKey)) {
            GameRules.decodeDouble(
                prefs.getLong(preciseKey, GameRules.encodeDouble(defaultValue))
            )
        } else {
            prefs.getFloat(key, defaultValue.toFloat()).toDouble()
        }
    }

    fun resumeSimulation() {
        if (simulationJobs.any { it.isActive }) return
        simulationJobs.clear()
        val now = System.currentTimeMillis()
        _gameState.update { EventEngine.expireEventIfNeeded(it, now) }
        startGameLoop()
        startEventLoop()
        startDroneLoop()
        startTrashSpawnLoop()
        startDebrisShowerLoop()
    }

    fun pauseSimulation() {
        simulationJobs.forEach(Job::cancel)
        simulationJobs.clear()
        saveGameState()
    }

    private fun launchSimulationLoop(block: suspend CoroutineScope.() -> Unit) {
        simulationJobs += viewModelScope.launch(Dispatchers.Default, block = block)
    }

    private fun Double.finiteOr(fallback: Double): Double = if (isFinite()) this else fallback

    private fun startGameLoop() {
        launchSimulationLoop {
            var ticksUntilSave = SAVE_INTERVAL_SECONDS
            while (isActive) {
                delay(1000)
                processEconomyTick()
                ticksUntilSave--
                if (ticksUntilSave <= 0) {
                    saveGameState()
                    ticksUntilSave = SAVE_INTERVAL_SECONDS
                }
            }
        }
    }

    private fun startTrashSpawnLoop() {
        launchSimulationLoop {
            while (isActive) {
                val state = _gameState.value
                val interval = if (state.activeEvent?.type == GameEventType.BLACK_HOLE) {
                    randomProvider.nextLong(1500, 4000) // Spits out void debris faster
                } else {
                    randomProvider.nextLong(2000, 40000)
                }
                delay(interval)
                
                _gameState.update { currentState ->
                    if (currentState.scavengeTargets.size < 8) {
                        val rarity = if (currentState.activeEvent?.type == GameEventType.BLACK_HOLE) {
                            if (randomProvider.nextInt(100) < 40) Rarity.VOID else rollTrashRarity(currentState.currentPlanetId)
                        } else {
                            rollTrashRarity(currentState.currentPlanetId)
                        }
                        
                        val newTarget = ScavengeTarget(
                            id = debrisId.incrementAndGet(),
                            x = if (rarity == Rarity.VOID) currentState.activeEvent?.x ?: randomProvider.nextFloat() else randomProvider.nextFloat(),
                            y = if (rarity == Rarity.VOID) currentState.activeEvent?.y ?: (randomProvider.nextFloat() * 0.6f + 0.1f) else randomProvider.nextFloat() * 0.6f + 0.1f,
                            rarity = rarity,
                            expiresAt = System.currentTimeMillis() + 60000,
                            imageIndex = debrisImageIndex(rarity),
                            reward = rollDebrisReward(rarity, currentState.currentPlanetId)
                        )
                        currentState.copy(scavengeTargets = currentState.scavengeTargets + newTarget)
                    } else currentState
                }
            }
        }
        
        launchSimulationLoop {
            while (isActive) {
                delay(5000)
                val now = System.currentTimeMillis()
                _gameState.update { it.copy(
                    scavengeTargets = it.scavengeTargets.filter { t -> t.expiresAt > now }
                )}
            }
        }
    }

    private fun startDebrisShowerLoop() {
        launchSimulationLoop {
            while (isActive) {
                delay(DEBRIS_SHOWER_SPAWN_INTERVAL_MS)
                _gameState.update { state ->
                    if (state.activeEvent?.type != GameEventType.METEOR_SHOWER ||
                        state.scavengeTargets.count { it.isFalling } >= MAX_FALLING_DEBRIS
                    ) {
                        return@update state
                    }

                    val isMeteor = randomProvider.nextInt(100) < METEOR_SPAWN_CHANCE_PERCENT
                    val rarity = if (isMeteor) Rarity.COMMON else rollTrashRarity(state.currentPlanetId)
                    state.copy(
                        scavengeTargets = state.scavengeTargets + ScavengeTarget(
                            id = debrisId.incrementAndGet(),
                            x = randomProvider.nextFloat(),
                            y = -0.08f,
                            rarity = rarity,
                            expiresAt = state.activeEvent.expiresAt,
                            imageIndex = debrisImageIndex(rarity),
                            isFalling = true,
                            velocityX = randomProvider.nextFloat() * 0.008f - 0.004f,
                            velocityY = randomProvider.nextFloat() * 0.0045f + 0.006f,
                            isMeteor = isMeteor,
                            reward = rollDebrisReward(
                                if (isMeteor) Rarity.LEGENDARY else rarity,
                                state.currentPlanetId
                            ) * 0.5
                        )
                    )
                }
            }
        }
    }

    private fun debrisImageIndex(rarity: Rarity): Int = when (rarity) {
        Rarity.COMMON -> {
            val pool = listOf(1, 2, 7, 8)
            randomProvider.choose(pool)
        }
        Rarity.UNCOMMON -> {
            val pool = listOf(3, 9, 10)
            randomProvider.choose(pool)
        }
        Rarity.RARE -> {
            val pool = listOf(4, 11, 12)
            randomProvider.choose(pool)
        }
        Rarity.EPIC -> {
            val pool = listOf(5, 13)
            randomProvider.choose(pool)
        }
        Rarity.LEGENDARY -> {
            val pool = listOf(6, 14)
            randomProvider.choose(pool)
        }
        Rarity.VOID -> 6 // Use legendary icon but with different behavior
    }

    private fun rollDebrisReward(rarity: Rarity, planetId: String): Double {
        var reward = if (rarity == Rarity.VOID) {
            randomProvider.nextLong(Rarity.VOID.minReward, Rarity.VOID.maxReward + 1).toDouble()
        } else {
            randomProvider.nextLong(rarity.minReward, rarity.maxReward + 1).toDouble()
        }
        // Crystal Hearth (p4) or Sky Haven (p15): 50% chance to get +100% (x2) reward for debris
        if ((planetId == "p4" || planetId == "p15") && rarity != Rarity.LEGENDARY) {
            if (randomProvider.nextInt(100) < 50) {
                reward *= 2.0
            }
        }
        // Pink Nebula (p17): +50% reward from all sources
        if (planetId == "p17") {
            reward *= 1.5
        }
        return EconomyBalance.scaledReward(reward, planetId)
    }

    private fun rollTrashRarity(planetId: String): Rarity {
        // Red Dust (p10) or Sky Haven (p15): Epic/Legendary weight x2
        val weights = Rarity.entries.map { r ->
            val planetMultiplier = if ((planetId == "p10" || planetId == "p15") && (r == Rarity.EPIC || r == Rarity.LEGENDARY)) 2 else 1
            val technologyMultiplier = if (Technology.LUCK_MATRIX in _gameState.value.technologies && (r == Rarity.EPIC || r == Rarity.LEGENDARY)) 2 else 1
            val tradeMultiplier = if (
                (_gameState.value.activeEffects[SkillType.TRADE_LUCK.id] ?: 0L) > System.currentTimeMillis() &&
                (r == Rarity.RARE || r == Rarity.EPIC || r == Rarity.LEGENDARY)
            ) 2 else 1
            r.spawnWeight * planetMultiplier * technologyMultiplier * tradeMultiplier
        }
        val totalWeight = weights.sum()
        val roll = randomProvider.nextInt(totalWeight)
        var cumulative = 0
        for (i in Rarity.entries.indices) {
            cumulative += weights[i]
            if (roll < cumulative) return Rarity.entries[i]
        }
        return Rarity.COMMON
    }

    private fun startEventLoop() {
        launchSimulationLoop {
            while (isActive) {
                val intervalPlanetId = _gameState.value.currentPlanetId
                delay(EventEngine.nextIntervalMillis(intervalPlanetId, randomProvider))

                val state = _gameState.value
                if (state.activeEvent != null || state.pendingEventChain != null) continue
                val selectedType = EventEngine.selectType(state.currentPlanetId, randomProvider) ?: continue
                val duration = EventEngine.nextDurationMillis(state.currentPlanetId, randomProvider)
                val clickValue = if (
                    selectedType == GameEventType.ASTEROID ||
                    selectedType == GameEventType.DISTRESS_SIGNAL ||
                    selectedType == GameEventType.ABANDONED_STATION ||
                    selectedType == GameEventType.PIRATE_RAID ||
                    selectedType == GameEventType.TRADING_SHIP
                ) calculateClickValue() else 1.0
                val now = System.currentTimeMillis()
                _gameState.update {
                    if (it.activeEvent == null) {
                        EventEngine.startEvent(it, selectedType, duration, now, randomProvider, clickValue)
                    } else {
                        it
                    }
                }
            }
        }
    }

    fun onAsteroidClick() {
        val now = System.currentTimeMillis()
        _gameState.update { EventEngine.onAsteroidClick(it, now) }
    }
    
    fun onBlackHoleClick() {
        val now = System.currentTimeMillis()
        _gameState.update { state ->
            EventEngine.onBlackHoleClick(state, now, randomProvider) { x, y, expiresAt ->
                ScavengeTarget(
                    id = debrisId.incrementAndGet(),
                    x = x,
                    y = y,
                    rarity = Rarity.RARE,
                    expiresAt = expiresAt,
                    imageIndex = debrisImageIndex(Rarity.RARE),
                    reward = rollDebrisReward(Rarity.RARE, state.currentPlanetId)
                )
            }
        }
    }

    fun onPirateRaidClick() {
        val now = System.currentTimeMillis()
        _gameState.update { EventEngine.onPirateRaidClick(it, now) }
    }

    fun onDroneClick(droneId: Long) {
        val now = System.currentTimeMillis()
        _gameState.update { EventEngine.onInfectedDroneClick(it, droneId, now) }
    }

    fun resolveCyberVirus(success: Boolean) {
        val now = System.currentTimeMillis()
        _gameState.update { EventEngine.resolveCyberVirus(it, success, now) }
        saveGameState()
    }

    fun respondToDistressSignal(choice: DistressChoice) {
        val now = System.currentTimeMillis()
        _gameState.update { EventEngine.respondToDistressSignal(it, choice, now, randomProvider) }
    }

    fun respondToAbandonedStation(choice: StationChoice) {
        val now = System.currentTimeMillis()
        _gameState.update { EventEngine.respondToAbandonedStation(it, choice, now, randomProvider) }
    }

    fun buyTradeOffer(offer: TradeOffer) {
        val now = System.currentTimeMillis()
        _gameState.update { EventEngine.buyTradeOffer(it, offer, now) }
    }

    fun clearEventChainResult() {
        _gameState.update(EventEngine::clearChainResult)
    }

    fun clearEventLog() {
        _gameState.update { it.copy(eventLog = emptyList()) }
        saveGameState()
    }

    private fun startDroneLoop() {
        launchSimulationLoop {
            while (isActive) {
                delay(150)
                updateDrones()
            }
        }
    }

    private fun updateDrones() {
        val now = System.currentTimeMillis()
        val currentState = _gameState.value
        val planetId = currentState.currentPlanetId
        val fleetCounts = currentState.activeFleetCounts
        val activeEvent = currentState.activeEvent
        val isBlackHole = activeEvent?.type == GameEventType.BLACK_HOLE
        val isSolarFlare = activeEvent?.type == GameEventType.SOLAR_FLARE
        val isStorm = activeEvent?.type == GameEventType.STORM
        val infectedId = currentState.infectedDroneId
        val bhX = activeEvent?.x ?: 0.5f
        val bhY = activeEvent?.y ?: 0.5f
        
        _gameState.update { state ->
            val drones = state.drones.toMutableList()
            
            // Sync drones list with fleet counts
            val dronesByType = drones.groupBy(DroneData::type)
            fleetItems.forEach { item ->
                val count = fleetCounts[item.id] ?: 0
                val currentOfThisType = dronesByType[item.id].orEmpty()
                if (currentOfThisType.size < count) {
                    repeat(count - currentOfThisType.size) {
                        val spawn = randomPatrolPoint()
                        drones.add(DroneData(randomProvider.nextLong(Long.MAX_VALUE), spawn.first, spawn.second, type = item.id))
                    }
                } else if (currentOfThisType.size > count) {
                    val toRemove = currentOfThisType.size - count
                    drones.removeAll(currentOfThisType.take(toRemove).toSet())
                }
            }

            val targets = state.scavengeTargets.mapNotNull { target ->
                if (isBlackHole) {
                    val dx = bhX - target.x
                    val dy = bhY - target.y
                    val distSq = dx * dx + dy * dy
                    if (distSq < 0.0005f) return@mapNotNull null // Sucked in
                    val dist = sqrt(distSq.toDouble()).toFloat()
                    return@mapNotNull target.copy(
                        x = target.x + (dx / dist) * 0.02f,
                        y = target.y + (dy / dist) * 0.02f
                    )
                }
                if (!target.isFalling) return@mapNotNull target
                target.copy(
                    x = target.x + target.velocityX,
                    y = target.y + target.velocityY
                ).takeIf { it.x in -0.15f..1.15f && it.y <= 1.1f }
            }.toMutableList()
            if (drones.isEmpty()) return@update state.copy(scavengeTargets = targets)

            var debrisGained = 0.0
            val claimedTargetIds = drones.filter { it.state != DroneState.BROKEN }.mapNotNullTo(mutableSetOf()) { it.targetId }
            var debrisCollectedCount = 0

            val updatedDrones = drones.map { drone ->
                if (drone.state == DroneState.BROKEN) {
                    if (now >= drone.disabledUntil) {
                        return@map drone.copy(state = DroneState.IDLE, disabledUntil = 0)
                    }
                    return@map drone
                }

                val droneConfig = fleetById[drone.type]
                val droneRarity = droneConfig?.rarity ?: Rarity.COMMON

                var moveMultiplier = 1.0f
                if (state.activeEffects.getOrDefault(SkillType.VOID_ENERGY.id, 0L) > now) {
                    moveMultiplier = 2.0f
                }
                if (isStorm) moveMultiplier *= 0.7f
                moveMultiplier *= DroneTraitEngine.modifiers(state.activeFleetCounts).speedMultiplier

                var nx = drone.x
                var ny = drone.y
                var nState = drone.state
                var nTargetId = drone.targetId
                var nHasCargo = drone.hasCargo
                var nCargoRarity = drone.cargoRarity
                var nCargoReward = drone.cargoReward
                var nPatrolTargetX = drone.patrolTargetX
                var nPatrolTargetY = drone.patrolTargetY
                var nDisabledUntil = 0L

                if (isBlackHole) {
                    val dx = bhX - nx
                    val dy = bhY - ny
                    val distSq = dx * dx + dy * dy
                    
                    // Gravity Pull
                    val dist = sqrt(distSq.toDouble()).toFloat().coerceAtLeast(0.01f)
                    val gravityStrength = (0.002f / distSq).coerceIn(0.005f, 0.02f)
                    
                    if (distSq < 0.0008f) { // Sucked in
                        nState = DroneState.BROKEN
                        nDisabledUntil = now + 120_000 // 2 minutes
                        nTargetId = null
                        nHasCargo = false
                    } else {
                        nx += (dx / dist) * gravityStrength
                        ny += (dy / dist) * gravityStrength
                        moveMultiplier *= (dist * 2f).coerceIn(0.2f, 1.0f) // Slower near center
                        nState = DroneState.SUCKED_IN
                    }
                } else if (isSolarFlare) {
                    nx += randomProvider.nextFloat() * 0.01f - 0.005f
                    ny += randomProvider.nextFloat() * 0.01f - 0.005f
                    nState = DroneState.JAMMED
                } else if (drone.id == infectedId) {
                    nState = DroneState.INFECTED
                    nx += randomProvider.nextFloat() * 0.04f - 0.02f
                    ny += randomProvider.nextFloat() * 0.04f - 0.02f
                    debrisGained -= EventEngine.cyberVirusTheft(state.totalDebris)
                } else {
                    if (nState == DroneState.SUCKED_IN || nState == DroneState.JAMMED || nState == DroneState.INFECTED) nState = DroneState.IDLE
                    
                    var moveStep = DRONE_MOVE_STEP * moveMultiplier
                    if (planetId == "p18") moveStep *= 1.5f // Cloud City: Drones 50% faster

                    when (nState) {
                        DroneState.IDLE -> {
                            val availableTarget = targets
                                .filter { target ->
                                    target.id !in claimedTargetIds &&
                                        (target.isMeteor || droneRarity.canCollect(target.rarity))
                                }
                                .maxByOrNull { if (it.isMeteor) Int.MAX_VALUE else it.rarity.ordinal }

                            if (availableTarget != null) {
                                nTargetId = availableTarget.id
                                nState = DroneState.MOVING_TO_DEBRIS
                                nPatrolTargetX = null
                                nPatrolTargetY = null
                                claimedTargetIds += availableTarget.id
                            } else {
                                if (nPatrolTargetX == null || nPatrolTargetY == null ||
                                    distanceSquared(nx, ny, nPatrolTargetX, nPatrolTargetY) <= moveStep * moveStep
                                ) {
                                    val patrolTarget = randomPatrolPoint()
                                    nPatrolTargetX = patrolTarget.first
                                    nPatrolTargetY = patrolTarget.second
                                }
                                val moved = movePatrolDrone(
                                    nx,
                                    ny,
                                    nPatrolTargetX ?: nx,
                                    nPatrolTargetY ?: ny,
                                    drone.id,
                                    moveStep
                                )
                                nx = moved.first
                                ny = moved.second
                            }
                        }
                        DroneState.MOVING_TO_DEBRIS -> {
                            val target = targets.find { it.id == drone.targetId }
                            if (target != null) {
                                val dx = target.x - nx
                                val dy = target.y - ny
                                val distSq = dx * dx + dy * dy
                                if (distSq <= moveStep * moveStep) {
                                    nx = target.x
                                    ny = target.y
                                    
                                    if (target.isMeteor) {
                                        // Rocky Bastion (p19): Immune to meteors
                                        if (planetId == "p19") {
                                            nState = DroneState.RETURNING
                                            nHasCargo = true
                                            nCargoRarity = Rarity.LEGENDARY
                                            nCargoReward = target.reward
                                        } else {
                                            // Magma S-15 (p9) or Sky Haven (p15): 70% success, others 50%
                                            val successChance = if (planetId == "p9" || planetId == "p15") 70 else 50
                                            if (randomProvider.nextInt(100) >= successChance) {
                                                nState = DroneState.BROKEN
                                                // Mech World (p11) or Sky Haven (p15): 20s repair, others 60s
                                                val repairDuration = if (planetId == "p11" || planetId == "p15") 20000L else 60000L
                                                nDisabledUntil = now + repairDuration
                                                nHasCargo = false
                                                nCargoRarity = null
                                                nCargoReward = 0.0
                                            } else {
                                                nState = DroneState.RETURNING
                                                nHasCargo = true
                                                nCargoRarity = Rarity.LEGENDARY
                                                nCargoReward = target.reward
                                            }
                                        }
                                    } else {
                                        nState = DroneState.RETURNING
                                        nHasCargo = true
                                        nCargoRarity = target.rarity
                                        nCargoReward = target.reward
                                    }
                                    targets.removeAll { it.id == target.id }
                                    nTargetId = null
                                } else {
                                    val dist = sqrt(distSq.toDouble()).toFloat()
                                    nx += (dx / dist) * moveStep
                                    ny += (dy / dist) * moveStep
                                }
                            } else {
                                nState = DroneState.RETURNING
                            }
                        }
                        DroneState.RETURNING -> {
                            val dx = DRONE_HOME_POSITION - nx
                            val dy = DRONE_HOME_POSITION - ny
                            val distSq = dx * dx + dy * dy
                            if (distSq <= moveStep * moveStep) {
                                nx = DRONE_HOME_POSITION
                                ny = DRONE_HOME_POSITION
                                if (nHasCargo) {
                                    debrisGained += nCargoReward
                                    debrisCollectedCount++
                                }
                                nState = DroneState.IDLE
                                nHasCargo = false
                                nCargoRarity = null
                                nCargoReward = 0.0
                            } else {
                                val dist = sqrt(distSq.toDouble()).toFloat()
                                nx += (dx / dist) * moveStep
                                ny += (dy / dist) * moveStep
                            }
                        }
                        else -> {}
                    }
                }
                drone.copy(
                    x = nx.coerceIn(0f, 1f),
                    y = ny.coerceIn(0f, 1f),
                    state = nState,
                    targetId = nTargetId,
                    hasCargo = nHasCargo,
                    cargoRarity = nCargoRarity,
                    cargoReward = nCargoReward,
                    patrolTargetX = nPatrolTargetX,
                    patrolTargetY = nPatrolTargetY,
                    disabledUntil = nDisabledUntil
                )
            }

            val updatedQuests = if (debrisCollectedCount > 0) {
                QuestEngine.advance(
                    state.activeQuests,
                    QuestType.COLLECT_DEBRIS,
                    debrisCollectedCount.toDouble()
                )
            } else state.activeQuests

            state.copy(
                drones = updatedDrones,
                scavengeTargets = targets,
                totalDebris = (state.totalDebris + debrisGained).coerceAtLeast(0.0),
                activeQuests = updatedQuests,
                sessionStats = state.sessionStats.copy(
                    debrisEarned = state.sessionStats.debrisEarned + debrisGained.coerceAtLeast(0.0)
                )
            )
        }
    }

    private fun randomPatrolPoint(): Pair<Float, Float> {
        while (true) {
            val x = randomProvider.nextFloat() * 0.9f + 0.05f
            val y = randomProvider.nextFloat() * 0.9f + 0.05f
            if (distanceSquared(x, y, DRONE_HOME_POSITION, DRONE_HOME_POSITION) > PLANET_AVOID_RADIUS_SQ) {
                return x to y
            }
        }
    }

    private fun movePatrolDrone(
        x: Float,
        y: Float,
        targetX: Float,
        targetY: Float,
        id: Long,
        step: Float = DRONE_PATROL_STEP
    ): Pair<Float, Float> {
        return DroneEngine.movePatrol(
            x, y, targetX, targetY, id, step, DRONE_HOME_POSITION, PLANET_AVOID_RADIUS_SQ
        )
    }

    private fun distanceSquared(x1: Float, y1: Float, x2: Float, y2: Float): Float =
        DroneEngine.distanceSquared(x1, y1, x2, y2)

    private fun processEconomyTick() {
        val passiveIncome = calculateDPS()
        val now = System.currentTimeMillis()
        _gameState.update {
            var next = EconomyEngine.processTick(it, now, passiveIncome)
            val battle = next.titanBattle
            if (battle != null) {
                next = if (now >= battle.expiresAt) next.copy(titanBattle = null)
                else {
                    val afterAbility = FeatureEngine.processBossAbility(next, now)
                    applyTitanDamage(afterAbility, passiveIncome * FeatureEngine.stationBossMultiplier(afterAbility))
                }
            }
            if (next.weeklyGalaxy.active && next.weeklyGalaxy.rule == WeeklyRule.FRAGILE_DRONES) {
                next = next.copy(weeklyGalaxy = next.weeklyGalaxy.copy(
                    progress = (next.weeklyGalaxy.progress + passiveIncome).coerceAtMost(next.weeklyGalaxy.target)
                ))
            }
            markCompletedQuestCycles(
                AchievementEngine.evaluate(next),
                now
            )
        }
        refreshTimedQuests(now)
    }

    fun calculateDPS(): Double {
        val state = _gameState.value
        if (state.weeklyGalaxy.active && state.weeklyGalaxy.rule == WeeklyRule.CLICKS_ONLY) return 0.0
        val weeklyMultiplier = if (state.weeklyGalaxy.active && state.weeklyGalaxy.rule == WeeklyRule.FRAGILE_DRONES) 0.5 else 1.0
        return EconomyBalance.passiveIncome(state, fleetById) *
            FeatureEngine.stationDpsMultiplier(state) * weeklyMultiplier
    }

    fun calculateClickValue(): Double {
        val state = _gameState.value
        val tradeMultiplier = if ((state.activeEffects[SkillType.TRADE_POWER.id] ?: 0L) > System.currentTimeMillis()) 2.0 else 1.0
        return EconomyEngine.calculateClickValue(state, clickItems, randomProvider) *
            MetaProgressEngine.collectionMultiplier(state.fleetCounts, fleetById) *
            MetaProgressEngine.masteryMultiplier(state.droneParts) *
            DroneTraitEngine.modifiers(state.activeFleetCounts).clickMultiplier *
            MetaProgressEngine.technologyMultiplier(state.technologies) *
            EconomyBalance.planetIncomeMultiplier(state.currentPlanetId) * tradeMultiplier *
            FeatureEngine.stationClickMultiplier(state)
    }

    fun onPlanetClick(): Double {
        val clickPower = calculateClickValue()
        _gameState.update { currentState ->
            var newTotalDebris = currentState.totalDebris
            var newHotelDebt = currentState.currentHotelDebt
            var hotelDebtActive = currentState.isHotelDebtActive
            
            if (hotelDebtActive) {
                val payment = GameRules.applyHotelDebtPayment(
                    totalDebris = newTotalDebris,
                    currentDebt = newHotelDebt,
                    clickIncome = clickPower
                )
                newTotalDebris = payment.totalDebris
                newHotelDebt = payment.remainingDebt
                hotelDebtActive = payment.isDebtActive
            } else {
                newTotalDebris += clickPower
            }
            
            // Update Quest Progress
            val updatedQuests = QuestEngine.advance(currentState.activeQuests, QuestType.CLICK_PLANET)
            
            var next = currentState.copy(
                totalDebris = newTotalDebris, 
                currentHotelDebt = newHotelDebt, 
                isHotelDebtActive = hotelDebtActive,
                activeQuests = updatedQuests,
                sessionStats = currentState.sessionStats.copy(
                    clicks = currentState.sessionStats.clicks + 1,
                    debrisEarned = currentState.sessionStats.debrisEarned + clickPower
                ),
                lifetimeStats = currentState.lifetimeStats.copy(
                    clicks = currentState.lifetimeStats.clicks + 1
                )
            )
            if (next.weeklyGalaxy.active && next.weeklyGalaxy.rule == WeeklyRule.CLICKS_ONLY) {
                next = next.copy(weeklyGalaxy = next.weeklyGalaxy.copy(
                    progress = (next.weeklyGalaxy.progress + 1.0).coerceAtMost(next.weeklyGalaxy.target)
                ))
            }
            if (next.titanBattle != null) next = applyTitanDamage(next, clickPower * FeatureEngine.stationBossMultiplier(next), manual = true)
            next
        }
        return clickPower
    }

    private fun applyTitanDamage(state: GameState, damage: Double, manual: Boolean = false): GameState {
        val battle = state.titanBattle ?: return state
        if (manual && battle.shieldCharges > 0) {
            return state.copy(titanBattle = battle.copy(shieldCharges = battle.shieldCharges - 1))
        }
        if (manual && battle.minions > 0) {
            return state.copy(titanBattle = battle.copy(minions = battle.minions - 1))
        }
        val challenge = FeatureEngine.challenge(battle.challengeId)
        val sourceMultiplier = if (manual) challenge.manualDamageMultiplier else challenge.fleetDamageMultiplier
        val minionBlock = if (!manual) (1.0 - battle.minions * 0.07).coerceAtLeast(0.15) else 1.0
        val health = battle.health - damage.coerceAtLeast(0.0) * sourceMultiplier * minionBlock
        return if (health > 0.0) state.copy(titanBattle = battle.copy(health = health))
        else challenge.let {
            state.copy(
                titanBattle = null,
                titanWins = state.titanWins + 1,
                completedChallengeIds = state.completedChallengeIds + battle.challengeId,
                prestigePoints = state.prestigePoints + it.rewardPrestige,
                totalDebris = state.totalDebris + it.rewardDebris * FeatureEngine.stationRewardMultiplier(state)
            )
        }
    }

    fun toggleWeeklyGalaxy() {
        _gameState.update { state ->
            val refreshed = FeatureEngine.refreshWeekly(state)
            refreshed.copy(weeklyGalaxy = refreshed.weeklyGalaxy.copy(active = !refreshed.weeklyGalaxy.active))
        }
        saveGameState()
    }

    fun claimWeeklyGalaxyReward() {
        _gameState.update { state ->
            val galaxy = state.weeklyGalaxy
            if (galaxy.rewardClaimed || galaxy.progress < galaxy.target) state else state.copy(
                weeklyGalaxy = galaxy.copy(rewardClaimed = true, active = false),
                prestigePoints = state.prestigePoints + 2,
                totalDebris = state.totalDebris + 250_000.0 * FeatureEngine.stationRewardMultiplier(state)
            )
        }
        saveGameState()
    }

    fun startTitanBattle(challengeId: ChallengeId = ChallengeId.VOID_LEVIATHAN) {
        _gameState.update { state ->
            if (state.titanBattle != null || !FeatureEngine.isChallengeUnlocked(state, challengeId)) state
            else state.copy(titanBattle = FeatureEngine.createBoss(state, challengeId))
        }
    }

    fun upgradeStation(module: StationModule) {
        updateStoreState("station:${module.name}") { state ->
            val level = state.stationLevels[module] ?: 0
            val cost = FeatureEngine.stationCost(module, level)
            if (level >= 5 || state.totalDebris < cost) null else state.copy(
                totalDebris = state.totalDebris - cost,
                stationLevels = state.stationLevels + (module to level + 1)
            )
        }
    }

    fun buyClickUpgrade(id: String) {
        val item = clickItems.find { it.id == id } ?: return
        updateStoreState("click:$id") { state ->
            val currentLevel = (state.clickLevels[id] ?: 0).coerceAtLeast(0)
            if (currentLevel == Int.MAX_VALUE) return@updateStoreState null

            val marketMultiplier = if (state.weeklyGalaxy.active && state.weeklyGalaxy.rule == WeeklyRule.VOLATILE_MARKET)
                FeatureEngine.volatilePriceMultiplier() else 1.0
            val rawCost = item.base * 1.15.pow(currentLevel.toDouble()) * marketMultiplier
            if (!rawCost.isFinite()) return@updateStoreState null
            val purchaseCost = rawCost.toLong().toDouble()
            if (state.totalDebris < purchaseCost) return@updateStoreState null

            val updatedQuests = QuestEngine.advance(state.activeQuests, QuestType.BUY_UPGRADE)

            var next = state.copy(
                totalDebris = state.totalDebris - purchaseCost,
                clickLevels = state.clickLevels + (id to currentLevel + 1),
                activeQuests = updatedQuests
            )
            if (next.weeklyGalaxy.active && next.weeklyGalaxy.rule == WeeklyRule.VOLATILE_MARKET) {
                next = next.copy(weeklyGalaxy = next.weeklyGalaxy.copy(
                    progress = (next.weeklyGalaxy.progress + 1.0).coerceAtMost(next.weeklyGalaxy.target)
                ))
            }
            next
        }
    }

    fun sellFleet(id: String) {
        val item = fleetItems.find { it.id == id } ?: return
        updateStoreState("sell:$id") { state ->
            val currentCount = state.fleetCounts[id] ?: 0
            if (currentCount <= 0) return@updateStoreState null

            val rawCost = item.base * 1.15.pow((currentCount - 1).toDouble())
            if (!rawCost.isFinite()) return@updateStoreState null
            val refund = rawCost.toLong().toDouble() / 2.0
            state.copy(
                totalDebris = state.totalDebris + refund,
                fleetCounts = state.fleetCounts + (id to currentCount - 1),
                activeFleetCounts = state.activeFleetCounts +
                    (id to (state.activeFleetCounts[id] ?: 0).coerceAtMost(currentCount - 1))
            )
        }
    }

    fun buyPlanet(planetId: String) {
        val config = planets[planetId] ?: return
        updateStoreState("planet:$planetId") { state ->
            GameRules.purchaseOrSelectPlanet(state, planetId, config.price)
        }
    }

    fun startOpeningCase(type: CaseType = CaseType.COMMON) {
        updateStoreState("case") { state ->
            val caseCost = calculateCaseCost(state.casesPurchased, type)
            if (state.isOpeningCase || state.totalDebris < caseCost) {
                return@updateStoreState null
            }
            state.copy(
                totalDebris = state.totalDebris - caseCost,
                isOpeningCase = true,
                openingCaseType = type,
                casesPurchased = state.casesPurchased + 1,
                lastDroppedDroneId = null
            )
        }
    }

    fun calculateCaseCost(casesPurchased: Int, type: CaseType = CaseType.COMMON): Double =
        GameRules.calculateCaseCost(casesPurchased, type)

    fun finishOpeningCase() {
        val caseType = _gameState.value.openingCaseType ?: CaseType.COMMON
        val selectedRarity = GameRules.rollCaseRarity(caseType, randomProvider.nextInt(100))
        val availableDrones = fleetItems.filter { it.rarity == selectedRarity }
        val selectedDrone = randomProvider.chooseOrNull(availableDrones) ?: fleetItems.first()
        val droneId = selectedDrone.id
        _gameState.update { state ->
            val updatedQuests = QuestEngine.advance(
                QuestEngine.advance(state.activeQuests, QuestType.OPEN_CASE),
                QuestType.OBTAIN_DRONE,
                droneId = droneId
            )
            val isNewDiscovery = droneId !in state.discoveredDroneIds
            val hasFleetSlot = state.fleetCounts.values.sum() < EconomyBalance.MAX_DRONES
            val updatedFleet = if (isNewDiscovery && hasFleetSlot) {
                state.fleetCounts + (droneId to ((state.fleetCounts[droneId] ?: 0) + 1))
            } else state.fleetCounts
            val updatedParts = if (isNewDiscovery && hasFleetSlot) {
                state.droneParts
            } else {
                state.droneParts + (droneId to ((state.droneParts[droneId] ?: 0) + 1))
            }
            val activeCount = state.activeFleetCounts.values.sum()
            val updatedActiveFleet = if (isNewDiscovery && hasFleetSlot && activeCount < DroneTraitEngine.MAX_ACTIVE_DRONES) {
                state.activeFleetCounts + (droneId to ((state.activeFleetCounts[droneId] ?: 0) + 1))
            } else state.activeFleetCounts
            state.copy(
                isOpeningCase = false, 
                openingCaseType = null,
                fleetCounts = updatedFleet,
                activeFleetCounts = updatedActiveFleet,
                discoveredDroneIds = state.discoveredDroneIds + droneId,
                droneParts = updatedParts,
                lastDroppedDroneId = droneId,
                activeQuests = updatedQuests,
                sessionStats = state.sessionStats.copy(casesOpened = state.sessionStats.casesOpened + 1),
                lifetimeStats = state.lifetimeStats.copy(
                    casesOpened = state.lifetimeStats.casesOpened + 1
                )
            ) 
        }
        saveGameState()
    }

    fun deployDrone(id: String) {
        updateStoreState("deploy:$id") { state ->
            val owned = state.fleetCounts[id] ?: 0
            val active = state.activeFleetCounts[id] ?: 0
            if (active > 0 || owned <= 0 || state.activeFleetCounts.values.sum() >= DroneTraitEngine.MAX_ACTIVE_DRONES) null
            else state.copy(activeFleetCounts = state.activeFleetCounts + (id to 1))
        }
    }

    fun recallDrone(id: String) {
        updateStoreState("recall:$id") { state ->
            val active = state.activeFleetCounts[id] ?: 0
            if (active <= 0) null
            else state.copy(activeFleetCounts = state.activeFleetCounts + (id to active - 1))
        }
    }

    fun collectionReward(milestone: Int): Double = EconomyBalance.scaledReward(
        when (milestone) {
            3 -> 10_000.0
            6 -> 50_000.0
            12 -> 250_000.0
            else -> 1_000_000.0
        },
        _gameState.value.currentPlanetId
    )

    fun claimCollectionReward(milestone: Int) {
        val key = milestone.toString()
        updateStoreState("collection:$key") { state ->
            if (state.discoveredDroneIds.size < milestone || key in state.claimedCollectionMilestones) {
                return@updateStoreState null
            }
            state.copy(
                totalDebris = state.totalDebris + collectionReward(milestone),
                claimedCollectionMilestones = state.claimedCollectionMilestones + key
            )
        }
    }

    fun claimQuestReward(questId: String) {
        _gameState.update { state ->
            val quest = state.activeQuests.find { it.id == questId } ?: return@update state
            if (!quest.isCompleted || quest.isClaimed) return@update state

            var newTotalDebris = state.totalDebris + quest.rewardDebris
            var newCasesPurchased = state.casesPurchased
            var triggeringCaseOpening = false
            
            if (quest.rewardCases > 0) {
                val totalDrones = state.fleetCounts.values.sum()
                if (totalDrones < EconomyBalance.MAX_DRONES) {
                    triggeringCaseOpening = true
                } else {
                    // Reward debris instead if drone limit reached
                    newTotalDebris += 25000.0 * quest.rewardCases
                }
            }
            
            var newFleetCounts = state.fleetCounts
            if (quest.rewardDroneId != null) {
                val totalDrones = state.fleetCounts.values.sum()
                if (totalDrones < EconomyBalance.MAX_DRONES) {
                    newFleetCounts = newFleetCounts + (quest.rewardDroneId to (newFleetCounts[quest.rewardDroneId] ?: 0) + 1)
                } else {
                    newTotalDebris += 50000.0
                }
            }

            val newActiveQuests = state.activeQuests.filter { it.id != questId }
            val newCompletedQuestIds = state.completedQuestIds + questId
            val now = System.currentTimeMillis()
            val dailyFinished = quest.cadence == QuestCadence.DAILY &&
                newActiveQuests.none { it.cadence == QuestCadence.DAILY }
            val weeklyFinished = quest.cadence == QuestCadence.WEEKLY &&
                newActiveQuests.none { it.cadence == QuestCadence.WEEKLY }

            state.copy(
                totalDebris = newTotalDebris,
                prestigePoints = state.prestigePoints + quest.rewardPrestigePoints,
                fleetCounts = newFleetCounts,
                activeQuests = newActiveQuests,
                dailyQuestsCompletedAt = if (dailyFinished && state.dailyQuestsCompletedAt < 0L) now else state.dailyQuestsCompletedAt,
                weeklyQuestsCompletedAt = if (weeklyFinished && state.weeklyQuestsCompletedAt < 0L) now else state.weeklyQuestsCompletedAt,
                completedQuestIds = newCompletedQuestIds,
                isOpeningCase = if (triggeringCaseOpening) true else state.isOpeningCase,
                // Cases from quests don't increase price growth in shop, but we could make them.
                // For now, let's keep them as a separate bonus.
                lastDroppedDroneId = if (triggeringCaseOpening) null else state.lastDroppedDroneId
            )
        }
        
        saveGameState()
    }

    fun clearReward() {
        _gameState.update { it.copy(lastDroppedDroneId = null) }
    }

    fun clearOfflineReward() {
        _gameState.update { it.copy(lastOfflineReward = 0.0) }
    }

    fun prestige() {
        updateStoreState("prestige") { state ->
            if (!EconomyBalance.canPrestige(state)) return@updateStoreState null
            val reward = EconomyBalance.prestigeReward(state)
            GameState(
                prestigePoints = state.prestigePoints + reward,
                technologies = state.technologies,
                completedQuestIds = state.completedQuestIds,
                lifetimeStats = state.lifetimeStats.copy(prestiges = state.lifetimeStats.prestiges + 1),
                unlockedAchievementIds = state.unlockedAchievementIds,
                claimedAchievementIds = state.claimedAchievementIds
            )
        }
    }

    fun buyTechnology(technology: Technology) {
        updateStoreState("technology:${technology.name}") { state ->
            if (technology in state.technologies || state.prestigePoints < technology.cost) null
            else state.copy(
                prestigePoints = state.prestigePoints - technology.cost,
                technologies = state.technologies + technology
            )
        }
    }

    fun claimAchievement(achievementId: String) {
        updateStoreState("achievement:$achievementId") { state ->
            AchievementEngine.claim(state, achievementId)
        }
    }

    private fun markCompletedQuestCycles(state: GameState, now: Long): GameState {
        val daily = state.activeQuests.filter { it.cadence == QuestCadence.DAILY }
        val weekly = state.activeQuests.filter { it.cadence == QuestCadence.WEEKLY }
        return state.copy(
            dailyQuestsCompletedAt = if (state.dailyQuestsCompletedAt < 0L && daily.isNotEmpty() && daily.all { it.isCompleted }) now else state.dailyQuestsCompletedAt,
            weeklyQuestsCompletedAt = if (state.weeklyQuestsCompletedAt < 0L && weekly.isNotEmpty() && weekly.all { it.isCompleted }) now else state.weeklyQuestsCompletedAt
        )
    }

    private fun refreshTimedQuests(now: Long = System.currentTimeMillis()) {
        val calendar = java.util.Calendar.getInstance()
        val dayKey = calendar.get(java.util.Calendar.YEAR) * 1_000L +
            calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val weekKey = calendar.getWeekYear() * 100L + calendar.get(java.util.Calendar.WEEK_OF_YEAR)
        _gameState.update { state ->
            var quests = state.activeQuests
            var dailyCompletedAt = state.dailyQuestsCompletedAt
            var weeklyCompletedAt = state.weeklyQuestsCompletedAt
            val hasDaily = quests.any { it.cadence == QuestCadence.DAILY }
            val hasWeekly = quests.any { it.cadence == QuestCadence.WEEKLY }
            if ((!hasDaily && dailyCompletedAt < 0L) ||
                (dailyCompletedAt > 0L && now - dailyCompletedAt >= DAILY_QUEST_COOLDOWN) ||
                quests.any { it.cadence == QuestCadence.DAILY && !it.id.startsWith("d2_") }
            ) {
                quests = quests.filterNot { it.cadence == QuestCadence.DAILY } +
                    createDailyQuests(dayKey, state.currentPlanetId)
                dailyCompletedAt = -1L
            }
            if ((!hasWeekly && weeklyCompletedAt < 0L) ||
                (weeklyCompletedAt > 0L && now - weeklyCompletedAt >= WEEKLY_QUEST_COOLDOWN) ||
                quests.any { it.cadence == QuestCadence.WEEKLY && !it.id.startsWith("w2_") }
            ) {
                quests = quests.filterNot { it.cadence == QuestCadence.WEEKLY } +
                    createWeeklyQuests(weekKey, state.currentPlanetId)
                weeklyCompletedAt = -1L
            }
            state.copy(
                activeQuests = quests,
                dailyQuestDay = dayKey,
                weeklyQuestWeek = weekKey,
                dailyQuestsCompletedAt = dailyCompletedAt,
                weeklyQuestsCompletedAt = weeklyCompletedAt
            )
        }
    }

    fun takeHotelDebt() {
        updateStoreState("hotel-debt") { state ->
            if (state.isHotelDebtActive) null else state.copy(
                totalDebris = state.totalDebris + GameRules.HOTEL_LOAN_AMOUNT,
                isHotelDebtActive = true,
                currentHotelDebt = GameRules.HOTEL_LOAN_AMOUNT
            )
        }
    }

    private fun updateStoreState(actionKey: String, transform: (GameState) -> GameState?) {
        synchronized(storeActionLock) {
            val now = System.nanoTime()
            val lastAction = lastStoreActionNanos[actionKey] ?: 0L
            if (now - lastAction < STORE_ACTION_DEBOUNCE_NANOS) return

            while (true) {
                val state = _gameState.value
                val updatedState = transform(state) ?: return
                if (_gameState.compareAndSet(state, updatedState)) {
                    lastStoreActionNanos[actionKey] = now
                    saveGameState()
                    return
                }
            }
        }
    }
}

data class ItemConfig(val id: String, val name: String, val base: Double, val value: Double, val iconRes: Int)
data class FleetConfig(val id: String, val name: String, val base: Double, val iconRes: Int, val spriteIndex: Int = -1, val rarity: Rarity = Rarity.COMMON)
data class PlanetConfig(val name: String, val price: Double, val desc: String, val color: Color, val imageRes: Int, val spriteIndex: Int = -1)

private fun Map<String, Int>.limitActiveFleet(): Map<String, Int> {
    var slotsLeft = DroneTraitEngine.MAX_ACTIVE_DRONES
    return entries.associate { (id, rawCount) ->
        val active = rawCount.coerceIn(0, 1).coerceAtMost(slotsLeft)
        slotsLeft -= active
        id to active
    }
}

private fun Map<String, Int>.limitOwnedFleet(): Map<String, Int> {
    var slotsLeft = EconomyBalance.MAX_DRONES
    return entries.associate { (id, rawCount) ->
        val owned = rawCount.coerceAtLeast(0).coerceAtMost(slotsLeft)
        slotsLeft -= owned
        id to owned
    }
}

private const val DRONE_MOVE_STEP = 0.02f
private const val DAILY_QUEST_COOLDOWN = 24L * 60L * 60L * 1_000L
private const val WEEKLY_QUEST_COOLDOWN = 7L * 24L * 60L * 60L * 1_000L
private const val DRONE_HOME_POSITION = 0.5f
private const val STORE_ACTION_DEBOUNCE_NANOS = 100_000_000L
private const val DEBRIS_SHOWER_SPAWN_INTERVAL_MS = 900L
private const val MAX_FALLING_DEBRIS = 12
private const val DRONE_PATROL_STEP = 0.0064f
private const val PLANET_AVOID_RADIUS = 0.18f
private const val PLANET_AVOID_RADIUS_SQ = PLANET_AVOID_RADIUS * PLANET_AVOID_RADIUS
private const val METEOR_SPAWN_CHANCE_PERCENT = 20
private const val SAVE_VERSION_KEY = "saveVersion"
private const val CURRENT_SAVE_VERSION = 1
private const val SAVE_INTERVAL_SECONDS = 15
private const val LAST_ACTIVE_AT_KEY = "lastActiveAt"
