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
        assertTrue(quests.all { it.id.endsWith("_42") })
        assertTrue(quests.all { it.cadence == QuestCadence.DAILY })
    }

    @Test
    fun `weekly quests preserve descriptions and hard difficulty`() {
        val quests = factory.weekly(7, "p1")
        assertEquals(3, quests.size)
        assertEquals("CLICK_PLANET:5000", quests.first().description)
        assertTrue(quests.all { it.difficulty == QuestDifficulty.HARD })
    }
}
