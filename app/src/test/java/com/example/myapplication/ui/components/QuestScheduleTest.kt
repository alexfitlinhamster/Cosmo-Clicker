package com.example.myapplication.ui.components

import com.example.myapplication.QuestCadence
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestScheduleTest {
    @Test
    fun dailyResetIsTheNextLocalMidnight() {
        val now = System.currentTimeMillis()
        val delay = nextQuestResetAt(now, QuestCadence.DAILY) - now
        assertTrue(delay in 1L..(24L * 60L * 60L * 1_000L))
    }

    @Test
    fun weeklyResetIsWithinTheNextSevenDays() {
        val now = System.currentTimeMillis()
        val delay = nextQuestResetAt(now, QuestCadence.WEEKLY) - now
        assertTrue(delay in 1L..(7L * 24L * 60L * 60L * 1_000L))
    }
}
