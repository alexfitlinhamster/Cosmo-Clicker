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
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)

    val clickItems = listOf(
        ItemConfig("magnet", "Plasma Magnet", 15.0, 1.0, R.drawable.magnet),
        ItemConfig("torch", "Weld Torch", 200.0, 10.0, R.drawable.torch),
        ItemConfig("wrench", "Quantum Wrench", 5000.0, 100.0, R.drawable.wrench),
        ItemConfig("harvester", "Debris Harvester", 80000.0, 750.0, R.drawable.harvester),
        ItemConfig("beacon", "Signal Beacon", 1500000.0, 6000.0, R.drawable.beacon)
    )

    val fleetItems = (1..29).map { i ->
        val resId = application.resources.getIdentifier("dron$i", "drawable", application.packageName)
        FleetConfig(
            id = "drone_$i",
            name = "Drone #$i",
            base = 10.0 * 1.8.pow(i.toDouble() - 1),
            rate = 0.5 * 2.1.pow(i.toDouble() - 1),
            iconRes = if (resId != 0) resId else R.drawable.magnet, 
            spriteIndex = -1 
        )
    }

    // Keeping ONLY the 8 new planets as requested
    val planets = mapOf(
        "p1" to PlanetConfig("Sylva", 0.0, "Forest World", Color(0xFF2E7D32), R.drawable.planet1, -1),
        "p2" to PlanetConfig("Oceania", 25000.0, "Water World", Color(0xFF1976D2), R.drawable.planet2, -1),
        "p3" to PlanetConfig("Ignis", 150000.0, "Volcanic", Color(0xFFD32F2F), R.drawable.planet3, -1),
        "p4" to PlanetConfig("Glacies", 1000000.0, "Ice World", Color(0xFF00BCD4), R.drawable.planet4, -1),
        "p5" to PlanetConfig("Aurea", 10000000.0, "Gold Veins", Color(0xFFFBC02D), R.drawable.planet5, -1),
        "p6" to PlanetConfig("Toxis", 50000000.0, "Toxic Gas", Color(0xFF388E3C), R.drawable.planet6, -1),
        "p7" to PlanetConfig("Exo-Prime", 250000000.0, "Advanced", Color(0xFF7B1FA2), R.drawable.planet7, -1),
        "p8" to PlanetConfig("Void-9", 1000000000.0, "Dark Matter", Color(0xFF212121), R.drawable.planet8, -1)
    )

    private val _gameState = MutableStateFlow(loadGameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    init {
        startGameLoop()
        startEventLoop()
        startDroneLoop()
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
                processPassiveIncome()
                saveGameState()
            }
        }
    }

    private fun startEventLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(15000 + Random.nextLong(30000))
                if (_gameState.value.activeEvent == null) {
                    val r = Random.nextFloat()
                    when {
                        r < 0.33f -> spawnEvent(GameEventType.STORM, "Space Storm!", 30000)
                        r < 0.66f -> spawnEvent(GameEventType.ASTEROID, "Gold Asteroid!", 10000)
                        else -> spawnEvent(GameEventType.PIRATES, "Pirates!", 20000)
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
                val bonus = calculateDPS() * 300 + 500
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
        
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(4000)
                if (_gameState.value.scavengeTargets.size < 8) {
                    val newTarget = ScavengeTarget(
                        System.currentTimeMillis(),
                        Random.nextFloat(),
                        Random.nextFloat()
                    )
                    _gameState.update { it.copy(scavengeTargets = it.scavengeTargets + newTarget) }
                }
            }
        }
    }

    private fun updateDrones() {
        val state = _gameState.value
        val fleetCounts = state.fleetCounts
        
        _gameState.update { currentState ->
            val drones = currentState.drones.toMutableList()
            
            fleetItems.forEach { item ->
                val count = fleetCounts[item.id] ?: 0
                val currentOfThisType = drones.filter { it.type == item.id }
                if (currentOfThisType.size < count) {
                    repeat(count - currentOfThisType.size) {
                        drones.add(DroneData(Random.nextLong(), Random.nextFloat(), Random.nextFloat(), type = item.id))
                    }
                } else if (currentOfThisType.size > count) {
                    val toRemove = currentOfThisType.size - count
                    repeat(toRemove) {
                        val droneToRemove = drones.find { it.type == item.id }
                        if (droneToRemove != null) {
                            drones.remove(droneToRemove)
                        }
                    }
                }
            }

            if (drones.isEmpty()) return@update currentState

            val targets = currentState.scavengeTargets.toMutableList()
            var debrisGained = 0.0
            val claimedTargetIds = drones.mapNotNull { it.targetId }.toSet()

            val updatedDrones = drones.map { drone ->
                var nx = drone.x
                var ny = drone.y
                var nState = drone.state
                var nTargetId = drone.targetId
                var nHasCargo = drone.hasCargo
                val speed = 0.025f

                when (drone.state) {
                    DroneState.IDLE -> {
                        val availableTarget = targets.firstOrNull { t -> t.id !in claimedTargetIds }
                        if (availableTarget != null) {
                            nTargetId = availableTarget.id
                            nState = DroneState.MOVING_TO_DEBRIS
                        } else {
                            nx += (Random.nextFloat() - 0.5f) * 0.005f
                            ny += (Random.nextFloat() - 0.5f) * 0.005f
                        }
                    }
                    DroneState.MOVING_TO_DEBRIS -> {
                        val target = targets.find { it.id == drone.targetId }
                        if (target != null) {
                            val dx = target.x - nx
                            val dy = target.y - ny
                            val distSq = dx * dx + dy * dy
                            if (distSq < 0.0025) {
                                nState = DroneState.RETURNING
                                nHasCargo = true
                                targets.removeAll { it.id == target.id }
                                nTargetId = null
                            } else {
                                val dist = Math.sqrt(distSq.toDouble()).toFloat()
                                nx += (dx / dist) * speed
                                ny += (dy / dist) * speed
                            }
                        } else {
                            nState = DroneState.RETURNING
                        }
                    }
                    DroneState.RETURNING -> {
                        val dx = 0.5f - nx
                        val dy = 0.5f - ny
                        val distSq = dx * dx + dy * dy
                        if (distSq < 0.0025) {
                            if (nHasCargo) {
                                debrisGained += 5.0
                            }
                            nState = DroneState.IDLE
                            nHasCargo = false
                        } else {
                            val dist = Math.sqrt(distSq.toDouble()).toFloat()
                            nx += (dx / dist) * speed
                            ny += (dy / dist) * speed
                        }
                    }
                }
                drone.copy(x = nx.coerceIn(0f, 1f), y = ny.coerceIn(0f, 1f), state = nState, targetId = nTargetId, hasCargo = nHasCargo)
            }

            currentState.copy(
                drones = updatedDrones,
                scavengeTargets = targets,
                totalDebris = currentState.totalDebris + debrisGained
            )
        }
    }

    private fun processPassiveIncome() {
        var dps = calculateDPS()
        _gameState.update { currentState ->
            var newTotalDebris = currentState.totalDebris
            var newHotelDebt = currentState.currentHotelDebt
            var hotelDebtActive = currentState.isHotelDebtActive
            if (currentState.activeEvent?.type == GameEventType.PIRATES) newTotalDebris *= 0.95

            if (dps > 0) {
                if (hotelDebtActive) {
                    val debtPayment = dps * 0.3
                    val actualIncome = dps * 0.7
                    newHotelDebt -= debtPayment
                    newTotalDebris += actualIncome
                    if (newHotelDebt <= 0) {
                        newTotalDebris = 0.0
                        hotelDebtActive = false
                    }
                } else {
                    newTotalDebris += dps
                }
            }
            currentState.copy(totalDebris = newTotalDebris, currentHotelDebt = newHotelDebt, isHotelDebtActive = hotelDebtActive)
        }
    }

    fun calculateDPS(): Double {
        val state = _gameState.value
        var dps = fleetItems.sumOf { (state.fleetCounts[it.id] ?: 0) * it.rate }
        when (state.currentPlanetId) {
            "p4" -> dps *= 1.5
            "p7" -> dps *= 2.0
            "p1" -> dps *= 1.2
            "p8" -> dps *= 2.0
        }
        if (state.isHotelDebtActive) dps *= 5.0
        return dps
    }

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
        var valToReturn = calculateClickValue()
        _gameState.update { currentState ->
            var clickPower = valToReturn
            if (currentState.currentPlanetId == "p4" && Random.nextFloat() < 0.1) {
                clickPower *= 5.0
                valToReturn = clickPower
            }
            var newTotalDebris = currentState.totalDebris
            var newHotelDebt = currentState.currentHotelDebt
            var hotelDebtActive = currentState.isHotelDebtActive
            if (hotelDebtActive) {
                val debtPayment = clickPower * 0.3
                val actualIncome = clickPower * 0.7
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
        return valToReturn
    }

    fun buyClickUpgrade(id: String) {
        val item = clickItems.find { it.id == id } ?: return
        val currentLvl = _gameState.value.clickLevels[id] ?: 0
        val cost = cost(item.base, currentLvl)
        if (_gameState.value.totalDebris >= cost) {
            _gameState.update { it.copy(totalDebris = it.totalDebris - cost, clickLevels = it.clickLevels + (id to currentLvl + 1)) }
            saveGameState()
        }
    }

    fun buyFleet(id: String) {
        val item = fleetItems.find { it.id == id } ?: return
        val currentCount = _gameState.value.fleetCounts[id] ?: 0
        val totalDrones = _gameState.value.fleetCounts.values.sum()
        
        if (totalDrones >= 5) return // Limit drones to 5

        var cost = cost(item.base, currentCount)
        if (_gameState.value.currentPlanetId == "p6") cost *= 0.85
        if (_gameState.value.totalDebris >= cost) {
            _gameState.update { it.copy(totalDebris = it.totalDebris - cost, fleetCounts = it.fleetCounts + (id to currentCount + 1)) }
            saveGameState()
        }
    }

    fun sellFleet(id: String) {
        val item = fleetItems.find { it.id == id } ?: return
        val currentCount = _gameState.value.fleetCounts[id] ?: 0
        if (currentCount <= 0) return

        val cost = cost(item.base, currentCount - 1)
        val refund = cost / 2.0
        
        _gameState.update { it.copy(totalDebris = it.totalDebris + refund, fleetCounts = it.fleetCounts + (id to currentCount - 1)) }
        saveGameState()
    }

    fun buyPlanet(planetId: String) {
        val config = planets[planetId] ?: return
        val state = _gameState.value
        
        if (state.ownedPlanets.contains(planetId)) {
            _gameState.update { it.copy(currentPlanetId = planetId) }
            saveGameState()
        } else if (state.totalDebris >= config.price) {
            _gameState.update { it.copy(
                totalDebris = it.totalDebris - config.price, 
                currentPlanetId = planetId,
                ownedPlanets = it.ownedPlanets + planetId
            ) }
            saveGameState()
        }
    }

    fun takeHotelDebt() {
        if (!_gameState.value.isHotelDebtActive) {
            _gameState.update { it.copy(isHotelDebtActive = true, currentHotelDebt = 1000000.0) }
            saveGameState()
        }
    }

    private fun cost(base: Double, level: Int) = (base * 1.15.pow(level.toDouble())).toLong().toDouble()
}

data class ItemConfig(val id: String, val name: String, val base: Double, val value: Double, val iconRes: Int)
data class FleetConfig(val id: String, val name: String, val base: Double, val rate: Double, val iconRes: Int, val spriteIndex: Int = -1)
data class PlanetConfig(val name: String, val price: Double, val desc: String, val color: Color, val imageRes: Int, val spriteIndex: Int = -1)
