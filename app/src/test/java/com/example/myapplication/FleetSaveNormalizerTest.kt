package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Test

class FleetSaveNormalizerTest {
    @Test
    fun `owned counts reject negatives and respect total capacity`() {
        val normalized = FleetSaveNormalizer.owned(
            linkedMapOf("broken" to -4, "first" to 8, "second" to 8),
            capacity = 10
        )

        assertEquals(mapOf("broken" to 0, "first" to 8, "second" to 2), normalized)
    }

    @Test
    fun `active counts allow at most one copy of each drone`() {
        val normalized = FleetSaveNormalizer.active(
            linkedMapOf("first" to 9, "second" to 1, "third" to 1),
            capacity = 2
        )

        assertEquals(mapOf("first" to 1, "second" to 1, "third" to 0), normalized)
    }

    @Test
    fun `negative capacity disables every active drone`() {
        assertEquals(
            mapOf("first" to 0),
            FleetSaveNormalizer.active(mapOf("first" to 1), capacity = -1)
        )
    }
}
