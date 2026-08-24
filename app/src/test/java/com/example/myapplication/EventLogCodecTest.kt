package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventLogCodecTest {

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

}
