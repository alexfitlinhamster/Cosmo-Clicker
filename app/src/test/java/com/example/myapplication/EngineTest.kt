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
            activeEvent = GameEvent(GameEventType.BLACK_HOLE, "", 10_000L),
            activeEffects = mapOf("expired" to 99L, "active" to 101L)
        )
        val result = EconomyEngine.processTick(state, 100L)

        assertEquals(995.0, result.totalDebris, 0.0)
        assertFalse("expired" in result.activeEffects)
        assertTrue("active" in result.activeEffects)
    }
}
