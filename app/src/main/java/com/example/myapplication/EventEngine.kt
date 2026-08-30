package com.example.myapplication

object EventEngine {
    enum class Category { POSITIVE, NEGATIVE, MIXED }

    private const val MIN_INTERVAL_MS = 45_000L
    private const val INTERVAL_RANGE_MS = 45_000L
    private const val MIN_DURATION_MS = 20_000L
    private const val MAX_DURATION_MS = 60_000L
    private const val EVENT_CLICK_MULTIPLIER = 2.0
    private const val BLACK_HOLE_TAPS = 6
    const val SALVAGE_RUSH_DURATION_MS = 15_000L
    private const val STORM_TAPS = 3
    private const val SOLAR_FLARE_TAPS = 4
    private const val PIRATE_RAID_TAPS = 5
    private const val VOID_ENERGY_DURATION_MS = 30_000L
    private const val RARE_TARGET_DURATION_MS = 30_000L
    private const val DISTRESS_RESCUE_DURATION_MS = 10_000L
    private const val STATION_SAFE_DURATION_MS = 8_000L
    private const val STATION_REACTOR_DURATION_MS = 15_000L
    private const val TRADE_POWER_DURATION_MS = 60_000L
    private const val TRADE_LUCK_DURATION_MS = 90_000L
    private const val TRADE_CLICK_DURATION_MS = 45_000L
    private const val TRADE_FLEET_DURATION_MS = 60_000L
    private const val ELITE_DURATION_MULTIPLIER = 1.2

    private val eliteTypes = setOf(
        GameEventType.SOLAR_FLARE,
        GameEventType.CYBER_VIRUS,
        GameEventType.PIRATE_RAID
    )

    private val eventWeights = linkedMapOf(
        // Other enum values stay readable for old saves and event-log entries.
        // Only events played directly on the game field are generated.
        GameEventType.METEOR_SHOWER to 32,
        GameEventType.SOLAR_FLARE to 20,
        GameEventType.CYBER_VIRUS to 12,
        GameEventType.PIRATE_RAID to 12,
        // Roughly 4% of field events: visible often enough to feel real, still genuinely rare.
        GameEventType.TRADING_SHIP to 3
    )

    private val negativeTypes = setOf(
        GameEventType.STORM,
        GameEventType.SOLAR_FLARE,
        GameEventType.CYBER_VIRUS,
        GameEventType.PIRATE_RAID
    )

    fun category(type: GameEventType): Category = when (type) {
        GameEventType.TRADING_SHIP -> Category.POSITIVE
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
                NegativeEventResistanceOutcome.REDIRECT_TO_POSITIVE -> GameEventType.TRADING_SHIP
                NegativeEventResistanceOutcome.CANCEL -> null
            }
        }
        return selectedType
    }

    fun eliteChancePercent(planetId: String): Int =
        (3 + EconomyBalance.planetIndex(planetId) / 6).coerceAtMost(9)

    fun rollElite(type: GameEventType, planetId: String, random: RandomProvider): Boolean =
        type in eliteTypes && random.nextInt(100) < eliteChancePercent(planetId)

    private fun eliteRewardMultiplier(type: GameEventType): Double = when (type) {
        GameEventType.BLACK_HOLE -> 1.75
        GameEventType.SOLAR_FLARE -> 2.8
        GameEventType.CYBER_VIRUS -> 2.35
        GameEventType.DISTRESS_SIGNAL -> 1.9
        GameEventType.ABANDONED_STATION -> 2.55
        GameEventType.PIRATE_RAID -> 3.2
        else -> 1.0
    }

    fun startEvent(
        state: GameState,
        type: GameEventType,
        durationMillis: Long,
        nowMillis: Long,
        random: RandomProvider,
        clickValue: Double = 1.0,
        isElite: Boolean = false
    ): GameState {
        val workingDrones = state.drones.filter { it.state != DroneState.BROKEN }
        val actualType = when {
            type == GameEventType.CYBER_VIRUS && workingDrones.isEmpty() -> GameEventType.METEOR_SHOWER
            else -> type
        }
        val infectedDroneId = if (actualType == GameEventType.CYBER_VIRUS) {
            random.choose(workingDrones).id
        } else null

        val baseReward = if (actualType == GameEventType.DISTRESS_SIGNAL) {
            eventReward(state, clickValue, 500.0, 40.0, 0.006)
        } else if (actualType == GameEventType.ABANDONED_STATION) {
            eventReward(state, clickValue, 1_500.0, 60.0, 0.010)
        } else if (actualType == GameEventType.PIRATE_RAID) {
            eventReward(state, clickValue, 1_500.0, 50.0, 0.012)
        } else if (actualType == GameEventType.TRADING_SHIP) {
            eventReward(state, clickValue, 500.0, 25.0, 0.004)
        } else if (actualType == GameEventType.CYBER_VIRUS) {
            eventReward(state, clickValue, 1_000.0, 50.0, 0.008)
        } else if (actualType == GameEventType.STORM) {
            eventReward(state, clickValue, 750.0, 35.0, 0.007)
        } else if (actualType == GameEventType.SOLAR_FLARE) {
            eventReward(state, clickValue, 1_000.0, 45.0, 0.008)
        } else if (actualType == GameEventType.BLACK_HOLE && isElite) {
            eventReward(state, clickValue, 1_500.0, 55.0, 0.009)
        } else {
            0.0
        }
        val eliteDuration = if (isElite) {
            (durationMillis * ELITE_DURATION_MULTIPLIER).toLong()
        } else durationMillis
        val event = GameEvent(
                type = actualType,
                expiresAt = nowMillis + eliteDuration,
                x = random.nextFloat(),
                y = random.nextFloat() * 0.6f + 0.1f,
                startedAt = nowMillis,
                reward = baseReward * if (isElite) eliteRewardMultiplier(actualType) else 1.0,
                isElite = isElite
            )
        return appendLog(state, event.type, EventLogOutcome.STARTED, nowMillis).copy(
            activeEvent = event,
            eventMultiplier = if (actualType == GameEventType.STORM || actualType == GameEventType.SOLAR_FLARE) {
                EVENT_CLICK_MULTIPLIER
            } else {
                1.0
            },
            eventTapsLeft = when (actualType) {
                GameEventType.BLACK_HOLE -> if (isElite) 8 else BLACK_HOLE_TAPS
                GameEventType.DISTRESS_SIGNAL -> if (isElite) 4 else 3
                GameEventType.ABANDONED_STATION -> if (isElite) 4 else 3
                GameEventType.TRADING_SHIP -> 18
                GameEventType.STORM -> STORM_TAPS
                GameEventType.SOLAR_FLARE -> SOLAR_FLARE_TAPS
                GameEventType.PIRATE_RAID -> if (isElite) 7 else PIRATE_RAID_TAPS
                GameEventType.METEOR_SHOWER -> if (isElite) 7 else 5
                GameEventType.CYBER_VIRUS -> if (isElite) 8 else 6
            },
            infectedDroneId = infectedDroneId,
            stormSequence = when (actualType) {
                GameEventType.STORM -> List(3) { random.nextInt(3) }
                GameEventType.BLACK_HOLE -> listOf(random.nextInt(4))
                GameEventType.DISTRESS_SIGNAL -> listOf(random.nextInt(4))
                GameEventType.ABANDONED_STATION -> listOf(random.nextInt(3))
                GameEventType.TRADING_SHIP -> emptyList()
                GameEventType.SOLAR_FLARE -> List(if (isElite) 4 else 3) { random.nextInt(4) }
                GameEventType.PIRATE_RAID -> listOf(random.nextInt(3))
                GameEventType.METEOR_SHOWER -> listOf(random.nextInt(4))
                GameEventType.CYBER_VIRUS -> listOf(random.nextInt(4))
            },
            stormProgress = 0,
            stormRound = if (actualType == GameEventType.BLACK_HOLE) 0 else 1
        )
    }

    fun expireEventIfNeeded(state: GameState, nowMillis: Long): GameState {
        val event = state.activeEvent ?: return state
        return if (event.expiresAt <= nowMillis) {
            if (event.type == GameEventType.CYBER_VIRUS) {
                val burnedIncome = (event.reward * 1.2).coerceAtMost(state.totalDebris)
                finishEvent(
                    state.copy(totalDebris = (state.totalDebris - burnedIncome).coerceAtLeast(0.0)),
                    EventLogOutcome.EXPIRED,
                    nowMillis
                )
            } else finishEvent(state, EventLogOutcome.EXPIRED, nowMillis)
        } else state
    }

    fun onCyberNodeClick(state: GameState, node: Int, nowMillis: Long, random: RandomProvider): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.CYBER_VIRUS) return state
        val expected = state.stormSequence.firstOrNull() ?: return state
        if (node != expected) {
            return state.copy(
                eventTapsLeft = (state.eventTapsLeft + 1).coerceAtMost(if (event.isElite) 8 else 6),
                stormSequence = listOf(random.nextInt(4))
            )
        }
        val left = state.eventTapsLeft - 1
        if (left > 0) return state.copy(
            eventTapsLeft = left,
            stormProgress = state.stormProgress + 1,
            stormSequence = listOf(random.nextInt(4))
        )
        return finishEvent(
            state.copy(totalDebris = state.totalDebris + event.reward),
            EventLogOutcome.COMPLETED,
            nowMillis,
            event.reward
        )
    }

    fun onChallengeClick(
        state: GameState,
        nowMillis: Long,
        random: RandomProvider = KotlinRandomProvider
    ): GameState {
        val event = state.activeEvent ?: return state
        if (event.type != GameEventType.STORM && event.type != GameEventType.SOLAR_FLARE) return state
        val tapsLeft = state.eventTapsLeft - 1
        val hitReward = if (event.type == GameEventType.STORM) event.reward / STORM_TAPS else 0.0
        if (tapsLeft > 0) return state.copy(
            totalDebris = state.totalDebris + hitReward,
            eventTapsLeft = tapsLeft,
            activeEvent = if (event.type == GameEventType.STORM) event.copy(
                x = 0.12f + random.nextFloat() * 0.76f,
                y = 0.12f + random.nextFloat() * 0.52f
            ) else event
        )
        val finalReward = if (event.type == GameEventType.STORM) hitReward else event.reward
        val rewarded = state.copy(totalDebris = state.totalDebris + finalReward)
        return finishEvent(rewarded, EventLogOutcome.COMPLETED, nowMillis, event.reward)
    }

    fun onStormNodeClick(state: GameState, node: Int, nowMillis: Long, random: RandomProvider): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.STORM || state.stormSequence.isEmpty()) return state
        val expected = state.stormSequence[state.stormProgress.coerceIn(0, state.stormSequence.lastIndex)]
        if (node != expected) return state.copy(stormProgress = 0, eventMultiplier = 1.0)
        val nextProgress = state.stormProgress + 1
        if (nextProgress < state.stormSequence.size) return state.copy(stormProgress = nextProgress)
        val roundReward = event.reward / 3.0
        if (state.stormRound >= 3) {
            return finishEvent(
                state.copy(totalDebris = state.totalDebris + roundReward),
                EventLogOutcome.COMPLETED,
                nowMillis,
                event.reward
            )
        }
        val nextLength = state.stormSequence.size + 1
        return state.copy(
            totalDebris = state.totalDebris + roundReward,
            stormSequence = List(nextLength) { random.nextInt(3) },
            stormProgress = 0,
            stormRound = state.stormRound + 1,
            eventMultiplier = 1.0 + state.stormRound * 0.5
        )
    }

    fun onMeteorTargetClick(state: GameState, target: Int, nowMillis: Long, random: RandomProvider): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.METEOR_SHOWER) return state
        val expected = state.stormSequence.firstOrNull() ?: return state
        if (target != expected) {
            val penalty = (event.reward * 0.04).coerceAtMost(state.totalDebris * 0.005)
            return state.copy(
                totalDebris = (state.totalDebris - penalty).coerceAtLeast(0.0),
                eventMultiplier = (state.eventMultiplier - 0.15).coerceAtLeast(1.0),
                stormSequence = listOf(random.nextInt(4))
            )
        }
        val remaining = state.eventTapsLeft - 1
        val hitReward = event.reward / (if (event.isElite) 7.0 else 5.0)
        if (remaining > 0) return state.copy(
            totalDebris = state.totalDebris + hitReward * state.eventMultiplier,
            eventTapsLeft = remaining,
            stormProgress = state.stormProgress + 1,
            eventMultiplier = (state.eventMultiplier + 0.1).coerceAtMost(2.0),
            stormSequence = listOf(random.nextInt(4))
        )
        val rewarded = state.copy(totalDebris = state.totalDebris + hitReward * state.eventMultiplier)
        return finishEvent(rewarded, EventLogOutcome.COMPLETED, nowMillis, event.reward * state.eventMultiplier)
    }

    fun tickTradingShipCombat(
        state: GameState,
        nowMillis: Long,
        random: RandomProvider,
        createLoot: (Float, Float, Double) -> ScavengeTarget
    ): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.TRADING_SHIP) return state

        val attackers = state.drones.count { it.state != DroneState.BROKEN && it.disabledUntil <= nowMillis }
        val damage = (attackers / 2).coerceAtLeast(if (attackers > 0) 1 else 0)
        val hullLeft = (state.eventTapsLeft - damage).coerceAtLeast(0)
        val movedEvent = event.copy(
            x = (event.x + (random.nextFloat() - 0.35f) * 0.12f).coerceIn(0.14f, 0.86f),
            y = (event.y + (random.nextFloat() - 0.5f) * 0.08f).coerceIn(0.16f, 0.62f)
        )
        if (hullLeft > 0 || attackers == 0) {
            return state.copy(activeEvent = movedEvent, eventTapsLeft = hullLeft)
        }

        return if (random.nextInt(100) < 50) {
            val rewarded = state.copy(
                activeEvent = movedEvent,
                totalDebris = state.totalDebris + event.reward * 2.0
            )
            finishEvent(rewarded, EventLogOutcome.COMPLETED, nowMillis, event.reward * 2.0)
        } else {
            val loot = List(6) {
                createLoot(
                    (movedEvent.x + (random.nextFloat() - 0.5f) * 0.22f).coerceIn(0.08f, 0.92f),
                    (movedEvent.y + (random.nextFloat() - 0.5f) * 0.18f).coerceIn(0.10f, 0.78f),
                    event.reward / 3.0
                )
            }
            finishEvent(
                state.copy(activeEvent = movedEvent, scavengeTargets = state.scavengeTargets + loot),
                EventLogOutcome.COMPLETED,
                nowMillis
            )
        }
    }

    fun onSolarChannelClick(state: GameState, channel: Int, nowMillis: Long, random: RandomProvider): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.SOLAR_FLARE || state.stormSequence.isEmpty()) return state
        val expected = state.stormSequence[state.stormProgress.coerceIn(0, state.stormSequence.lastIndex)]
        if (channel != expected) {
            return state.copy(
                stormProgress = 0,
                eventMultiplier = (state.eventMultiplier - 0.25).coerceAtLeast(1.0),
                totalDebris = (state.totalDebris - event.reward * 0.03).coerceAtLeast(0.0)
            )
        }
        val nextProgress = state.stormProgress + 1
        if (nextProgress < state.stormSequence.size) return state.copy(stormProgress = nextProgress)
        val phaseReward = event.reward / 4.0 * state.eventMultiplier
        if (state.stormRound >= 4) {
            val rewarded = state.copy(
                totalDebris = state.totalDebris + phaseReward,
                activeEffects = state.activeEffects + (SkillType.VOID_ENERGY.id to nowMillis + 30_000L)
            )
            return finishEvent(rewarded, EventLogOutcome.COMPLETED, nowMillis, event.reward * state.eventMultiplier)
        }
        return state.copy(
            totalDebris = state.totalDebris + phaseReward,
            stormSequence = List(state.stormSequence.size + 1) { random.nextInt(4) },
            stormProgress = 0,
            stormRound = state.stormRound + 1,
            eventMultiplier = (state.eventMultiplier + 0.35).coerceAtMost(3.0)
        )
    }

    private fun eventReward(
        state: GameState,
        clickValue: Double,
        minimum: Double,
        multiplier: Double,
        wealthShare: Double
    ): Double {
        val clickEconomy = clickValue.coerceAtLeast(0.0) * multiplier
        val savedEconomy = state.totalDebris.coerceAtLeast(0.0) * wealthShare
        return maxOf(minimum, clickEconomy, savedEconomy).coerceAtMost(1.0e18)
    }

    fun cyberVirusTheft(totalDebris: Double): Double =
        (totalDebris.coerceAtLeast(0.0) * 0.00005).coerceIn(1.0, 100_000.0)

    fun pirateRaidTheft(totalDebris: Double, modulesLeft: Int = PIRATE_RAID_TAPS): Double {
        val pressure = modulesLeft.coerceIn(1, PIRATE_RAID_TAPS) / PIRATE_RAID_TAPS.toDouble()
        return (totalDebris.coerceAtLeast(0.0) * 0.002 * pressure).coerceIn(1.0, 5_000_000.0)
    }

    fun onPirateTargetClick(state: GameState, target: Int, nowMillis: Long, random: RandomProvider): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.PIRATE_RAID || state.stormRound != 1 || state.stormSequence.isEmpty()) return state
        val expected = state.stormSequence.first()
        if (target != expected) {
            return state.copy(
                totalDebris = (state.totalDebris - pirateRaidTheft(state.totalDebris, state.eventTapsLeft)).coerceAtLeast(0.0),
                eventMultiplier = (state.eventMultiplier - 0.08).coerceAtLeast(0.72),
                stormSequence = listOf(random.nextInt(3))
            )
        }
        val requiredHits = if (event.isElite) 7 else PIRATE_RAID_TAPS
        val nextProgress = state.stormProgress + 1
        if (nextProgress >= requiredHits) {
            return state.copy(stormRound = 2, stormProgress = requiredHits, eventTapsLeft = 0)
        }
        return state.copy(
            stormProgress = nextProgress,
            eventTapsLeft = requiredHits - nextProgress,
            stormSequence = listOf(random.nextInt(3)),
            activeEvent = event.copy(
                x = 0.2f + random.nextFloat() * 0.6f,
                y = 0.16f + random.nextFloat() * 0.36f
            )
        )
    }

    fun resolvePirateRaid(state: GameState, captureCargo: Boolean, nowMillis: Long): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.PIRATE_RAID || state.stormRound != 2) return state
        val multiplier = if (captureCargo) 1.6 else 1.1
        val reward = event.reward * multiplier * state.eventMultiplier.coerceAtLeast(0.7)
        val rewarded = state.copy(
            totalDebris = state.totalDebris + reward,
            droneParts = if (captureCargo) state.droneParts + (
                "drone_21" to ((state.droneParts["drone_21"] ?: 0) + if (event.isElite) 5 else 3)
            ) else state.droneParts,
            activeEffects = if (!captureCargo) {
                state.activeEffects + (SkillType.VOID_ENERGY.id to nowMillis + 30_000L)
            } else state.activeEffects
        )
        return finishEvent(rewarded, EventLogOutcome.COMPLETED, nowMillis, reward)
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
        if (event?.type != GameEventType.DISTRESS_SIGNAL || state.stormRound != 2) return state
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

    fun onDistressSignalNode(
        state: GameState,
        node: Int,
        random: RandomProvider
    ): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.DISTRESS_SIGNAL || state.stormRound != 1) return state
        val expected = state.stormSequence.firstOrNull() ?: return state
        if (node != expected) {
            val loss = (state.totalDebris * 0.005).coerceAtMost(1_000_000.0)
            return state.copy(
                totalDebris = (state.totalDebris - loss).coerceAtLeast(0.0),
                stormProgress = (state.stormProgress - 1).coerceAtLeast(0),
                stormSequence = listOf(random.nextInt(4))
            )
        }

        val requiredLocks = if (event.isElite) 4 else 3
        val nextProgress = state.stormProgress + 1
        if (nextProgress >= requiredLocks) {
            return state.copy(
                stormRound = 2,
                stormProgress = requiredLocks,
                eventTapsLeft = 0,
                stormSequence = emptyList()
            )
        }
        return state.copy(
            stormProgress = nextProgress,
            eventTapsLeft = requiredLocks - nextProgress,
            stormSequence = listOf(random.nextInt(4))
        )
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
        if (event?.type != GameEventType.ABANDONED_STATION || state.stormRound != 2) return state
        val safeRoute = choice == StationChoice.SAFE_ROUTE
        val success = random.nextInt(100) < if (safeRoute) 90 else 50
        val reward = if (success) {
            event.reward * (if (safeRoute) 1.5 else 5.0) * state.eventMultiplier.coerceAtLeast(0.76)
        } else 0.0
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

    fun onStationRelayClick(
        state: GameState,
        relay: Int,
        random: RandomProvider
    ): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.ABANDONED_STATION || state.stormRound != 1) return state
        val expected = state.stormSequence.firstOrNull() ?: return state
        if (relay != expected) {
            val loss = (state.totalDebris * 0.0075).coerceAtMost(2_000_000.0)
            return state.copy(
                totalDebris = (state.totalDebris - loss).coerceAtLeast(0.0),
                stormRound = 1,
                eventMultiplier = (state.eventMultiplier - 0.08).coerceAtLeast(0.76),
                stormSequence = listOf(random.nextInt(3))
            )
        }

        val requiredRelays = if (event.isElite) 4 else 3
        val nextProgress = state.stormProgress + 1
        if (nextProgress >= requiredRelays) {
            return state.copy(
                stormRound = 2,
                stormProgress = requiredRelays,
                eventTapsLeft = 0,
                stormSequence = emptyList()
            )
        }
        return state.copy(
            stormProgress = nextProgress,
            eventTapsLeft = requiredRelays - nextProgress,
            stormSequence = listOf(random.nextInt(3))
        )
    }

    fun buyTradeOffer(state: GameState, offer: TradeOffer, nowMillis: Long): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.TRADING_SHIP || state.stormRound != 2) return state
        val cost = tradeOfferCost(event, offer, state.eventMultiplier)
        if (state.totalDebris < cost) return state
        val paid = state.copy(totalDebris = state.totalDebris - cost)
        val rewarded = when (offer) {
            TradeOffer.POWER_CORE -> paid.withTradeEffect(SkillType.TRADE_POWER, nowMillis + TRADE_POWER_DURATION_MS)
            TradeOffer.LUCK_SCANNER -> paid.withTradeEffect(SkillType.TRADE_LUCK, nowMillis + TRADE_LUCK_DURATION_MS)
            TradeOffer.CLICK_AMPLIFIER -> paid.withTradeEffect(SkillType.TRADE_CLICK_BOOST, nowMillis + TRADE_CLICK_DURATION_MS)
            TradeOffer.FLEET_OVERDRIVE -> paid.withTradeEffect(SkillType.TRADE_FLEET_BOOST, nowMillis + TRADE_FLEET_DURATION_MS)
            TradeOffer.DEBRIS_CARGO -> paid.copy(totalDebris = paid.totalDebris + event.reward * 2.0)
            TradeOffer.COMMON_CASE -> paid.copy(isOpeningCase = true, openingCaseType = CaseType.COMMON, lastDroppedDroneId = null)
            TradeOffer.RARE_CASE -> paid.copy(isOpeningCase = true, openingCaseType = CaseType.RARE, lastDroppedDroneId = null)
            TradeOffer.LEGENDARY_CASE -> paid.copy(isOpeningCase = true, openingCaseType = CaseType.LEGENDARY, lastDroppedDroneId = null)
            TradeOffer.RANDOM_DRONE -> grantTradeDrone(paid, event)
        }
        return finishEvent(
            rewarded,
            EventLogOutcome.CHOICE,
            nowMillis
        )
    }

    fun tradeOffers(event: GameEvent, count: Int = 3): List<TradeOffer> =
        TradeOffer.entries
            .sortedBy { offer -> mixTradeSeed(event.startedAt + offer.ordinal * 9_973L) }
            .take(count.coerceIn(1, TradeOffer.entries.size))

    fun tradeOfferCost(event: GameEvent, offer: TradeOffer): Double = when (offer) {
        TradeOffer.POWER_CORE -> event.reward
        TradeOffer.LUCK_SCANNER -> event.reward * 0.75
        TradeOffer.CLICK_AMPLIFIER -> event.reward * 0.85
        TradeOffer.FLEET_OVERDRIVE -> event.reward * 0.9
        TradeOffer.DEBRIS_CARGO -> event.reward * 0.75
        TradeOffer.COMMON_CASE -> maxOf(900.0, event.reward * 0.65)
        TradeOffer.RARE_CASE -> maxOf(4_500.0, event.reward * 1.4)
        TradeOffer.LEGENDARY_CASE -> maxOf(18_000.0, event.reward * 3.5)
        TradeOffer.RANDOM_DRONE -> maxOf(25_000.0, event.reward * 6.0)
    }

    fun tradeOfferCost(event: GameEvent, offer: TradeOffer, priceMultiplier: Double): Double =
        tradeOfferCost(event, offer) * priceMultiplier.coerceIn(0.76, 1.24)

    fun onTradeChannelClick(state: GameState, channel: Int, random: RandomProvider): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.TRADING_SHIP || state.stormRound != 1) return state
        val expected = state.stormSequence.firstOrNull() ?: return state
        if (channel != expected) {
            return state.copy(
                eventMultiplier = (state.eventMultiplier + 0.08).coerceAtMost(1.24),
                stormSequence = listOf(random.nextInt(3))
            )
        }
        val nextProgress = state.stormProgress + 1
        return if (nextProgress >= 3) {
            state.copy(
                stormRound = 2,
                stormProgress = 3,
                eventTapsLeft = 0,
                eventMultiplier = (state.eventMultiplier - 0.08).coerceAtLeast(0.76),
                stormSequence = emptyList()
            )
        } else {
            state.copy(
                stormProgress = nextProgress,
                eventTapsLeft = 3 - nextProgress,
                eventMultiplier = (state.eventMultiplier - 0.08).coerceAtLeast(0.76),
                stormSequence = listOf(random.nextInt(3))
            )
        }
    }

    private fun GameState.withTradeEffect(type: SkillType, expiresAt: Long): GameState =
        copy(activeEffects = activeEffects + (type.id to expiresAt))

    private fun grantTradeDrone(state: GameState, event: GameEvent): GameState {
        val droneId = "drone_${((mixTradeSeed(event.startedAt) ushr 1) % 29L).toInt() + 1}"
        val owned = state.fleetCounts[droneId] ?: 0
        val canStoreDrone = state.fleetCounts.values.sum() < EconomyBalance.MAX_DRONES
        if (!canStoreDrone) {
            return state.copy(droneParts = state.droneParts + (droneId to (state.droneParts[droneId] ?: 0) + 1))
        }
        val activeTotal = state.activeFleetCounts.values.sum()
        return state.copy(
            fleetCounts = state.fleetCounts + (droneId to owned + 1),
            activeFleetCounts = if (activeTotal < DroneTraitEngine.MAX_ACTIVE_DRONES) {
                state.activeFleetCounts + (droneId to (state.activeFleetCounts[droneId] ?: 0) + 1)
            } else state.activeFleetCounts,
            discoveredDroneIds = state.discoveredDroneIds + droneId,
            lastDroppedDroneId = droneId
        )
    }

    private fun mixTradeSeed(value: Long): Long {
        var mixed = value xor (value ushr 33)
        mixed *= -49064778989728563L
        mixed = mixed xor (mixed ushr 33)
        mixed *= -4265267296055464877L
        return mixed xor (mixed ushr 33)
    }

    fun clearChainResult(state: GameState): GameState = state.copy(eventChainResult = null)

    fun onBlackHoleNodeClick(
        state: GameState,
        node: Int,
        nowMillis: Long,
        random: RandomProvider,
        createRareTarget: (x: Float, y: Float, expiresAt: Long) -> ScavengeTarget
    ): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.BLACK_HOLE) return state

        val expected = state.stormSequence.firstOrNull() ?: return state
        if (node != expected) {
            val loss = (state.totalDebris * 0.01).coerceAtMost(5_000_000.0)
            return state.copy(
                totalDebris = (state.totalDebris - loss).coerceAtLeast(0.0),
                stormRound = state.stormRound + 1,
                stormSequence = listOf(random.nextInt(4))
            )
        }

        val tapsLeft = state.eventTapsLeft - 1
        if (tapsLeft > 0) return state.copy(
            eventTapsLeft = tapsLeft,
            stormProgress = state.stormProgress + 1,
            stormSequence = listOf(random.nextInt(4))
        )

        val targets = state.scavengeTargets.toMutableList()
        val rewardCount = PlanetEventModifiers.forPlanet(state.currentPlanetId)
            .blackHoleRareTargetRewardCount + if (event.isElite) 2 else 0
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

        return finishEvent(
            state.copy(totalDebris = state.totalDebris + event.reward),
            EventLogOutcome.COMPLETED,
            nowMillis,
            event.reward
        ).copy(
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
        val completed = outcome != EventLogOutcome.EXPIRED
        val quests = if (completed) QuestEngine.advance(state.activeQuests, QuestType.COMPLETE_EVENT)
            else state.activeQuests
        return appendLog(state, eventType, outcome, timestamp, reward).copy(
        activeEvent = null,
        eventMultiplier = 1.0,
        eventTapsLeft = 0,
        infectedDroneId = null,
        stormSequence = emptyList(),
        stormProgress = 0,
        stormRound = 1,
        lifetimeStats = state.lifetimeStats.copy(
            eventsCompleted = state.lifetimeStats.eventsCompleted + if (completed) 1 else 0
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
        eventLog = (state.eventLog + EventLogEntry(timestamp, type, outcome, reward)).takeLast(30),
        encounteredEventTypes = if (outcome == EventLogOutcome.STARTED) {
            state.encounteredEventTypes
        } else {
            state.encounteredEventTypes + type
        }
    )

}
