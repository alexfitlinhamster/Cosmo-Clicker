package com.example.myapplication

object EventEngine {
    enum class Category { POSITIVE, NEGATIVE, MIXED }

    private const val MIN_INTERVAL_MS = 45_000L
    private const val INTERVAL_RANGE_MS = 45_000L
    private const val MIN_DURATION_MS = 20_000L
    private const val MAX_DURATION_MS = 60_000L
    private const val EVENT_CLICK_MULTIPLIER = 2.0
    private const val BLACK_HOLE_TAPS = 10
    private const val ASTEROID_TAPS = 5
    private const val GOLDEN_SHARD_COUNT = 5
    const val SALVAGE_RUSH_DURATION_MS = 15_000L
    private const val STORM_TAPS = 3
    private const val SOLAR_FLARE_TAPS = 4
    private const val PIRATE_RAID_TAPS = 6
    private const val ASTEROID_REWARD = 250.0
    private const val VOID_ENERGY_DURATION_MS = 30_000L
    private const val RARE_TARGET_DURATION_MS = 30_000L
    private const val DISTRESS_RESCUE_DURATION_MS = 10_000L
    private const val STATION_SAFE_DURATION_MS = 8_000L
    private const val STATION_REACTOR_DURATION_MS = 15_000L
    private const val TRADE_POWER_DURATION_MS = 60_000L
    private const val TRADE_LUCK_DURATION_MS = 90_000L
    private const val TRADE_CLICK_DURATION_MS = 45_000L
    private const val TRADE_FLEET_DURATION_MS = 60_000L

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
                GameEventType.ASTEROID -> ASTEROID_TAPS
                GameEventType.STORM -> STORM_TAPS
                GameEventType.SOLAR_FLARE -> SOLAR_FLARE_TAPS
                GameEventType.PIRATE_RAID -> PIRATE_RAID_TAPS
                else -> 0
            },
            infectedDroneId = infectedDroneId,
            stormSequence = when (actualType) {
                GameEventType.STORM -> List(3) { random.nextInt(3) }
                GameEventType.SOLAR_FLARE -> List(3) { random.nextInt(4) }
                else -> emptyList()
            },
            stormProgress = 0,
            stormRound = 1
        )
    }

    fun expireEventIfNeeded(state: GameState, nowMillis: Long): GameState {
        val event = state.activeEvent ?: return state
        return if (event.expiresAt <= nowMillis) {
            finishEvent(state, EventLogOutcome.EXPIRED, nowMillis)
        } else state
    }

    fun onAsteroidClick(
        state: GameState,
        nowMillis: Long,
        random: RandomProvider = KotlinRandomProvider
    ): GameState {
        val event = state.activeEvent
        if (event?.type != GameEventType.ASTEROID) return state
        val reward = event.reward.takeIf { it > 0.0 }
            ?: (ASTEROID_REWARD * state.eventMultiplier)
        val tapsLeft = state.eventTapsLeft - 1
        val hitNumber = (ASTEROID_TAPS - state.eventTapsLeft + 1).coerceIn(1, ASTEROID_TAPS)
        val hitReward = reward * hitNumber / 15.0
        if (tapsLeft > 0) return state.copy(
            totalDebris = state.totalDebris + hitReward,
            eventTapsLeft = tapsLeft,
            activeEvent = event.copy(
                x = 0.12f + random.nextFloat() * 0.76f,
                y = 0.12f + random.nextFloat() * 0.52f
            )
        )
        val fragments = List(GOLDEN_SHARD_COUNT) { index ->
            val angle = Math.PI * 2.0 * index / GOLDEN_SHARD_COUNT
            ScavengeTarget(
                id = Long.MIN_VALUE + event.startedAt + index,
                x = GameRules.clampDebrisSpawnCoordinate(event.x + kotlin.math.cos(angle).toFloat() * 0.12f),
                y = GameRules.clampDebrisSpawnCoordinate(event.y + kotlin.math.sin(angle).toFloat() * 0.12f),
                rarity = Rarity.LEGENDARY,
                expiresAt = nowMillis + 20_000L,
                imageIndex = 6,
                reward = reward * 0.12,
                isGoldenShard = true
            )
        }
        return finishEvent(
            state.copy(totalDebris = state.totalDebris + hitReward),
            EventLogOutcome.COMPLETED,
            nowMillis,
            reward
        ).copy(
            scavengeTargets = state.scavengeTargets + fragments,
            goldenShardsRemaining = GOLDEN_SHARD_COUNT
        )
    }

    fun collectGoldenShard(state: GameState, targetId: Long, nowMillis: Long): GameState {
        val shard = state.scavengeTargets.firstOrNull { it.id == targetId && it.isGoldenShard } ?: return state
        val remaining = (state.goldenShardsRemaining - 1).coerceAtLeast(0)
        return state.copy(
            totalDebris = state.totalDebris + shard.reward,
            scavengeTargets = state.scavengeTargets.filterNot { it.id == targetId },
            goldenShardsRemaining = remaining,
            activeEffects = if (remaining == 0) {
                state.activeEffects + (SkillType.SALVAGE_RUSH.id to nowMillis + SALVAGE_RUSH_DURATION_MS)
            } else state.activeEffects
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

    fun calculateAsteroidReward(clickValue: Double, random: RandomProvider): Double {
        val multiplier = 50 + random.nextInt(101)
        return (clickValue.coerceAtLeast(0.0) * multiplier).coerceIn(ASTEROID_REWARD, 25_000.0)
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
        eventLog = (state.eventLog + EventLogEntry(timestamp, type, outcome, reward)).takeLast(30)
    )

}
