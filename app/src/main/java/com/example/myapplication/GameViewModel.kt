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
        ItemConfig("magnet", "Plasma Magnet", 15.0, 1.0, R.drawable.magnet),
        ItemConfig("torch", "Weld Torch", 200.0, 10.0, R.drawable.torch),
        ItemConfig("wrench", "Quantum Wrench", 5000.0, 100.0, R.drawable.wrench),
        ItemConfig("harvester", "Debris Harvester", 80000.0, 750.0, R.drawable.harvester),
        ItemConfig("beacon", "Signal Beacon", 1500000.0, 6000.0, R.drawable.beacon)
    )

    val fleetItems = (1..29).map { i ->
        val resId = application.resources.getIdentifier("dron$i", "drawable", application.packageName)
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
            iconRes = if (resId != 0) resId else R.drawable.magnet, 
            spriteIndex = -1,
            rarity = rarity
        )
    }

    val planets = mapOf(
        "p1" to PlanetConfig("Sylva", 0.0, "Forest World", Color(0xFF2E7D32), R.drawable.planet1),
        "p2" to PlanetConfig("Oceania", 10000.0, "Water World", Color(0xFF1976D2), R.drawable.planet2),
        "p3" to PlanetConfig("Ignis", 50000.0, "Volcanic", Color(0xFFD32F2F), R.drawable.planet3),
        "p4" to PlanetConfig("Glacies", 250000.0, "Ice World", Color(0xFF00BCD4), R.drawable.planet4),
        "p5" to PlanetConfig("Aurea", 1000000.0, "Gold Veins", Color(0xFFFBC02D), R.drawable.planet5),
        "p6" to PlanetConfig("Toxis", 5000000.0, "Toxic Gas", Color(0xFF388E3C), R.drawable.planet6),
        "p7" to PlanetConfig("Exo-Prime", 25000000.0, "Advanced", Color(0xFF7B1FA2), R.drawable.planet7),
        "p8" to PlanetConfig("Void-9", 100000000.0, "Dark Matter", Color(0xFF212121), R.drawable.planet8)
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
            totalDebris = prefs.getFloat("totalDebris", 50f).toDouble(),
            clickLevels = clickLevels,
            fleetCounts = fleetCounts,
            currentPlanetId = prefs.getString("currentPlanetId", "p1") ?: "p1",
            ownedPlanets = ownedPlanets,
            isHotelDebtActive = prefs.getBoolean("isHotelDebtActive", false),
            currentHotelDebt = prefs.getFloat("currentHotelDebt", 0f).toDouble()
        )
    }

    private fun saveGameState() {
        val state = _gameState.value
        prefs.edit().apply {
            putFloat("totalDebris", state.totalDebris.toFloat())
            state.clickLevels.forEach { (id, lvl) -> putInt("click_$id", lvl) }
            state.fleetCounts.forEach { (id, count) -> putInt("fleet_$id", count) }
            putString("currentPlanetId", state.currentPlanetId)
            putStringSet("ownedPlanets", state.ownedPlanets)
            putBoolean("isHotelDebtActive", state.isHotelDebtActive)
            putFloat("currentHotelDebt", state.currentHotelDebt.toFloat())
            apply()
        }
    }

    private fun startGameLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                // Passive income for drones removed per TZ. 
                // Only planet bonus (if any) or debt processing remains.
                processEconomyTick()
                saveGameState()
            }
        }
    }

    private fun startTrashSpawnLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                // Рандомный интервал 2-40 сек
                delay(Random.nextLong(2000, 40000))
                
                _gameState.update { state ->
                    if (state.scavengeTargets.size < 3) {
                        val rarity = rollTrashRarity()
                        val newTarget = ScavengeTarget(
                            id = debrisId.incrementAndGet(),
                            x = Random.nextFloat(),
                            y = Random.nextFloat() * 0.6f + 0.1f, // Не спавним слишком низко/высоко
                            rarity = rarity,
                            expiresAt = System.currentTimeMillis() + 60000,
                            imageIndex = debrisImageIndex(rarity)
                        )
                        state.copy(scavengeTargets = state.scavengeTargets + newTarget)
                    } else state
                }
            }
        }
        
        // Цикл очистки просроченного мусора
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

                    val rarity = rollTrashRarity()
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
                            velocityY = Random.nextFloat() * 0.009f + 0.012f
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

    private fun rollTrashRarity(): Rarity {
        val roll = Random.nextInt(100)
        var cumulative = 0
        for (r in Rarity.entries) {
            cumulative += r.spawnWeight
            if (roll < cumulative) return r
        }
        return Rarity.COMMON
    }

    private fun startEventLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(15000 + Random.nextLong(30000))
                if (_gameState.value.activeEvent == null) {
                    val durationMs = Random.nextLong(MIN_EVENT_DURATION_MS, MAX_EVENT_DURATION_MS + 1)
                    when (Random.nextInt(GameEventType.entries.size)) {
                        0 -> spawnEvent(GameEventType.STORM, "Space Storm!", durationMs)
                        1 -> spawnEvent(GameEventType.ASTEROID, "Gold Asteroid!", durationMs)
                        2 -> spawnEvent(GameEventType.PIRATES, "Pirates!", durationMs)
                        else -> spawnEvent(GameEventType.METEOR_SHOWER, "Debris Shower!", durationMs)
                    }
                }
            }
        }
    }

    private fun spawnEvent(type: GameEventType, title: String, durationMs: Long) {
        val x = Random.nextFloat()
        val y = Random.nextFloat() * 0.6f + 0.1f
        _gameState.update { it.copy(
            activeEvent = GameEvent(type, title, System.currentTimeMillis() + durationMs, x, y),
            eventMultiplier = if (type == GameEventType.STORM) 3.0 else 1.0,
            pirateTapsLeft = if (type == GameEventType.PIRATES) 5 else 0
        ) }
        
        viewModelScope.launch {
            delay(durationMs)
            _gameState.update { 
                if (it.activeEvent?.expiresAt ?: 0 <= System.currentTimeMillis()) {
                    it.copy(activeEvent = null, eventMultiplier = 1.0)
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

    fun onPirateClick() {
        _gameState.update { state ->
            if (state.activeEvent?.type == GameEventType.PIRATES) {
                val taps = state.pirateTapsLeft - 1
                if (taps <= 0) {
                    state.copy(activeEvent = null, pirateTapsLeft = 0)
                } else {
                    state.copy(pirateTapsLeft = taps)
                }
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
        val state = _gameState.value
        val fleetCounts = state.fleetCounts
        
        _gameState.update { currentState ->
            val drones = currentState.drones.toMutableList()
            
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

            val targets = currentState.scavengeTargets.mapNotNull { target ->
                if (!target.isFalling) return@mapNotNull target
                target.copy(
                    x = target.x + target.velocityX,
                    y = target.y + target.velocityY
                ).takeIf { it.x in -0.15f..1.15f && it.y <= 1.1f }
            }.toMutableList()
            if (drones.isEmpty()) return@update currentState.copy(scavengeTargets = targets)

            var debrisGained = 0.0
            val claimedTargetIds = drones.mapNotNullTo(mutableSetOf()) { it.targetId }

            val updatedDrones = drones.map { drone ->
                var nx = drone.x
                var ny = drone.y
                var nState = drone.state
                var nTargetId = drone.targetId
                var nHasCargo = drone.hasCargo
                var nCargoRarity = drone.cargoRarity
                var nPatrolTargetX = drone.patrolTargetX
                var nPatrolTargetY = drone.patrolTargetY
                
                val droneConfig = fleetItems.find { it.id == drone.type }
                val droneRarity = droneConfig?.rarity ?: Rarity.COMMON

                when (drone.state) {
                    DroneState.IDLE -> {
                        // Ищем самый редкий доступный мусор
                        val availableTarget = targets
                            .filter { t -> t.id !in claimedTargetIds && droneRarity.canCollect(t.rarity) }
                            .maxByOrNull { it.rarity.ordinal }
                        
                        if (availableTarget != null) {
                            nTargetId = availableTarget.id
                            nState = DroneState.MOVING_TO_DEBRIS
                            nPatrolTargetX = null
                            nPatrolTargetY = null
                            claimedTargetIds += availableTarget.id
                        } else {
                            if (nPatrolTargetX == null || nPatrolTargetY == null ||
                                distanceSquared(nx, ny, nPatrolTargetX, nPatrolTargetY) <= DRONE_MOVE_STEP * DRONE_MOVE_STEP
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
                                drone.id
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
                            if (distSq <= DRONE_MOVE_STEP * DRONE_MOVE_STEP) {
                                nx = target.x
                                ny = target.y
                                nState = DroneState.RETURNING
                                nHasCargo = true
                                nCargoRarity = target.rarity
                                targets.removeAll { it.id == target.id }
                                nTargetId = null
                            } else {
                                val dist = Math.sqrt(distSq.toDouble()).toFloat()
                                nx += (dx / dist) * DRONE_MOVE_STEP
                                ny += (dy / dist) * DRONE_MOVE_STEP
                            }
                        } else {
                            nState = DroneState.RETURNING
                        }
                    }
                    DroneState.RETURNING -> {
                        val dx = DRONE_HOME_POSITION - nx
                        val dy = DRONE_HOME_POSITION - ny
                        val distSq = dx * dx + dy * dy
                        if (distSq <= DRONE_MOVE_STEP * DRONE_MOVE_STEP) {
                            nx = DRONE_HOME_POSITION
                            ny = DRONE_HOME_POSITION
                            if (nHasCargo && nCargoRarity != null) {
                                // The collected debris determines the entire reward.
                                debrisGained += nCargoRarity.debrisReward
                            }
                            nState = DroneState.IDLE
                            nHasCargo = false
                            nCargoRarity = null
                        } else {
                            val dist = Math.sqrt(distSq.toDouble()).toFloat()
                            nx += (dx / dist) * DRONE_MOVE_STEP
                            ny += (dy / dist) * DRONE_MOVE_STEP
                        }
                    }
                }
                drone.copy(
                    x = nx.coerceIn(0f, 1f),
                    y = ny.coerceIn(0f, 1f),
                    state = nState,
                    targetId = nTargetId,
                    hasCargo = nHasCargo,
                    cargoRarity = nCargoRarity,
                    patrolTargetX = nPatrolTargetX,
                    patrolTargetY = nPatrolTargetY
                )
            }

            currentState.copy(
                drones = updatedDrones,
                scavengeTargets = targets,
                totalDebris = currentState.totalDebris + debrisGained
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
        id: Long
    ): Pair<Float, Float> {
        val dx = targetX - x
        val dy = targetY - y
        val distance = sqrt(dx * dx + dy * dy)
        if (distance <= DRONE_PATROL_STEP) return targetX to targetY

        var stepX = dx / distance * DRONE_PATROL_STEP
        var stepY = dy / distance * DRONE_PATROL_STEP
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
            stepX = -radialY / radialLength * DRONE_PATROL_STEP * clockwise
            stepY = radialX / radialLength * DRONE_PATROL_STEP * clockwise
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
            var newTotalDebris = currentState.totalDebris
            var newHotelDebt = currentState.currentHotelDebt
            var hotelDebtActive = currentState.isHotelDebtActive
            
            // Если активен ивент с пиратами, они крадут 0.1% каждую секунду
            if (currentState.activeEvent?.type == GameEventType.PIRATES) newTotalDebris *= 0.999

            if (hotelDebtActive) {
                // Долг выплачивается только с активных действий теперь? 
                // Оставим просто обработку остатка если нужно.
            }
            currentState.copy(totalDebris = newTotalDebris, currentHotelDebt = newHotelDebt, isHotelDebtActive = hotelDebtActive)
        }
    }

    fun calculateDPS(): Double = 0.0 // Пассивный доход убран по ТЗ

    fun calculateClickValue(): Double {
        val state = _gameState.value
        var v = 1.0 + clickItems.sumOf { (state.clickLevels[it.id] ?: 0) * it.value }
        when (state.currentPlanetId) {
            "p3" -> v += 5.0
            "p2" -> v *= 1.2
            "p8" -> v *= 2.0
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
                val debtPayment = clickPower * 0.3
                val actualIncome = clickPower * 0.7
                newHotelDebt -= debtPayment
                newTotalDebris += actualIncome
                if (newHotelDebt <= 0) {
                    newTotalDebris = 0.0
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
            if (state.isOpeningCase || totalDrones >= 5 || state.totalDebris < CASE_COST) {
                return@updateStoreState null
            }
            state.copy(
                totalDebris = state.totalDebris - CASE_COST,
                isOpeningCase = true,
                lastDroppedDroneId = null
            )
        }
    }

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
                isHotelDebtActive = true,
                currentHotelDebt = 1000000.0
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
private const val CASE_COST = 1000.0
private const val MIN_EVENT_DURATION_MS = 20_000L
private const val MAX_EVENT_DURATION_MS = 60_000L
private const val DEBRIS_SHOWER_SPAWN_INTERVAL_MS = 450L
private const val MAX_FALLING_DEBRIS = 12
private const val DRONE_PATROL_STEP = 0.008f
private const val PLANET_AVOID_RADIUS = 0.22f
private const val PLANET_AVOID_RADIUS_SQ = PLANET_AVOID_RADIUS * PLANET_AVOID_RADIUS
