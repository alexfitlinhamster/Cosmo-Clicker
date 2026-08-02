package com.example.myapplication

object EventEngine {
    enum class Category { POSITIVE, NEGATIVE, MIXED }

    private const val MIN_INTERVAL_MS = 15_000L
    private const val INTERVAL_RANGE_MS = 30_000L
    private const val MIN_DURATION_MS = 20_000L
    private const val MAX_DURATION_MS = 60_000L
    private const val EVENT_CLICK_MULTIPLIER = 3.0
    private const val BLACK_HOLE_TAPS = 10
    private const val ASTEROID_REWARD = 500.0
    private const val VOID_ENERGY_DURATION_MS = 30_000L
    private const val RARE_TARGET_DURATION_MS = 30_000L
    private const val DISTRESS_RESCUE_DURATION_MS = 10_000L

    private val eventWeights = linkedMapOf(
        GameEventType.STORM to 20,
        GameEventType.ASTEROID to 25,
        GameEventType.METEOR_SHOWER to 20,
        GameEventType.BLACK_HOLE to 8,
        GameEventType.SOLAR_FLARE to 15,
        GameEventType.CYBER_VIRUS to 12,
        GameEventType.DISTRESS_SIGNAL to 10
    )

    private val negativeTypes = setOf(
        GameEventType.STORM,
        GameEventType.SOLAR_FLARE,
        GameEventType.CYBER_VIRUS
    )

    fun category(type: GameEventType): Category = when (type) {
        GameEventType.ASTEROID -> Category.POSITIVE
        GameEventType.CYBER_VIRUS -> Category.NEGATIVE
        GameEventType.STORM,
        GameEventType.METEOR_SHOWER,
        GameEventType.BLACK_HOLE,
        GameEventType.SOLAR_FLARE,
        GameEventType.DISTRESS_SIGNAL -> Category.MIXED
    }

    fun nextIntervalMillis(planetId: String, random: RandomProvider): Long {
        val baseInterval = MIN_INTERVAL_MS + random.nextLong(INTERVAL_RANGE_MS)
        val modifiers = PlanetEventModifiers.forPlanet(planetId)
        return (baseInterval / modifiers.frequencyMultiplier).toLong()
    }

    fun nextDurationMillis(planetId: String, random: RandomProvider): Long {
        val baseDuration = random.nextLong(MIN_DURATION_MS, MAX_DURATION_MS + 1)
        val modifiers = PlanetEventModifiers.forPlanet(planetId)
        return (baseDuration * modifiers.durationMultiplier).toLong()
    }

    fun selectType(planetId: String, random: RandomProvider): GameEventType? {
        val modifiers = PlanetEventModifiers.forPlanet(planetId)
        val availableWeights = eventWeights.filterKeys { it !in modifiers.blockedEvents }
        val totalWeight = availableWeights.values.sum()
        var roll = random.nextInt(totalWeight)
        var selectedType = availableWeights.entries.first().key
        for ((type, weight) in availableWeights) {
            if (roll < weight) {
                selectedType = type
                break
            }
            roll -= weight
        }

        val resistancePercent = (modifiers.negativeEventResistance * 100).toInt()
        if (selectedType in negativeTypes && resistancePercent > 0 &&
            random.nextInt(100) < resistancePercent
        ) {
            return when (modifiers.resistanceOutcome) {
                NegativeEventResistanceOutcome.REDIRECT_TO_POSITIVE -> GameEventType.ASTEROID
                NegativeEventResistanceOutcome.CANCEL -> null
            }
        }
        return selectedType
    }

    fun startEvent(
        state: GameState,
        type: GameEventType,
        durationMillis: Long,
        nowMillis: Long,
        random: RandomProvider,
        clickValue: Double = 1.0
    ): GameState {
        val workingDrones = state.drones.filter { it.state != DroneState.BROKEN }
        val actualType = if (type == GameEventType.CYBER_VIRUS && workingDrones.isEmpty()) {
            GameEventType.ASTEROID
        } else type
        val infectedDroneId = if (actualType == GameEventType.CYBER_VIRUS) {
            random.choose(workingDrones).id
        } else null

        return state.copy(
            activeEvent = GameEvent(
                type = actualType,
                expiresAt = nowMillis + durationMillis,
                x = random.nextFloat(),
                y = random.nextFloat() * 0.6f + 0.1f,
                startedAt = nowMillis,
                reward = if (actualType == GameEventType.ASTEROID) {
                    calculateAsteroidReward(clickValue, random)
                } else if (actualType == GameEventType.DISTRESS_SIGNAL) {
                    maxOf(1_000.0, clickValue.coerceAtLeast(0.0) * 200.0)
                } else {
                    0.0
                }
            ),
            eventMultiplier = if (actualType == GameEventType.STORM || actualType == GameEventType.SOLAR_FLARE) {
                EVENT_CLICK_MULTIPLIER
            } else {
                1.0
            },
            eventTapsLeft = if (actualType == GameEventType.BLACK_HOLE) BLACK_HOLE_TAPS else 0,
            infectedDroneId = infectedDroneId
        )
    }

    fun expireEventIfNeeded(state: GameState, nowMillis: Long): GameState {
        val event = state.activeEvent ?: return state
        return if (event.expiresAt <= nowMillis) finishEvent(state) else state
    }

    fun onAsteroidClick(state: GameState): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.ASTEROID) return state
        val reward = event.reward.takeIf { it > 0.0 }
            ?: (ASTEROID_REWARD * state.eventMultiplier)
        return finishEvent(
            state.copy(totalDebris = state.totalDebris + reward)
        )
    }

    fun calculateAsteroidReward(clickValue: Double, random: RandomProvider): Double {
        val multiplier = 50 + random.nextInt(101)
        return maxOf(ASTEROID_REWARD, clickValue.coerceAtLeast(0.0) * multiplier)
    }

    fun cyberVirusTheft(totalDebris: Double): Double =
        (totalDebris.coerceAtLeast(0.0) * 0.00005).coerceIn(1.0, 100_000.0)

    fun onInfectedDroneClick(state: GameState, droneId: Long): GameState {
        if (state.activeEvent?.type != GameEventType.CYBER_VIRUS || state.infectedDroneId != droneId) {
            return state
        }
        return finishEvent(state)
    }

    fun respondToDistressSignal(
        state: GameState,
        choice: DistressChoice,
        nowMillis: Long,
        random: RandomProvider
    ): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.DISTRESS_SIGNAL) return state
        return when (choice) {
            DistressChoice.SALVAGE -> finishEvent(state).copy(
                totalDebris = state.totalDebris + event.reward,
                eventChainResult = EventChainResult(success = true, reward = event.reward)
            )
            DistressChoice.RESCUE -> {
                val success = random.nextInt(100) < 70
                finishEvent(state).copy(
                    pendingEventChain = PendingEventChain(
                        resolvesAt = nowMillis + DISTRESS_RESCUE_DURATION_MS,
                        success = success,
                        reward = if (success) event.reward * 3.0 else 0.0
                    )
                )
            }
        }
    }

    fun resolvePendingChainIfNeeded(state: GameState, nowMillis: Long): GameState {
        val pending = state.pendingEventChain ?: return state
        if (pending.resolvesAt > nowMillis) return state
        return state.copy(
            totalDebris = state.totalDebris + pending.reward,
            pendingEventChain = null,
            eventChainResult = EventChainResult(pending.success, pending.reward)
        )
    }

    fun clearChainResult(state: GameState): GameState = state.copy(eventChainResult = null)

    fun onBlackHoleClick(
        state: GameState,
        nowMillis: Long,
        random: RandomProvider,
        createRareTarget: (x: Float, y: Float, expiresAt: Long) -> ScavengeTarget
    ): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.BLACK_HOLE) return state

        val tapsLeft = state.eventTapsLeft - 1
        if (tapsLeft > 0) return state.copy(eventTapsLeft = tapsLeft)

        val targets = state.scavengeTargets.toMutableList()
        val rewardCount = PlanetEventModifiers.forPlanet(state.currentPlanetId)
            .blackHoleRareTargetRewardCount
        if (rewardCount > 0) {
            repeat(rewardCount) {
                val x = GameRules.clampDebrisSpawnCoordinate(
                    event.x + (random.nextFloat() - 0.5f) * 0.2f
                )
                val y = GameRules.clampDebrisSpawnCoordinate(
                    event.y + (random.nextFloat() - 0.5f) * 0.2f
                )
                targets += createRareTarget(x, y, nowMillis + RARE_TARGET_DURATION_MS)
            }
        }

        return finishEvent(state).copy(
            scavengeTargets = targets,
            activeEffects = state.activeEffects +
                (SkillType.VOID_ENERGY.id to nowMillis + VOID_ENERGY_DURATION_MS)
        )
    }

    private fun finishEvent(state: GameState): GameState = state.copy(
        activeEvent = null,
        eventMultiplier = 1.0,
        eventTapsLeft = 0,
        infectedDroneId = null
    )

}
