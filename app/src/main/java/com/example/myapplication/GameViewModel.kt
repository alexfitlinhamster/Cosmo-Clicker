package com.example.myapplication

import android.app.Application
import android.content.Context
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
import kotlin.random.Random
import java.util.concurrent.atomic.AtomicLong

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
    private val storeActionLock = Any()
    private val lastStoreActionNanos = mutableMapOf<String, Long>()
    private val debrisId = AtomicLong(System.currentTimeMillis())

    private val droneNames = listOf(
        "Scrap-Bot", "Copper Cloud", "Rusty Rover", "Azure Ace", "Cobalt Collector",
        "Blue Beam", "Forest Phantom", "Jade Jumper", "Emerald Eye", "Crimson Crusher",
        "Ruby Reaper", "Solar Stinger", "Amber Apex", "Void Vulture", "Shadow Shifter",
        "Ghost Glider", "Plasma Prowler", "Neon Nibbler", "Cyber Cicada", "Titan Talon",
        "Iron Icarus", "Gold Guardian", "Gilded Golem", "Quartz Quill", "Silver Spectre",
        "Diamond Diver", "Onyx Orb", "Obsidian Owl", "Quantum Quark"
    )

    val clickItems = listOf(
        ItemConfig("magnet", "Plasma Magnet", 15.0, 1.0, R.drawable.upgrade_magnet),
        ItemConfig("torch", "Weld Torch", 200.0, 10.0, R.drawable.upgrade_weld_torch),
        ItemConfig("wrench", "Quantum Wrench", 5000.0, 100.0, R.drawable.upgrade_quantum_wrench),
        ItemConfig("harvester", "Debris Harvester", 80000.0, 750.0, R.drawable.upgrade_debris_harvester),
        ItemConfig("beacon", "Signal Beacon", 1500000.0, 6000.0, R.drawable.upgrade_signal_beacon)
    )

    val fleetItems = (1..29).map { i ->
        val drawableName = "drone_${i.toString().padStart(2, '0')}"
        val resId = application.resources.getIdentifier(drawableName, "drawable", application.packageName)
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
            iconRes = if (resId != 0) resId else R.drawable.upgrade_magnet,
            spriteIndex = -1,
            rarity = rarity
        )
    }

    val planets = mapOf(
        "p1" to PlanetConfig("Azurea", 0.0, "Home planet", Color(0xFF2196F3), R.drawable.planet_blue_01),
        "p2" to PlanetConfig("Canyon Prime", 10000.0, "Dry and windy world", Color(0xFFFFA726), R.drawable.planet_canyon_01),
        "p3" to PlanetConfig("Nebula Echo", 50000.0, "Glow of distant stars", Color(0xFF7E57C2), R.drawable.planet_cosmic_01),
        "p4" to PlanetConfig("Crystal Hearth", 250000.0, "Fragile beauty", Color(0xFF26C6DA), R.drawable.planet_crystal_01),
        "p5" to PlanetConfig("Dune Horizon", 1250000.0, "Endless sands", Color(0xFFFFCC80), R.drawable.planet_desert_01),
        "p6" to PlanetConfig("Volt Nova", 6250000.0, "World of electricity", Color(0xFFFFF176), R.drawable.planet_energy_01),
        "p7" to PlanetConfig("Gas Giant G-7", 31250000.0, "Dense atmosphere", Color(0xFF9CCC65), R.drawable.planet_gas_01),
        "p8" to PlanetConfig("Jungle Core", 156250000.0, "Wild nature", Color(0xFF43A047), R.drawable.planet_jungle_01),
        "p9" to PlanetConfig("Magma S-15", 781250000.0, "Burning abyss", Color(0xFFE53935), R.drawable.planet_lava_01),
        "p10" to PlanetConfig("Red Dust", 3906250000.0, "Ancient ruins", Color(0xFFFF7043), R.drawable.planet_mars_01),
        "p11" to PlanetConfig("Mech World X", 19531250000.0, "Factory complex", Color(0xFF78909C), R.drawable.planet_mech_01),
        "p12" to PlanetConfig("Luna Silvis", 97656250000.0, "Night guardian", Color(0xFFBDBDBD), R.drawable.planet_moon_01),
        "p13" to PlanetConfig("Abyss Ocean", 488281250000.0, "Deep sea", Color(0xFF1E88E5), R.drawable.planet_ocean_01),
        "p14" to PlanetConfig("Ring Oasis", 2441406250000.0, "Sky belt", Color(0xFFFFD54F), R.drawable.planet_ring_01),
        "p15" to PlanetConfig("Sky Haven", 12207031250000.0, "Above clouds", Color(0xFFE1F5FE), R.drawable.planet_sky_01),
        "p16" to PlanetConfig("Toxic Waste", 61035156250000.0, "Corrosive", Color(0xFF76FF03), R.drawable.planet_toxic_01),
        "p17" to PlanetConfig("Pink Nebula", 305175781250000.0, "Sweet shimmer", Color(0xFFF06292), R.drawable.planet_pink_01),
        "p18" to PlanetConfig("Cloud City", 1525878906250000.0, "Floating", Color(0xFF81D4FA), R.drawable.planet_cloud_01),
        "p19" to PlanetConfig("Rocky Bastion", 7629394531250000.0, "Stone fortress", Color(0xFF8D6E63), R.drawable.planet_rock_01),
        "p20" to PlanetConfig("Foggy Void", 38146972656250000.0, "Light disappears", Color(0xFF455A64), R.drawable.planet_fog_01)
    )

    private val _gameState = MutableStateFlow(loadGameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    init {
        startGameLoop()
        startEventLoop()
        startDroneLoop()
        startTrashSpawnLoop()
        startDebrisShowerLoop()
    }

    private fun loadGameState(): GameState {
        val clickLevels = mutableMapOf<String, Int>()
        clickItems.forEach { clickLevels[it.id] = prefs.getInt("click_${it.id}", 0) }
        
        val fleetCounts = mutableMapOf<String, Int>()
        fleetItems.forEach { fleetCounts[it.id] = prefs.getInt("fleet_${it.id}", 0) }

        val ownedPlanets = prefs.getStringSet("ownedPlanets", setOf("p1")) ?: setOf("p1")

        return GameState(
            totalDebris = loadPreciseDouble("totalDebris", 50.0),
            clickLevels = clickLevels,
            fleetCounts = fleetCounts,
            currentPlanetId = prefs.getString("currentPlanetId", "p1") ?: "p1",
            ownedPlanets = ownedPlanets,
            isHotelDebtActive = prefs.getBoolean("isHotelDebtActive", false),
            currentHotelDebt = loadPreciseDouble("currentHotelDebt", 0.0),
            casesPurchased = prefs.getInt("casesPurchased", fleetCounts.values.sum())
        )
    }

    private fun saveGameState() {
        val state = _gameState.value
        prefs.edit().apply {
            putLong("totalDebrisBits", state.totalDebris.toRawBits())
            state.clickLevels.forEach { (id, lvl) -> putInt("click_$id", lvl) }
            state.fleetCounts.forEach { (id, count) -> putInt("fleet_$id", count) }
            putString("currentPlanetId", state.currentPlanetId)
            putStringSet("ownedPlanets", state.ownedPlanets)
            putBoolean("isHotelDebtActive", state.isHotelDebtActive)
            putLong("currentHotelDebtBits", state.currentHotelDebt.toRawBits())
            putInt("casesPurchased", state.casesPurchased)
            apply()
        }
    }

    private fun loadPreciseDouble(key: String, defaultValue: Double): Double {
        val preciseKey = "${key}Bits"
        return if (prefs.contains(preciseKey)) {
            Double.fromBits(prefs.getLong(preciseKey, defaultValue.toRawBits()))
        } else {
            prefs.getFloat(key, defaultValue.toFloat()).toDouble()
        }
    }

    private fun startGameLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                processEconomyTick()
                saveGameState()
            }
        }
    }

    private fun startTrashSpawnLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(Random.nextLong(2000, 40000))
                
                _gameState.update { state ->
                    if (state.scavengeTargets.size < 3) {
                        val rarity = rollTrashRarity(state.currentPlanetId)
                        val newTarget = ScavengeTarget(
                            id = debrisId.incrementAndGet(),
                            x = Random.nextFloat(),
                            y = Random.nextFloat() * 0.6f + 0.1f,
                            rarity = rarity,
                            expiresAt = System.currentTimeMillis() + 60000,
                            imageIndex = debrisImageIndex(rarity),
                            reward = rollDebrisReward(rarity, state.currentPlanetId)
                        )
                        state.copy(scavengeTargets = state.scavengeTargets + newTarget)
                    } else state
                }
            }
        }
        
        viewModelScope.launch(Dispatchers.Default) {
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
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(DEBRIS_SHOWER_SPAWN_INTERVAL_MS)
                _gameState.update { state ->
                    if (state.activeEvent?.type != GameEventType.METEOR_SHOWER ||
                        state.scavengeTargets.count { it.isFalling } >= MAX_FALLING_DEBRIS
                    ) {
                        return@update state
                    }

                    val isMeteor = Random.nextInt(100) < METEOR_SPAWN_CHANCE_PERCENT
                    val rarity = if (isMeteor) Rarity.COMMON else rollTrashRarity(state.currentPlanetId)
                    state.copy(
                        scavengeTargets = state.scavengeTargets + ScavengeTarget(
                            id = debrisId.incrementAndGet(),
                            x = Random.nextFloat(),
                            y = -0.08f,
                            rarity = rarity,
                            expiresAt = state.activeEvent.expiresAt,
                            imageIndex = debrisImageIndex(rarity),
                            isFalling = true,
                            velocityX = Random.nextFloat() * 0.008f - 0.004f,
                            velocityY = Random.nextFloat() * 0.009f + 0.012f,
                            isMeteor = isMeteor,
                            reward = rollDebrisReward(if (isMeteor) Rarity.LEGENDARY else rarity, state.currentPlanetId)
                        )
                    )
                }
            }
        }
    }

    private fun debrisImageIndex(rarity: Rarity): Int = when (rarity) {
        Rarity.COMMON -> Random.nextInt(1, 3)
        Rarity.UNCOMMON -> 3
        Rarity.RARE -> 4
        Rarity.EPIC -> 5
        Rarity.LEGENDARY -> 6
    }

    private fun rollDebrisReward(rarity: Rarity, planetId: String): Double {
        var reward = Random.nextLong(rarity.minReward, rarity.maxReward + 1).toDouble()
        // Crystal Hearth (p4) or Sky Haven (p15): 50% chance to get +100% (x2) reward for debris
        if ((planetId == "p4" || planetId == "p15") && rarity != Rarity.LEGENDARY) {
            if (Random.nextInt(100) < 50) {
                reward *= 2.0
            }
        }
        // Pink Nebula (p17): +50% reward from all sources
        if (planetId == "p17") {
            reward *= 1.5
        }
        return reward
    }

    private fun rollTrashRarity(planetId: String): Rarity {
        // Red Dust (p10) or Sky Haven (p15): Epic/Legendary weight x2
        val weights = Rarity.entries.map { r ->
            if ((planetId == "p10" || planetId == "p15") && (r == Rarity.EPIC || r == Rarity.LEGENDARY)) r.spawnWeight * 2
            else r.spawnWeight
        }
        val totalWeight = weights.sum()
        val roll = Random.nextInt(totalWeight)
        var cumulative = 0
        for (i in Rarity.entries.indices) {
            cumulative += weights[i]
            if (roll < cumulative) return Rarity.entries[i]
        }
        return Rarity.COMMON
    }

    private fun startEventLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val planetId = _gameState.value.currentPlanetId
                var baseInterval = 15000 + Random.nextLong(30000)
                
                // Nebula Echo (p3): Events 30% more often
                if (planetId == "p3" || planetId == "p15") baseInterval = (baseInterval / 1.3).toLong()
                
                delay(baseInterval)
                
                if (_gameState.value.activeEvent == null) {
                    val durationBase = Random.nextLong(MIN_EVENT_DURATION_MS, MAX_EVENT_DURATION_MS + 1)
                    // Jungle Core (p8): Events duration x2
                    val durationMs = if ((planetId == "p8" || planetId == "p15")) durationBase * 2 else durationBase
                    
                    val eventTypes = GameEventType.entries.toMutableList()
                    
                    // Gas Giant (p7): Immune to CYBER_VIRUS
                    if (planetId == "p7" || planetId == "p15") eventTypes.remove(GameEventType.CYBER_VIRUS)
                    
                    var selectedType = eventTypes.random()
                    
                    // Luna Silvis (p12): -25% chance for negative events
                    if ((planetId == "p12" || planetId == "p15") && (selectedType == GameEventType.SOLAR_FLARE || selectedType == GameEventType.CYBER_VIRUS || selectedType == GameEventType.STORM)) {
                        if (Random.nextInt(100) < 25) {
                            selectedType = GameEventType.ASTEROID // Redirect to positive
                        }
                    }

                    // Toxic Waste (p16): 40% chance to dissolve negative events instantly
                    if (planetId == "p16" && (selectedType == GameEventType.SOLAR_FLARE || selectedType == GameEventType.CYBER_VIRUS || selectedType == GameEventType.STORM)) {
                        if (Random.nextInt(100) < 40) {
                            continue // Skip event
                        }
                    }

                    spawnEvent(selectedType, eventTitle(selectedType), durationMs)
                }
            }
        }
    }

    private fun eventTitle(type: GameEventType) = when(type) {
        GameEventType.STORM -> "Space Storm!"
        GameEventType.ASTEROID -> "Gold Asteroid!"
        GameEventType.METEOR_SHOWER -> "Debris Shower!"
        GameEventType.BLACK_HOLE -> "Black Hole!"
        GameEventType.SOLAR_FLARE -> "Solar Flare!"
        GameEventType.CYBER_VIRUS -> "Cyber Virus!"
    }

    private fun spawnEvent(type: GameEventType, title: String, durationMs: Long) {
        val x = Random.nextFloat()
        val y = Random.nextFloat() * 0.6f + 0.1f
        _gameState.update { state ->
            var infectedId: Long? = null
            if (type == GameEventType.CYBER_VIRUS && state.drones.isNotEmpty()) {
                infectedId = state.drones.filter { it.state != DroneState.BROKEN }.randomOrNull()?.id
            }

            state.copy(
                activeEvent = GameEvent(type, title, System.currentTimeMillis() + durationMs, x, y),
                eventMultiplier = if (type == GameEventType.STORM || type == GameEventType.SOLAR_FLARE) 3.0 else 1.0,
                eventTapsLeft = if (type == GameEventType.BLACK_HOLE) 10 else 0,
                infectedDroneId = infectedId
            )
        }
        
        viewModelScope.launch {
            delay(durationMs)
            _gameState.update { 
                if (it.activeEvent?.expiresAt ?: 0 <= System.currentTimeMillis()) {
                    it.copy(activeEvent = null, eventMultiplier = 1.0, infectedDroneId = null)
                } else it
            }
        }
    }

    fun onAsteroidClick() {
        _gameState.update { state ->
            if (state.activeEvent?.type == GameEventType.ASTEROID) {
                val bonus = 500.0 * state.eventMultiplier
                state.copy(totalDebris = state.totalDebris + bonus, activeEvent = null)
            } else state
        }
    }
    
    fun onBlackHoleClick() {
        _gameState.update { state ->
            if (state.activeEvent?.type == GameEventType.BLACK_HOLE) {
                val taps = state.eventTapsLeft - 1
                if (taps <= 0) {
                    val targets = state.scavengeTargets.toMutableList()
                    // Abyss Ocean (p13) or Sky Haven (p15) reward: 5 rare items
                    if (state.currentPlanetId == "p13" || state.currentPlanetId == "p15") {
                        repeat(5) {
                            targets.add(ScavengeTarget(
                                id = debrisId.incrementAndGet(),
                                x = state.activeEvent.x + (Random.nextFloat() - 0.5f) * 0.2f,
                                y = state.activeEvent.y + (Random.nextFloat() - 0.5f) * 0.2f,
                                rarity = Rarity.RARE,
                                expiresAt = System.currentTimeMillis() + 30000,
                                imageIndex = debrisImageIndex(Rarity.RARE),
                                reward = rollDebrisReward(Rarity.RARE, state.currentPlanetId)
                            ))
                        }
                    }
                    state.copy(activeEvent = null, eventTapsLeft = 0, scavengeTargets = targets)
                } else {
                    state.copy(eventTapsLeft = taps)
                }
            } else state
        }
    }

    fun onDroneClick(droneId: Long) {
        _gameState.update { state ->
            if (state.infectedDroneId == droneId) {
                state.copy(infectedDroneId = null, activeEvent = null)
            } else state
        }
    }

    private fun startDroneLoop() {
        viewModelScope.launch(Dispatchers.Default) {
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
        val fleetCounts = currentState.fleetCounts
        val activeEvent = currentState.activeEvent
        val isBlackHole = activeEvent?.type == GameEventType.BLACK_HOLE
        val isSolarFlare = activeEvent?.type == GameEventType.SOLAR_FLARE
        val infectedId = currentState.infectedDroneId
        val bhX = activeEvent?.x ?: 0.5f
        val bhY = activeEvent?.y ?: 0.5f
        
        _gameState.update { state ->
            val drones = state.drones.toMutableList()
            
            // Sync drones list with fleet counts
            fleetItems.forEach { item ->
                val count = fleetCounts[item.id] ?: 0
                val currentOfThisType = drones.filter { it.type == item.id }
                if (currentOfThisType.size < count) {
                    repeat(count - currentOfThisType.size) {
                        val spawn = randomPatrolPoint()
                        drones.add(DroneData(Random.nextLong(), spawn.first, spawn.second, type = item.id))
                    }
                } else if (currentOfThisType.size > count) {
                    val toRemove = currentOfThisType.size - count
                    repeat(toRemove) {
                        val droneToRemove = drones.find { it.type == item.id }
                        if (droneToRemove != null) drones.remove(droneToRemove)
                    }
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

            val updatedDrones = drones.map { drone ->
                if (drone.state == DroneState.BROKEN) {
                    if (now >= drone.disabledUntil) {
                        return@map drone.copy(state = DroneState.IDLE, disabledUntil = 0)
                    }
                    return@map drone
                }

                var moveStep = DRONE_MOVE_STEP
                if (planetId == "p18") moveStep *= 1.5f // Cloud City: Drones 50% faster

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

                val droneConfig = fleetItems.find { it.id == drone.type }
                val droneRarity = droneConfig?.rarity ?: Rarity.COMMON

                if (isBlackHole) {
                    val dx = bhX - nx
                    val dy = bhY - ny
                    val distSq = dx * dx + dy * dy
                    if (distSq > 0.001f) {
                        val dist = sqrt(distSq.toDouble()).toFloat()
                        nx += (dx / dist) * 0.005f
                        ny += (dy / dist) * 0.005f
                    }
                    nState = DroneState.SUCKED_IN
                } else if (isSolarFlare) {
                    nx += Random.nextFloat() * 0.01f - 0.005f
                    ny += Random.nextFloat() * 0.01f - 0.005f
                    nState = DroneState.JAMMED
                } else if (drone.id == infectedId) {
                    nState = DroneState.INFECTED
                    nx += Random.nextFloat() * 0.04f - 0.02f
                    ny += Random.nextFloat() * 0.04f - 0.02f
                    debrisGained -= 10.0 // Steal debris
                } else {
                    if (nState == DroneState.SUCKED_IN || nState == DroneState.JAMMED || nState == DroneState.INFECTED) nState = DroneState.IDLE
                    
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
                                            if (Random.nextInt(100) >= successChance) {
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

            state.copy(
                drones = updatedDrones,
                scavengeTargets = targets,
                totalDebris = (state.totalDebris + debrisGained).coerceAtLeast(0.0)
            )
        }
    }

    private fun randomPatrolPoint(): Pair<Float, Float> {
        while (true) {
            val x = Random.nextFloat() * 0.9f + 0.05f
            val y = Random.nextFloat() * 0.9f + 0.05f
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
        val dx = targetX - x
        val dy = targetY - y
        val distance = sqrt(dx * dx + dy * dy)
        if (distance <= step) return targetX to targetY

        var stepX = dx / distance * step
        var stepY = dy / distance * step
        if (distanceSquared(
                x + stepX,
                y + stepY,
                DRONE_HOME_POSITION,
                DRONE_HOME_POSITION
            ) < PLANET_AVOID_RADIUS_SQ
        ) {
            val radialX = x - DRONE_HOME_POSITION
            val radialY = y - DRONE_HOME_POSITION
            val clockwise = if (id and 1L == 0L) 1f else -1f
            val radialLength = sqrt(radialX * radialX + radialY * radialY).coerceAtLeast(0.001f)
            stepX = -radialY / radialLength * step * clockwise
            stepY = radialX / radialLength * step * clockwise
        }
        return (x + stepX) to (y + stepY)
    }

    private fun distanceSquared(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return dx * dx + dy * dy
    }

    private fun processEconomyTick() {
        _gameState.update { currentState ->
            currentState
        }
    }

    fun calculateDPS(): Double = 0.0 // Пассивный доход убран по ТЗ

    fun calculateClickValue(): Double {
        val state = _gameState.value
        var v = 1.0 + clickItems.sumOf { (state.clickLevels[it.id] ?: 0) * it.value }
        
        when (state.currentPlanetId) {
            "p1" -> if (Random.nextInt(100) < 15) v *= 2.0 // Azurea
            "p2" -> v *= 1.2 // Canyon Prime
            "p5" -> v += 15.0 // Dune Horizon
            "p6" -> if (Random.nextInt(100) < 30) v *= 4.0 // Volt Nova
            "p14" -> if (state.activeEvent != null) v *= 2.0 // Ring Oasis
            "p20" -> if (Random.nextInt(100) < 10) v *= 10.0 // Foggy Void: x10 Critical
            "p15" -> { // Sky Haven: Hybrid bonus
                if (Random.nextInt(100) < 25) v *= 3.0
                v += 20.0
                if (state.activeEvent != null) v *= 1.5
            }
        }
        v *= state.eventMultiplier
        return v
    }

    fun onPlanetClick(): Double {
        val clickPower = calculateClickValue()
        _gameState.update { currentState ->
            var newTotalDebris = currentState.totalDebris
            var newHotelDebt = currentState.currentHotelDebt
            var hotelDebtActive = currentState.isHotelDebtActive
            
            if (hotelDebtActive) {
                val debtPayment = minOf(clickPower * HOTEL_DEBT_PAYMENT_SHARE, newHotelDebt)
                val actualIncome = clickPower - debtPayment
                newHotelDebt -= debtPayment
                newTotalDebris += actualIncome
                if (newHotelDebt <= 0) {
                    newHotelDebt = 0.0
                    hotelDebtActive = false
                }
            } else {
                newTotalDebris += clickPower
            }
            currentState.copy(totalDebris = newTotalDebris, currentHotelDebt = newHotelDebt, isHotelDebtActive = hotelDebtActive)
        }
        return clickPower
    }

    fun buyClickUpgrade(id: String) {
        val item = clickItems.find { it.id == id } ?: return
        updateStoreState("click:$id") { state ->
            val currentLevel = (state.clickLevels[id] ?: 0).coerceAtLeast(0)
            if (currentLevel == Int.MAX_VALUE) return@updateStoreState null

            val rawCost = item.base * 1.15.pow(currentLevel.toDouble())
            if (!rawCost.isFinite()) return@updateStoreState null
            val purchaseCost = rawCost.toLong().toDouble()
            if (state.totalDebris < purchaseCost) return@updateStoreState null

            state.copy(
                totalDebris = state.totalDebris - purchaseCost,
                clickLevels = state.clickLevels + (id to currentLevel + 1)
            )
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
                fleetCounts = state.fleetCounts + (id to currentCount - 1)
            )
        }
    }

    fun buyPlanet(planetId: String) {
        val config = planets[planetId] ?: return
        updateStoreState("planet:$planetId") { state ->
            when {
                state.currentPlanetId == planetId -> null
                state.ownedPlanets.contains(planetId) -> state.copy(currentPlanetId = planetId)
                state.totalDebris >= config.price -> state.copy(
                    totalDebris = state.totalDebris - config.price,
                    currentPlanetId = planetId,
                    ownedPlanets = state.ownedPlanets + planetId
                )
                else -> null
            }
        }
    }

    fun startOpeningCase() {
        updateStoreState("case") { state ->
            val totalDrones = state.fleetCounts.values.sum()
            val caseCost = calculateCaseCost(state.casesPurchased)
            if (state.isOpeningCase || totalDrones >= 5 || state.totalDebris < caseCost) {
                return@updateStoreState null
            }
            state.copy(
                totalDebris = state.totalDebris - caseCost,
                isOpeningCase = true,
                casesPurchased = state.casesPurchased + 1,
                lastDroppedDroneId = null
            )
        }
    }

    fun calculateCaseCost(casesPurchased: Int): Double =
        CASE_BASE_COST * CASE_COST_MULTIPLIER.pow(casesPurchased.coerceAtLeast(0).toDouble())

    fun finishOpeningCase() {
        val randomRoll = Random.nextInt(100)
        var accumulatedChance = 0
        var selectedRarity = Rarity.COMMON
        for (rarity in Rarity.entries.reversed()) {
            accumulatedChance += rarity.spawnWeight
            if (randomRoll < accumulatedChance) {
                selectedRarity = rarity
                break
            }
        }
        val availableDrones = fleetItems.filter { it.rarity == selectedRarity }
        val selectedDrone = availableDrones.randomOrNull() ?: fleetItems.first()
        val droneId = selectedDrone.id
        val currentCount = _gameState.value.fleetCounts[droneId] ?: 0
        _gameState.update { it.copy(isOpeningCase = false, fleetCounts = it.fleetCounts + (droneId to currentCount + 1), lastDroppedDroneId = droneId) }
        saveGameState()
    }

    fun clearReward() {
        _gameState.update { it.copy(lastDroppedDroneId = null) }
    }

    fun takeHotelDebt() {
        updateStoreState("hotel-debt") { state ->
            if (state.isHotelDebtActive) null else state.copy(
                totalDebris = state.totalDebris + HOTEL_LOAN_AMOUNT,
                isHotelDebtActive = true,
                currentHotelDebt = HOTEL_LOAN_AMOUNT
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

private const val DRONE_MOVE_STEP = 0.025f
private const val DRONE_HOME_POSITION = 0.5f
private const val STORE_ACTION_DEBOUNCE_NANOS = 100_000_000L
private const val CASE_BASE_COST = 1000.0
private const val CASE_COST_MULTIPLIER = 1.2
private const val HOTEL_LOAN_AMOUNT = 1_000_000.0
private const val HOTEL_DEBT_PAYMENT_SHARE = 0.3
private const val MIN_EVENT_DURATION_MS = 20_000L
private const val MAX_EVENT_DURATION_MS = 60_000L
private const val DEBRIS_SHOWER_SPAWN_INTERVAL_MS = 450L
private const val MAX_FALLING_DEBRIS = 12
private const val DRONE_PATROL_STEP = 0.008f
private const val PLANET_AVOID_RADIUS = 0.18f
private const val PLANET_AVOID_RADIUS_SQ = PLANET_AVOID_RADIUS * PLANET_AVOID_RADIUS
private const val METEOR_SPAWN_CHANCE_PERCENT = 20
