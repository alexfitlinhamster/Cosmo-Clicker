package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventLogCodecTest {
    @Test
    fun eventLogRoundTripsExactly() {
        val entries = listOf(
            EventLogEntry(100L, GameEventType.ASTEROID, EventLogOutcome.STARTED),
            EventLogEntry(200L, GameEventType.ASTEROID, EventLogOutcome.COMPLETED, 12_345.5)
        )
        assertEquals(entries, EventLogCodec.decode(EventLogCodec.encode(entries)))
    }

    @Test
    fun malformedEntriesAreIgnoredAndHistoryIsLimited() {
        assertTrue(EventLogCodec.decode("broken;1,UNKNOWN,STARTED,0").isEmpty())
        val entries = (1L..40L).map {
            EventLogEntry(it, GameEventType.STORM, EventLogOutcome.STARTED)
        }
        val decoded = EventLogCodec.decode(EventLogCodec.encode(entries))
        assertEquals(30, decoded.size)
        assertEquals(11L, decoded.first().timestamp)
        assertEquals(40L, decoded.last().timestamp)
    }

    @Test
    fun startingAndCompletingEventCreatesTwoJournalEntries() {
        val random = object : RandomProvider {
            override fun nextFloat() = 0.5f
            override fun nextInt(until: Int) = 0
            override fun nextLong(until: Long) = 0L
            override fun nextLong(from: Long, until: Long) = from
        }
        val started = EventEngine.startEvent(
            GameState(), GameEventType.ASTEROID, 1_000L, 100L, random
        )
        val completed = EventEngine.onAsteroidClick(started, 200L)

        assertEquals(EventLogOutcome.STARTED, completed.eventLog[0].outcome)
        assertEquals(EventLogOutcome.COMPLETED, completed.eventLog[1].outcome)
        assertTrue(completed.eventLog[1].reward >= 500.0)
    }
}
