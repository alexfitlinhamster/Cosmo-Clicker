package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEngineTest {
    @Test fun dailyRewardCanOnlyBeClaimedOncePerDay() {
        val now = 1_788_134_400_000L
        val claimed = DailyRewardEngine.claim(GameState(), now)!!
        assertNotNull(claimed)
        assertFalse(DailyRewardEngine.canClaim(claimed, now))
        assertNull(DailyRewardEngine.claim(claimed, now))
    }

    @Test fun dailyRewardBuildsASevenDayStreakAndThenCycles() {
        var state = GameState()
        val start = 1_788_134_400_000L
        repeat(7) { offset -> state = DailyRewardEngine.claim(state, start + offset * 86_400_000L)!! }
        assertEquals(7, state.dailyRewardStreak)
        assertTrue(state.prestigePoints >= 1)
        state = DailyRewardEngine.claim(state, start + 7 * 86_400_000L)!!
        assertEquals(1, state.dailyRewardStreak)
    }

    @Test fun overallProgressIsBoundedAndReachesOneForCompletedInputs() {
        val ids = listOf("a", "b")
        assertEquals(0, OverallProgressEngine.percent(GameState(ownedPlanets = emptySet()), ids))
        val complete = GameState(
            ownedPlanets = (1..39).mapTo(mutableSetOf()) { "p$it" },
            clickLevels = ids.associateWith { 1 },
            claimedAchievementIds = AchievementEngine.definitions.mapTo(mutableSetOf()) { it.id },
            technologies = Technology.entries.toSet(),
            stationLevels = StationModule.entries.associateWith { 5 }
        )
        assertEquals(100, OverallProgressEngine.percent(complete, ids))
    }

    @Test fun progressCategoriesExplainTheirOwnCompletion() {
        val state = GameState(
            ownedPlanets = setOf("p1", "p2"),
            clickLevels = mapOf("laser" to 1),
            technologies = setOf(Technology.POWER_CORE),
            stationLevels = mapOf(StationModule.HANGAR to 2)
        )

        val categories = OverallProgressEngine.categories(state, listOf("laser", "matrix"))

        assertEquals(5, categories.size)
        assertEquals(2, categories.first { it.id == "planets" }.completed)
        assertEquals(50, categories.first { it.id == "upgrades" }.percent)
    }

    @Test fun galacticCollectionUsesExistingJourneyData() {
        val state = GameState(
            ownedPlanets = setOf("p1", "p2", "p3"),
            discoveredDroneIds = setOf("drone_1", "drone_2"),
            claimedAchievementIds = setOf("click_100"),
            encounteredEventTypes = setOf(GameEventType.STORM, GameEventType.PIRATE_RAID),
            eventLog = listOf(
                EventLogEntry(1L, GameEventType.STORM, EventLogOutcome.COMPLETED),
                EventLogEntry(2L, GameEventType.STORM, EventLogOutcome.SUCCESS),
                EventLogEntry(3L, GameEventType.PIRATE_RAID, EventLogOutcome.FAILURE)
            )
        )

        val collection = GalacticCollectionEngine.progress(state, totalDrones = 29)

        assertEquals(3, collection.planets)
        assertEquals(2, collection.eventTypes)
        assertEquals(1, collection.achievements)
        assertEquals(2, collection.droneDiscoveries)
        assertEquals(8, collection.discoveredEntries)
    }
}
