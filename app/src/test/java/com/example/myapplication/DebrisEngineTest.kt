package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebrisEngineTest {
    private class FixedRandom(private val intValue: Int = 0, private val longValue: Long = 10) : RandomProvider {
        override fun nextFloat() = 0f
        override fun nextInt(until: Int) = intValue.coerceIn(0, until - 1)
        override fun nextLong(until: Long) = longValue.coerceIn(0, until - 1)
        override fun nextLong(from: Long, until: Long) = longValue.coerceIn(from, until - 1)
    }

    @Test
    fun `void debris always uses legendary image`() {
        assertEquals(6, DebrisEngine.imageIndex(Rarity.VOID, FixedRandom()))
    }

    @Test
    fun `crystal planet doubles non legendary reward on successful roll`() {
        val crystal = DebrisEngine.reward(Rarity.COMMON, "p4", FixedRandom())
        assertEquals(EconomyBalance.scaledReward(20.0, "p4"), crystal, 0.0001)
    }

    @Test
    fun `image pools stay within registered debris range`() {
        Rarity.entries.forEach { rarity ->
            assertTrue(DebrisEngine.imageIndex(rarity, FixedRandom()) in 1..14)
        }
    }

    @Test
    fun `zero rarity roll selects first rarity`() {
        assertEquals(
            Rarity.COMMON,
            DebrisEngine.rarity("p1", emptySet(), emptyMap(), 0L, FixedRandom())
        )
    }

    @Test
    fun `rarity selection always returns a defined value with active bonuses`() {
        val rarity = DebrisEngine.rarity(
            planetId = "p15",
            technologies = setOf(Technology.LUCK_MATRIX),
            activeEffects = mapOf(
                SkillType.TRADE_LUCK.id to 2_000L,
                SkillType.SALVAGE_RUSH.id to 2_000L
            ),
            nowMillis = 1_000L,
            random = FixedRandom(intValue = Int.MAX_VALUE)
        )
        assertTrue(rarity in Rarity.entries)
    }
}
