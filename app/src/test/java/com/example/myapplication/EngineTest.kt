package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {
    private class FixedRandom(private val value: Int) : RandomProvider {
        override fun nextFloat() = 0.5f
        override fun nextInt(until: Int) = value.coerceIn(0, until - 1)
        override fun nextLong(until: Long) = 0L
        override fun nextLong(from: Long, until: Long) = from
    }

    @Test
    fun clickCriticalCanBeTestedDeterministically() {
        val state = GameState(currentPlanetId = "p1", clickLevels = mapOf("magnet" to 2))
        val item = ItemConfig("magnet", "Magnet", 10.0, 5.0, 0)

        assertEquals(22.0, EconomyEngine.calculateClickValue(state, listOf(item), FixedRandom(0)), 0.0)
        assertEquals(11.0, EconomyEngine.calculateClickValue(state, listOf(item), FixedRandom(99)), 0.0)
    }

    @Test
    fun questProgressIsClampedAndCompleted() {
        val quest = Quest("q", QuestType.COLLECT_DEBRIS, "", 10.0, 8.0)
        val result = QuestEngine.advance(listOf(quest), QuestType.COLLECT_DEBRIS, 5.0).single()

        assertEquals(10.0, result.progress, 0.0)
        assertTrue(result.isCompleted)
    }

    @Test
    fun oneActionAdvancesMatchingDailyAndWeeklyQuests() {
        val quests = listOf(
            Quest("daily", QuestType.COMPLETE_EVENT, "", 2.0, 0.0, cadence = QuestCadence.DAILY),
            Quest("weekly", QuestType.COMPLETE_EVENT, "", 25.0, 0.0, cadence = QuestCadence.WEEKLY),
            Quest("clicks", QuestType.CLICK_PLANET, "", 10.0, 0.0)
        )

        val result = QuestEngine.advance(quests, QuestType.COMPLETE_EVENT)

        assertEquals(1.0, result[0].progress, 0.0)
        assertEquals(1.0, result[1].progress, 0.0)
        assertEquals(0.0, result[2].progress, 0.0)
    }

    @Test
    fun specificDroneQuestOnlyAdvancesForRequestedDrone() {
        val quest = Quest(
            id = "specific-drone",
            type = QuestType.OBTAIN_DRONE,
            description = "",
            target = 1.0,
            progress = 0.0,
            targetDroneId = "drone_07"
        )

        val wrongDrop = QuestEngine.advance(listOf(quest), QuestType.OBTAIN_DRONE, droneId = "drone_03").single()
        val requestedDrop = QuestEngine.advance(listOf(quest), QuestType.OBTAIN_DRONE, droneId = "drone_07").single()

        assertEquals(0.0, wrongDrop.progress, 0.0)
        assertEquals(1.0, requestedDrop.progress, 0.0)
        assertTrue(requestedDrop.isCompleted)
    }

    @Test
    fun rareDroneQuestRejectsCommonDropAndAcceptsRareDrop() {
        val quest = Quest("rare", QuestType.OBTAIN_RARE_DRONE, "", 1.0, 0.0)

        val common = QuestEngine.advance(
            listOf(quest), QuestType.OBTAIN_RARE_DRONE, droneRarity = Rarity.COMMON
        ).single()
        val rare = QuestEngine.advance(
            listOf(quest), QuestType.OBTAIN_RARE_DRONE, droneRarity = Rarity.RARE
        ).single()

        assertEquals(0.0, common.progress, 0.0)
        assertTrue(rare.isCompleted)
    }

    @Test
    fun economyTickExpiresEffectsAndAppliesBlackHoleDrain() {
        val state = GameState(
            totalDebris = 1_000.0,
            activeEvent = GameEvent(GameEventType.BLACK_HOLE, 10_000L),
            activeEffects = mapOf("expired" to 99L, "active" to 101L)
        )
        val result = EconomyEngine.processTick(state, 100L)

        assertEquals(995.0, result.totalDebris, 0.0)
        assertFalse("expired" in result.activeEffects)
        assertTrue("active" in result.activeEffects)
    }

    @Test
    fun pirateShipJamsAllPassiveIncome() {
        val state = GameState(
            totalDebris = 1_000.0,
            activeEvent = GameEvent(GameEventType.PIRATE_RAID, Long.MAX_VALUE),
            eventTapsLeft = 5
        )

        val result = EconomyEngine.processTick(state, 100L, passiveIncome = 500.0)

        assertEquals(1_000.0, result.totalDebris, 0.0)
    }

    @Test
    fun expiredEventIsFullyClearedWithoutApplyingItsEconomyEffect() {
        val state = GameState(
            totalDebris = 1_000.0,
            activeEvent = GameEvent(GameEventType.BLACK_HOLE, 100L),
            eventMultiplier = 3.0,
            eventTapsLeft = 4,
            infectedDroneId = 42L
        )

        val result = EconomyEngine.processTick(state, 100L)

        assertEquals(1_000.0, result.totalDebris, 0.0)
        assertEquals(null, result.activeEvent)
        assertEquals(1.0, result.eventMultiplier, 0.0)
        assertEquals(0, result.eventTapsLeft)
        assertEquals(null, result.infectedDroneId)
    }

    @Test
    fun activeEventIsNotClearedBeforeItsDeadline() {
        val event = GameEvent(GameEventType.CYBER_VIRUS, 101L)
        val state = GameState(activeEvent = event, infectedDroneId = 42L)

        val result = EventEngine.expireEventIfNeeded(state, 100L)

        assertEquals(event, result.activeEvent)
        assertEquals(42L, result.infectedDroneId)
    }

    @Test
    fun startingCyberVirusInfectsOnlyWorkingDrone() {
        val state = GameState(
            drones = listOf(
                DroneData(id = 1L, x = 0f, y = 0f, state = DroneState.BROKEN),
                DroneData(id = 2L, x = 0f, y = 0f)
            )
        )

        val result = EventEngine.startEvent(
            state = state,
            type = GameEventType.CYBER_VIRUS,
            durationMillis = 5_000L,
            nowMillis = 10_000L,
            random = FixedRandom(0)
        )

        assertEquals(GameEventType.CYBER_VIRUS, result.activeEvent?.type)
        assertEquals(15_000L, result.activeEvent?.expiresAt)
        assertEquals(2L, result.infectedDroneId)
    }




    @Test
    fun blackHoleCompletionCreatesPlanetRewardAndSpeedEffect() {
        val state = GameState(
            currentPlanetId = "p13",
            activeEvent = GameEvent(GameEventType.BLACK_HOLE, 2_000L),
            eventTapsLeft = 1,
            stormSequence = listOf(0),
            stormProgress = 5,
            stormRound = 0
        )

        val result = EventEngine.onBlackHoleNodeClick(
            state = state,
            node = 0,
            nowMillis = 1_000L,
            random = FixedRandom(0)
        ) { x, y, expiresAt ->
            ScavengeTarget(id = expiresAt + x.toLong() + y.toLong(), x = x, y = y, expiresAt = expiresAt)
        }

        assertEquals(null, result.activeEvent)
        assertEquals(0, result.eventTapsLeft)
        assertEquals(5, result.scavengeTargets.size)
        assertEquals(31_000L, result.activeEffects[SkillType.VOID_ENERGY.id])
    }

    @Test
    fun wrongBlackHoleNodeRaisesInstabilityAndConsumesDebris() {
        val state = GameState(
            totalDebris = 10_000.0,
            activeEvent = GameEvent(GameEventType.BLACK_HOLE, 20_000L),
            eventTapsLeft = 6,
            stormSequence = listOf(1),
            stormRound = 0
        )

        val result = EventEngine.onBlackHoleNodeClick(
            state = state,
            node = 2,
            nowMillis = 1_000L,
            random = FixedRandom(3)
        ) { x, y, expiresAt ->
            ScavengeTarget(id = expiresAt, x = x, y = y, expiresAt = expiresAt)
        }

        assertEquals(9_900.0, result.totalDebris, 0.0)
        assertEquals(1, result.stormRound)
        assertEquals(listOf(3), result.stormSequence)
        assertEquals(6, result.eventTapsLeft)
    }




    @Test
    fun rareEventChanceGrowsAcrossGalaxyAndExcludesTradingShip() {
        assertEquals(3, EventEngine.eliteChancePercent("p1"))
        assertEquals(7, EventEngine.eliteChancePercent("p25"))
        assertTrue(EventEngine.rollElite(GameEventType.PIRATE_RAID, "p25", FixedRandom(6)))
        assertFalse(EventEngine.rollElite(GameEventType.PIRATE_RAID, "p25", FixedRandom(7)))
        assertFalse(EventEngine.rollElite(GameEventType.TRADING_SHIP, "p25", FixedRandom(0)))
    }

    @Test
    fun randomEventPoolContainsOnlyFieldEvents() {
        val selected = listOf(0, 31, 32, 51, 52, 63, 64, 75, 76, 85)
            .mapNotNull { EventEngine.selectType("p1", FixedRandom(it)) }
            .toSet()

        assertEquals(
            setOf(
                GameEventType.METEOR_SHOWER,
                GameEventType.SOLAR_FLARE,
                GameEventType.CYBER_VIRUS,
                GameEventType.PIRATE_RAID,
                GameEventType.TRADING_SHIP
            ),
            selected
        )
    }

    @Test
    fun dronesDestroyFlyingShipAndAwardCurrency() {
        val event = GameEvent(GameEventType.TRADING_SHIP, expiresAt = 20_000L, reward = 500.0)
        val state = GameState(
            totalDebris = 100.0,
            activeEvent = event,
            eventTapsLeft = 1,
            drones = listOf(DroneData(1L, .2f, .2f))
        )

        val result = EventEngine.tickTradingShipCombat(state, 1_000L, FixedRandom(0)) { x, y, reward ->
            ScavengeTarget(1L, x, y, reward = reward)
        }

        assertEquals(null, result.activeEvent)
        assertEquals(1_100.0, result.totalDebris, 0.0)
        assertEquals(1, result.lifetimeStats.eventsCompleted)
    }


    @Test
    fun planetEventModifiersDriveTimingAndProtectionRules() {
        assertEquals(34_615L, EventEngine.nextIntervalMillis("p3", FixedRandom(0)))
        assertEquals(40_000L, EventEngine.nextDurationMillis("p8", FixedRandom(0)))
        assertEquals(0.25, PlanetEventModifiers.forPlanet("p12").negativeEventResistance, 0.0)
        assertEquals(NegativeEventResistanceOutcome.CANCEL, PlanetEventModifiers.forPlanet("p16").resistanceOutcome)
        assertTrue(GameEventType.CYBER_VIRUS in PlanetEventModifiers.forPlanet("p7").blockedEvents)
    }

    @Test
    fun distressSalvageAwardsGuaranteedRewardImmediately() {
        val state = GameState(
            totalDebris = 100.0,
            activeEvent = GameEvent(GameEventType.DISTRESS_SIGNAL, 5_000L, reward = 1_000.0),
            stormRound = 2
        )

        val result = EventEngine.respondToDistressSignal(
            state, DistressChoice.SALVAGE, 1_000L, FixedRandom(99)
        )

        assertEquals(1_100.0, result.totalDebris, 0.0)
        assertEquals(null, result.activeEvent)
        assertTrue(result.eventChainResult?.success == true)
    }

    @Test
    fun distressRescueResolvesOnTickAfterDelay() {
        val state = GameState(
            totalDebris = 100.0,
            activeEvent = GameEvent(GameEventType.DISTRESS_SIGNAL, 5_000L, reward = 1_000.0),
            stormRound = 2
        )
        val pending = EventEngine.respondToDistressSignal(
            state, DistressChoice.RESCUE, 1_000L, FixedRandom(0)
        )

        assertEquals(100.0, pending.totalDebris, 0.0)
        assertEquals(11_000L, pending.pendingEventChain?.resolvesAt)
        assertEquals(100.0, EconomyEngine.processTick(pending, 10_999L).totalDebris, 0.0)

        val resolved = EconomyEngine.processTick(pending, 11_000L)
        assertEquals(3_100.0, resolved.totalDebris, 0.0)
        assertEquals(null, resolved.pendingEventChain)
        assertTrue(resolved.eventChainResult?.success == true)
    }

    @Test
    fun distressSignalMustBeTriangulatedBeforeChoosingResponse() {
        val event = GameEvent(GameEventType.DISTRESS_SIGNAL, 20_000L, reward = 1_000.0)
        val initial = GameState(
            totalDebris = 100.0,
            activeEvent = event,
            eventTapsLeft = 3,
            stormSequence = listOf(0),
            stormRound = 1
        )

        val blocked = EventEngine.respondToDistressSignal(
            initial, DistressChoice.SALVAGE, 1_000L, FixedRandom(0)
        )
        assertEquals(initial, blocked)

        val located = (1..3).fold(initial) { state, _ ->
            EventEngine.onDistressSignalNode(state, 0, FixedRandom(0))
        }
        assertEquals(2, located.stormRound)
        assertEquals(3, located.stormProgress)
        assertEquals(0, located.eventTapsLeft)
    }

    @Test
    fun wrongDistressBeaconLosesProgressAndDebris() {
        val state = GameState(
            totalDebris = 10_000.0,
            activeEvent = GameEvent(GameEventType.DISTRESS_SIGNAL, 20_000L),
            eventTapsLeft = 2,
            stormSequence = listOf(1),
            stormProgress = 1,
            stormRound = 1
        )

        val result = EventEngine.onDistressSignalNode(state, 2, FixedRandom(3))
        assertEquals(9_950.0, result.totalDebris, 0.0)
        assertEquals(0, result.stormProgress)
        assertEquals(listOf(3), result.stormSequence)
    }


    @Test
    fun cyberVirusTheftScalesAndIsBounded() {
        assertEquals(1.0, EventEngine.cyberVirusTheft(0.0), 0.0)
        assertEquals(50.0, EventEngine.cyberVirusTheft(1_000_000.0), 0.0)
        assertEquals(100_000.0, EventEngine.cyberVirusTheft(Double.MAX_VALUE), 0.0)
    }

    @Test
    fun cyberVirusMinigameRewardsSuccess() {
        val state = GameState(
            totalDebris = 10_000.0,
            activeEvent = GameEvent(GameEventType.CYBER_VIRUS, 10_000L, reward = 5_000.0),
            infectedDroneId = 1L
        )
        val result = EventEngine.resolveCyberVirus(state, success = true, nowMillis = 2_000L)
        assertEquals(15_000.0, result.totalDebris, 0.0)
        assertEquals(null, result.activeEvent)
        assertTrue(result.eventChainResult?.success == true)
    }

    @Test
    fun cyberVirusMinigameFailureTakesThreePercentAndDisablesDrone() {
        val state = GameState(
            totalDebris = 10_000.0,
            drones = listOf(DroneData(1L, 0f, 0f)),
            activeEvent = GameEvent(GameEventType.CYBER_VIRUS, 10_000L),
            infectedDroneId = 1L
        )
        val result = EventEngine.resolveCyberVirus(state, success = false, nowMillis = 2_000L)
        assertEquals(9_700.0, result.totalDebris, 0.0)
        assertEquals(62_000L, result.drones.single().disabledUntil)
        assertTrue(result.eventChainResult?.success == false)
    }

    @Test
    fun abandonedStationSafeRouteHasShortDelayAndScaledReward() {
        val state = GameState(
            activeEvent = GameEvent(GameEventType.ABANDONED_STATION, 20_000L, reward = 1_000.0),
            stormRound = 2
        )
        val pending = EventEngine.respondToAbandonedStation(
            state, StationChoice.SAFE_ROUTE, 1_000L, FixedRandom(0)
        )

        assertEquals(9_000L, pending.pendingEventChain?.resolvesAt)
        assertEquals(1_500.0, pending.pendingEventChain?.reward ?: 0.0, 0.0)
        assertEquals(GameEventType.ABANDONED_STATION, pending.pendingEventChain?.eventType)
    }

    @Test
    fun abandonedStationReactorFailureAppliesTwoPercentPenaltyOnResolution() {
        val state = GameState(
            totalDebris = 10_000.0,
            activeEvent = GameEvent(GameEventType.ABANDONED_STATION, 20_000L, reward = 1_000.0),
            stormRound = 2
        )
        val pending = EventEngine.respondToAbandonedStation(
            state, StationChoice.REACTOR_CORE, 1_000L, FixedRandom(99)
        )
        assertEquals(16_000L, pending.pendingEventChain?.resolvesAt)
        assertEquals(200.0, pending.pendingEventChain?.failurePenalty ?: 0.0, 0.0)

        val resolved = EventEngine.resolvePendingChainIfNeeded(pending, 16_000L)
        assertEquals(9_800.0, resolved.totalDebris, 0.0)
        assertEquals(200.0, resolved.eventChainResult?.loss ?: 0.0, 0.0)
        assertTrue(resolved.eventChainResult?.success == false)
    }

    @Test
    fun abandonedStationRouteIsLockedUntilRelaysAreRestored() {
        val event = GameEvent(GameEventType.ABANDONED_STATION, 20_000L, reward = 1_000.0)
        val initial = GameState(
            activeEvent = event,
            eventTapsLeft = 3,
            stormSequence = listOf(0),
            stormRound = 1
        )

        val blocked = EventEngine.respondToAbandonedStation(
            initial, StationChoice.SAFE_ROUTE, 1_000L, FixedRandom(0)
        )
        assertEquals(initial, blocked)

        val opened = (1..3).fold(initial) { state, _ ->
            EventEngine.onStationRelayClick(state, 0, FixedRandom(0))
        }
        assertEquals(2, opened.stormRound)
        assertEquals(3, opened.stormProgress)
        assertEquals(0, opened.eventTapsLeft)
    }

    @Test
    fun wrongStationRelayDamagesResourcesAndReducesRewardQuality() {
        val state = GameState(
            totalDebris = 10_000.0,
            activeEvent = GameEvent(GameEventType.ABANDONED_STATION, 20_000L),
            eventTapsLeft = 3,
            stormSequence = listOf(1),
            stormRound = 1
        )

        val result = EventEngine.onStationRelayClick(state, 2, FixedRandom(0))
        assertEquals(9_925.0, result.totalDebris, 0.0)
        assertEquals(0.92, result.eventMultiplier, 0.0001)
        assertEquals(listOf(0), result.stormSequence)
    }

    @Test
    fun pirateRaidFreezesIncomeWithoutDrainingSavedDebris() {
        val state = GameState(
            totalDebris = 10_000.0,
            activeEvent = GameEvent(GameEventType.PIRATE_RAID, 2_000L),
            eventTapsLeft = 6
        )
        val result = EconomyEngine.processTick(state, 1_000L)
        assertEquals(10_000.0, result.totalDebris, 0.0)
        assertEquals(1.0, EventEngine.pirateRaidTheft(0.0), 0.0)
        assertEquals(5_000_000.0, EventEngine.pirateRaidTheft(Double.MAX_VALUE), 0.0)
    }

    @Test
    fun piratePursuitRequiresTrackingBeforeFinalDecision() {
        val state = GameState(
            totalDebris = 100.0,
            activeEvent = GameEvent(GameEventType.PIRATE_RAID, 2_000L, reward = 3_000.0),
            eventTapsLeft = 1,
            stormSequence = listOf(1),
            stormProgress = 4,
            stormRound = 1
        )

        val disabled = EventEngine.onPirateTargetClick(state, 1, 1_000L, FixedRandom(2))
        assertEquals(2, disabled.stormRound)
        assertEquals(5, disabled.stormProgress)
        assertEquals(0, disabled.eventTapsLeft)

        val result = EventEngine.resolvePirateRaid(disabled, captureCargo = false, nowMillis = 1_100L)
        assertEquals(3_400.0, result.totalDebris, 0.0001)
        assertEquals(null, result.activeEvent)
        assertEquals(EventLogOutcome.COMPLETED, result.eventLog.last().outcome)
        assertEquals(3_300.0, result.eventLog.last().reward, 0.0001)
        assertTrue(GameEventType.PIRATE_RAID in result.encounteredEventTypes)
        assertEquals(31_100L, result.activeEffects[SkillType.VOID_ENERGY.id])
    }

    @Test
    fun missingPirateSignalCostsDebrisAndMovesTarget() {
        val state = GameState(
            totalDebris = 10_000.0,
            activeEvent = GameEvent(GameEventType.PIRATE_RAID, 20_000L, reward = 1_000.0),
            eventTapsLeft = 5,
            stormSequence = listOf(1)
        )

        val result = EventEngine.onPirateTargetClick(state, 0, 1_000L, FixedRandom(2))
        assertEquals(9_980.0, result.totalDebris, 0.0)
        assertEquals(0.92, result.eventMultiplier, 0.0001)
        assertEquals(listOf(2), result.stormSequence)
    }

    @Test
    fun tradingShipPowerCoreCostsDebrisAndActivatesTimedBoost() {
        val state = GameState(
            totalDebris = 5_000.0,
            activeEvent = GameEvent(GameEventType.TRADING_SHIP, 20_000L, reward = 1_000.0),
            stormRound = 2
        )
        val result = EventEngine.buyTradeOffer(state, TradeOffer.POWER_CORE, 2_000L)

        assertEquals(4_000.0, result.totalDebris, 0.0)
        assertEquals(62_000L, result.activeEffects[SkillType.TRADE_POWER.id])
        assertEquals(null, result.activeEvent)
        assertEquals(EventLogOutcome.CHOICE, result.eventLog.last().outcome)
    }

    @Test
    fun tradingShipDoesNotCloseWhenPlayerCannotAffordOffer() {
        val event = GameEvent(GameEventType.TRADING_SHIP, 20_000L, reward = 1_000.0)
        val state = GameState(totalDebris = 100.0, activeEvent = event)
        val result = EventEngine.buyTradeOffer(state, TradeOffer.LUCK_SCANNER, 2_000L)

        assertEquals(state, result)
    }

    @Test
    fun tradingShipCargoChangesBetweenVisitsAndContainsThreeUniqueOffers() {
        val first = EventEngine.tradeOffers(GameEvent(GameEventType.TRADING_SHIP, 20_000L, startedAt = 1_000L))
        val second = EventEngine.tradeOffers(GameEvent(GameEventType.TRADING_SHIP, 30_000L, startedAt = 2_000L))

        assertEquals(3, first.size)
        assertEquals(3, first.toSet().size)
        assertTrue(first != second)
    }

    @Test
    fun tradingShipCanSellCasesDebrisAndDrones() {
        fun stateAt(startedAt: Long = 7_000L) = GameState(
            totalDebris = 30_000.0,
            activeEvent = GameEvent(GameEventType.TRADING_SHIP, 20_000L, startedAt = startedAt, reward = 1_000.0),
            stormRound = 2
        )

        val caseResult = EventEngine.buyTradeOffer(stateAt(), TradeOffer.RARE_CASE, 2_000L)
        assertTrue(caseResult.isOpeningCase)
        assertEquals(CaseType.RARE, caseResult.openingCaseType)

        val debrisResult = EventEngine.buyTradeOffer(stateAt(), TradeOffer.DEBRIS_CARGO, 2_000L)
        assertEquals(31_250.0, debrisResult.totalDebris, 0.0)

        val droneResult = EventEngine.buyTradeOffer(stateAt(), TradeOffer.RANDOM_DRONE, 2_000L)
        assertEquals(1, droneResult.fleetCounts.values.sum())
        assertEquals(1, droneResult.discoveredDroneIds.size)
    }


    @Test
    fun tradingShipHasSeparateTapAndFleetBoosts() {
        val event = GameEvent(GameEventType.TRADING_SHIP, 20_000L, reward = 1_000.0)
        val state = GameState(totalDebris = 5_000.0, activeEvent = event, stormRound = 2)

        val tap = EventEngine.buyTradeOffer(state, TradeOffer.CLICK_AMPLIFIER, 2_000L)
        val fleet = EventEngine.buyTradeOffer(state, TradeOffer.FLEET_OVERDRIVE, 2_000L)
        assertEquals(47_000L, tap.activeEffects[SkillType.TRADE_CLICK_BOOST.id])
        assertEquals(62_000L, fleet.activeEffects[SkillType.TRADE_FLEET_BOOST.id])
    }

}
