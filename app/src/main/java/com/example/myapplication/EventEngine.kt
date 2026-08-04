package com.example.myapplication

object EventEngine {
    enum class Category { POSITIVE, NEGATIVE, MIXED }

    private const val MIN_INTERVAL_MS = 15_000L
    private const val INTERVAL_RANGE_MS = 30_000L
    private const val MIN_DURATION_MS = 20_000L
    private const val MAX_DURATION_MS = 60_000L
    private const val EVENT_CLICK_MULTIPLIER = 3.0
    private const val BLACK_HOLE_TAPS = 10
    private const val PIRATE_RAID_TAPS = 15
    private const val ASTEROID_REWARD = 500.0
    private const val VOID_ENERGY_DURATION_MS = 30_000L
    private const val RARE_TARGET_DURATION_MS = 30_000L
    private const val DISTRESS_RESCUE_DURATION_MS = 10_000L
    private const val STATION_SAFE_DURATION_MS = 8_000L
    private const val STATION_REACTOR_DURATION_MS = 15_000L
    private const val TRADE_POWER_DURATION_MS = 60_000L
    private const val TRADE_LUCK_DURATION_MS = 90_000L

    private val eventWeights = linkedMapOf(
        GameEventType.STORM to 20,
        GameEventType.ASTEROID to 25,
        GameEventType.METEOR_SHOWER to 20,
        GameEventType.BLACK_HOLE to 8,
        GameEventType.SOLAR_FLARE to 15,
        GameEventType.CYBER_VIRUS to 12,
        GameEventType.DISTRESS_SIGNAL to 10,
        GameEventType.ABANDONED_STATION to 8,
        GameEventType.PIRATE_RAID to 8,
        GameEventType.TRADING_SHIP to 10
    )

    private val negativeTypes = setOf(
        GameEventType.STORM,
        GameEventType.SOLAR_FLARE,
        GameEventType.CYBER_VIRUS,
        GameEventType.PIRATE_RAID
    )

    fun category(type: GameEventType): Category = when (type) {
        GameEventType.ASTEROID, GameEventType.TRADING_SHIP -> Category.POSITIVE
        GameEventType.CYBER_VIRUS -> Category.NEGATIVE
        GameEventType.STORM,
        GameEventType.METEOR_SHOWER,
        GameEventType.BLACK_HOLE,
        GameEventType.SOLAR_FLARE,
        GameEventType.DISTRESS_SIGNAL,
        GameEventType.ABANDONED_STATION,
        GameEventType.PIRATE_RAID -> Category.MIXED
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

        val event = GameEvent(
                type = actualType,
                expiresAt = nowMillis + durationMillis,
                x = random.nextFloat(),
                y = random.nextFloat() * 0.6f + 0.1f,
                startedAt = nowMillis,
                reward = if (actualType == GameEventType.ASTEROID) {
                    calculateAsteroidReward(clickValue, random)
                } else if (actualType == GameEventType.DISTRESS_SIGNAL) {
                    maxOf(1_000.0, clickValue.coerceAtLeast(0.0) * 200.0)
                } else if (actualType == GameEventType.ABANDONED_STATION) {
                    maxOf(5_000.0, clickValue.coerceAtLeast(0.0) * 400.0)
                } else if (actualType == GameEventType.PIRATE_RAID) {
                    maxOf(5_000.0, clickValue.coerceAtLeast(0.0) * 300.0)
                } else if (actualType == GameEventType.TRADING_SHIP) {
                    maxOf(1_000.0, clickValue.coerceAtLeast(0.0) * 120.0)
                } else if (actualType == GameEventType.CYBER_VIRUS) {
                    maxOf(5_000.0, clickValue.coerceAtLeast(0.0) * 250.0)
                } else {
                    0.0
                }
            )
        return appendLog(state, event.type, EventLogOutcome.STARTED, nowMillis).copy(
            activeEvent = event,
            eventMultiplier = if (actualType == GameEventType.STORM || actualType == GameEventType.SOLAR_FLARE) {
                EVENT_CLICK_MULTIPLIER
            } else {
                1.0
            },
            eventTapsLeft = when (actualType) {
                GameEventType.BLACK_HOLE -> BLACK_HOLE_TAPS
                GameEventType.PIRATE_RAID -> PIRATE_RAID_TAPS
                else -> 0
            },
            infectedDroneId = infectedDroneId
        )
    }

    fun expireEventIfNeeded(state: GameState, nowMillis: Long): GameState {
        val event = state.activeEvent ?: return state
        return if (event.expiresAt <= nowMillis) {
            finishEvent(state, EventLogOutcome.EXPIRED, nowMillis)
        } else state
    }

    fun onAsteroidClick(state: GameState, nowMillis: Long): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.ASTEROID) return state
        val reward = event.reward.takeIf { it > 0.0 }
            ?: (ASTEROID_REWARD * state.eventMultiplier)
        return finishEvent(
            state.copy(totalDebris = state.totalDebris + reward),
            EventLogOutcome.COMPLETED,
            nowMillis,
            reward
        )
    }

    fun calculateAsteroidReward(clickValue: Double, random: RandomProvider): Double {
        val multiplier = 50 + random.nextInt(101)
        return maxOf(ASTEROID_REWARD, clickValue.coerceAtLeast(0.0) * multiplier)
    }

    fun cyberVirusTheft(totalDebris: Double): Double =
        (totalDebris.coerceAtLeast(0.0) * 0.00005).coerceIn(1.0, 100_000.0)

    fun pirateRaidTheft(totalDebris: Double): Double =
        (totalDebris.coerceAtLeast(0.0) * 0.002).coerceIn(5.0, 5_000_000.0)

    fun onPirateRaidClick(state: GameState, nowMillis: Long): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.PIRATE_RAID) return state
        val tapsLeft = state.eventTapsLeft - 1
        if (tapsLeft > 0) return state.copy(eventTapsLeft = tapsLeft)
        return finishEvent(
            state.copy(totalDebris = state.totalDebris + event.reward),
            EventLogOutcome.COMPLETED,
            nowMillis,
            event.reward
        )
    }

    fun onInfectedDroneClick(state: GameState, droneId: Long, nowMillis: Long): GameState {
        if (state.activeEvent?.type != GameEventType.CYBER_VIRUS || state.infectedDroneId != droneId) {
            return state
        }
        return finishEvent(state, EventLogOutcome.COMPLETED, nowMillis)
    }

    fun resolveCyberVirus(state: GameState, success: Boolean, nowMillis: Long): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.CYBER_VIRUS) return state
        if (success) {
            return finishEvent(
                state.copy(totalDebris = state.totalDebris + event.reward),
                EventLogOutcome.SUCCESS,
                nowMillis,
                event.reward
            ).copy(eventChainResult = EventChainResult(true, event.reward, GameEventType.CYBER_VIRUS))
        }
        val loss = (state.totalDebris * 0.03).coerceAtMost(5_000_000.0)
        val infectedId = state.infectedDroneId
        val disabled = state.drones.map { drone ->
            if (drone.id == infectedId) drone.copy(disabledUntil = nowMillis + 60_000L) else drone
        }
        return finishEvent(
            state.copy(totalDebris = (state.totalDebris - loss).coerceAtLeast(0.0), drones = disabled),
            EventLogOutcome.FAILURE,
            nowMillis
        ).copy(eventChainResult = EventChainResult(false, 0.0, GameEventType.CYBER_VIRUS, loss))
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
            DistressChoice.SALVAGE -> finishEvent(
                state,
                EventLogOutcome.COMPLETED,
                nowMillis,
                event.reward
            ).copy(
                totalDebris = state.totalDebris + event.reward,
                eventChainResult = EventChainResult(success = true, reward = event.reward)
            )
            DistressChoice.RESCUE -> {
                val success = random.nextInt(100) < 70
                finishEvent(state, EventLogOutcome.CHOICE, nowMillis).copy(
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
        return appendLog(
            state,
            pending.eventType,
            if (pending.success) EventLogOutcome.SUCCESS else EventLogOutcome.FAILURE,
            nowMillis,
            pending.reward
        ).copy(
            totalDebris = (state.totalDebris + pending.reward -
                if (pending.success) 0.0 else pending.failurePenalty).coerceAtLeast(0.0),
            pendingEventChain = null,
            eventChainResult = EventChainResult(
                pending.success,
                pending.reward,
                pending.eventType,
                if (pending.success) 0.0 else pending.failurePenalty
            )
        )
    }

    fun respondToAbandonedStation(
        state: GameState,
        choice: StationChoice,
        nowMillis: Long,
        random: RandomProvider
    ): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.ABANDONED_STATION) return state
        val safeRoute = choice == StationChoice.SAFE_ROUTE
        val success = random.nextInt(100) < if (safeRoute) 90 else 50
        val reward = if (success) event.reward * if (safeRoute) 1.5 else 5.0 else 0.0
        val penalty = if (safeRoute) 0.0 else state.totalDebris * 0.02
        return finishEvent(state, EventLogOutcome.CHOICE, nowMillis).copy(
            pendingEventChain = PendingEventChain(
                resolvesAt = nowMillis + if (safeRoute) STATION_SAFE_DURATION_MS else STATION_REACTOR_DURATION_MS,
                success = success,
                reward = reward,
                eventType = GameEventType.ABANDONED_STATION,
                failurePenalty = penalty
            )
        )
    }

    fun buyTradeOffer(state: GameState, offer: TradeOffer, nowMillis: Long): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.TRADING_SHIP) return state
        val cost = tradeOfferCost(event, offer)
        if (state.totalDebris < cost) return state
        val duration = if (offer == TradeOffer.POWER_CORE) {
            TRADE_POWER_DURATION_MS
        } else {
            TRADE_LUCK_DURATION_MS
        }
        val effect = if (offer == TradeOffer.POWER_CORE) SkillType.TRADE_POWER else SkillType.TRADE_LUCK
        return finishEvent(
            state.copy(
                totalDebris = state.totalDebris - cost,
                activeEffects = state.activeEffects + (effect.id to nowMillis + duration)
            ),
            EventLogOutcome.CHOICE,
            nowMillis
        )
    }

    fun tradeOfferCost(event: GameEvent, offer: TradeOffer): Double =
        event.reward * if (offer == TradeOffer.POWER_CORE) 1.0 else 0.75

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

        return finishEvent(state, EventLogOutcome.COMPLETED, nowMillis).copy(
            scavengeTargets = targets,
            activeEffects = state.activeEffects +
                (SkillType.VOID_ENERGY.id to nowMillis + VOID_ENERGY_DURATION_MS)
        )
    }

    private fun finishEvent(
        state: GameState,
        outcome: EventLogOutcome,
        timestamp: Long,
        reward: Double = 0.0
    ): GameState {
        val eventType = state.activeEvent?.type ?: return state
        val quests = QuestEngine.advance(state.activeQuests, QuestType.COMPLETE_EVENT)
        return appendLog(state, eventType, outcome, timestamp, reward).copy(
        activeEvent = null,
        eventMultiplier = 1.0,
        eventTapsLeft = 0,
        infectedDroneId = null,
        lifetimeStats = state.lifetimeStats.copy(
            eventsCompleted = state.lifetimeStats.eventsCompleted + 1
        ),
        activeQuests = quests
        )
    }

    private fun appendLog(
        state: GameState,
        type: GameEventType,
        outcome: EventLogOutcome,
        timestamp: Long,
        reward: Double = 0.0
    ): GameState = state.copy(
        eventLog = (state.eventLog + EventLogEntry(timestamp, type, outcome, reward)).takeLast(30)
    )

}
