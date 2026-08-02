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
    fun asteroidClickAwardsRewardAndResetsEventState() {
        val state = GameState(
            totalDebris = 100.0,
            activeEvent = GameEvent(GameEventType.ASTEROID, 1_000L),
            eventMultiplier = 2.0
        )

        val result = EventEngine.onAsteroidClick(state)

        assertEquals(1_100.0, result.totalDebris, 0.0)
        assertEquals(null, result.activeEvent)
        assertEquals(1.0, result.eventMultiplier, 0.0)
    }

    @Test
    fun blackHoleCompletionCreatesPlanetRewardAndSpeedEffect() {
        val state = GameState(
            currentPlanetId = "p13",
            activeEvent = GameEvent(GameEventType.BLACK_HOLE, 2_000L),
            eventTapsLeft = 1
        )

        val result = EventEngine.onBlackHoleClick(
            state = state,
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
    fun asteroidRewardScalesWithClickValueAndHasMinimum() {
        assertEquals(500.0, EventEngine.calculateAsteroidReward(1.0, FixedRandom(0)), 0.0)
        assertEquals(3_000.0, EventEngine.calculateAsteroidReward(20.0, FixedRandom(100)), 0.0)
    }

    @Test
    fun asteroidRewardIsFixedWhenEventStarts() {
        val result = EventEngine.startEvent(
            state = GameState(),
            type = GameEventType.ASTEROID,
            durationMillis = 20_000L,
            nowMillis = 10_000L,
            random = FixedRandom(0),
            clickValue = 20.0
        )

        assertEquals(10_000L, result.activeEvent?.startedAt)
        assertEquals(30_000L, result.activeEvent?.expiresAt)
        assertEquals(1_000.0, result.activeEvent?.reward ?: 0.0, 0.0)
    }

    @Test
    fun eventTypesAreSelectedByConfiguredWeights() {
        assertEquals(GameEventType.STORM, EventEngine.selectType("p1", FixedRandom(19)))
        assertEquals(GameEventType.ASTEROID, EventEngine.selectType("p1", FixedRandom(20)))
        assertEquals(GameEventType.METEOR_SHOWER, EventEngine.selectType("p1", FixedRandom(45)))
        assertEquals(GameEventType.BLACK_HOLE, EventEngine.selectType("p1", FixedRandom(65)))
        assertEquals(GameEventType.SOLAR_FLARE, EventEngine.selectType("p1", FixedRandom(73)))
        assertEquals(GameEventType.CYBER_VIRUS, EventEngine.selectType("p1", FixedRandom(88)))
    }

    @Test
    fun planetEventModifiersDriveTimingAndProtectionRules() {
        assertEquals(11_538L, EventEngine.nextIntervalMillis("p3", FixedRandom(0)))
        assertEquals(40_000L, EventEngine.nextDurationMillis("p8", FixedRandom(0)))
        assertEquals(GameEventType.ASTEROID, EventEngine.selectType("p12", FixedRandom(0)))
        assertEquals(null, EventEngine.selectType("p16", FixedRandom(0)))
        assertTrue(GameEventType.CYBER_VIRUS in PlanetEventModifiers.forPlanet("p7").blockedEvents)
    }

    @Test
    fun distressSalvageAwardsGuaranteedRewardImmediately() {
        val state = GameState(
            totalDebris = 100.0,
            activeEvent = GameEvent(GameEventType.DISTRESS_SIGNAL, 5_000L, reward = 1_000.0)
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
            activeEvent = GameEvent(GameEventType.DISTRESS_SIGNAL, 5_000L, reward = 1_000.0)
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
    fun cyberVirusWithoutWorkingDroneBecomesCollectibleAsteroid() {
        val state = GameState(
            drones = listOf(DroneData(1L, 0f, 0f, state = DroneState.BROKEN))
        )

        val result = EventEngine.startEvent(
            state, GameEventType.CYBER_VIRUS, 10_000L, 1_000L, FixedRandom(0), clickValue = 20.0
        )

        assertEquals(GameEventType.ASTEROID, result.activeEvent?.type)
        assertEquals(null, result.infectedDroneId)
        assertTrue((result.activeEvent?.reward ?: 0.0) >= 500.0)
    }

    @Test
    fun cyberVirusTheftScalesAndIsBounded() {
        assertEquals(1.0, EventEngine.cyberVirusTheft(0.0), 0.0)
        assertEquals(50.0, EventEngine.cyberVirusTheft(1_000_000.0), 0.0)
        assertEquals(100_000.0, EventEngine.cyberVirusTheft(Double.MAX_VALUE), 0.0)
    }
}
