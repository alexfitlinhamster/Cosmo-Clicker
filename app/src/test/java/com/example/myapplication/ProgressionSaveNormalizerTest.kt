package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressionSaveNormalizerTest {
    @Test
    fun outerGalaxyProgressRemainsOnItsOwnedWorld() {
        val owned = ProgressionSaveNormalizer.ownedPlanets(setOf("p1", "p24", "p39"))

        assertEquals(setOf("p1", "p24", "p39"), owned)
        assertEquals("p39", ProgressionSaveNormalizer.currentPlanet("p39", owned))
    }

    @Test
    fun malformedOrUnownedCurrentWorldFallsBackSafely() {
        val owned = ProgressionSaveNormalizer.ownedPlanets(setOf("invalid", "p0", "p2"))

        assertEquals(setOf("p1", "p2"), owned)
        assertEquals("p1", ProgressionSaveNormalizer.currentPlanet("invalid", owned))
        assertEquals("p1", ProgressionSaveNormalizer.currentPlanet("p8", owned))
    }

    @Test
    fun eventDiscoveriesMigrateFromRollingLogWhenDedicatedSaveIsAbsent() {
        val log = listOf(
            EventLogEntry(1L, GameEventType.STORM, EventLogOutcome.STARTED),
            EventLogEntry(2L, GameEventType.STORM, EventLogOutcome.COMPLETED),
            EventLogEntry(3L, GameEventType.PIRATE_RAID, EventLogOutcome.FAILURE)
        )

        assertEquals(
            setOf(GameEventType.STORM, GameEventType.PIRATE_RAID),
            EventDiscoveryNormalizer.restore(emptySet(), log)
        )
    }
}
