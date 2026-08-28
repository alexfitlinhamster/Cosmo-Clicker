package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedQuestFactoryTest {
    private val factory = TimedQuestFactory { type, target -> "$type:$target" }

    @Test
    fun `daily quests have stable cycle ids and cadence`() {
        val quests = factory.daily(42, "p1")
        assertEquals(5, quests.size)
        assertTrue(quests.all { it.id.startsWith(TimedQuestFactory.DAILY_ID_PREFIX) })
        assertTrue(quests.all { it.id.endsWith("_42") })
        assertTrue(quests.all { it.cadence == QuestCadence.DAILY })
    }

    @Test
    fun `weekly quests preserve descriptions and hard difficulty`() {
        val quests = factory.weekly(7, "p1")
        assertEquals(3, quests.size)
        assertTrue(quests.all { it.id.startsWith(TimedQuestFactory.WEEKLY_ID_PREFIX) })
        assertEquals("CLICK_PLANET:2500", quests.first().description)
        assertTrue(quests.all { it.difficulty == QuestDifficulty.HARD })
    }

    @Test
    fun `debris targets scale with progress but stay achievable`() {
        val early = factory.weekly(7, "p1").last().target
        val late = factory.weekly(7, "p39").last().target

        assertTrue(late > early)
        assertTrue(late <= 30_000.0)
    }
}
