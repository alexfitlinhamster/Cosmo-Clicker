package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetaProgressEngineTest {
    @Test
    fun collectionSetRequiresEveryDroneInTheSet() {
        val incomplete = (1..4).mapTo(mutableSetOf()) { "drone_$it" }
        assertTrue(MetaProgressEngine.completedCollectionSets(incomplete).isEmpty())
        assertEquals(1.0, MetaProgressEngine.collectionSetMultiplier(incomplete), 0.0001)

        val complete = incomplete + "drone_5"
        assertEquals(listOf("first_expedition"), MetaProgressEngine.completedCollectionSets(complete).map { it.id })
        assertEquals(1.03, MetaProgressEngine.collectionSetMultiplier(complete), 0.0001)
    }

    @Test
    fun bonusesFromCompletedSetsStack() {
        val firstTwoSets = (1..10).mapTo(mutableSetOf()) { "drone_$it" }
        assertEquals(1.06, MetaProgressEngine.collectionSetMultiplier(firstTwoSets), 0.0001)
    }

    @Test
    fun allSetsCoverEveryCollectibleDroneOnce() {
        val ids = MetaProgressEngine.collectionSets.flatMap { it.droneIds }
        assertEquals(29, ids.size)
        assertEquals(29, ids.toSet().size)
        assertEquals((1..29).map { "drone_$it" }.toSet(), ids.toSet())
    }

    @Test
    fun firstPrestigeImmediatelyAllowsAPermanentPurchase() {
        val before = GameState(
            totalDebris = 123_000.0,
            ownedPlanets = setOf("p1", "p10"),
            fleetCounts = mapOf("drone_1" to 2)
        )

        val prestiged = MetaProgressEngine.prestige(before)!!
        assertEquals(1, prestiged.prestigePoints)
        assertEquals(1, prestiged.lifetimeStats.prestiges)
        assertEquals(setOf("p1"), prestiged.ownedPlanets)

        val upgraded = MetaProgressEngine.buyTechnology(prestiged, Technology.POWER_CORE)!!
        assertEquals(0, upgraded.prestigePoints)
        assertTrue(Technology.POWER_CORE in upgraded.technologies)
        assertEquals(1.25, MetaProgressEngine.technologyMultiplier(upgraded.technologies), 0.0001)
        assertNull(MetaProgressEngine.buyTechnology(upgraded, Technology.POWER_CORE))
    }

    @Test
    fun prestigePreservesPreviouslyPurchasedTechnologies() {
        val before = GameState(
            ownedPlanets = setOf("p1", "p12"),
            prestigePoints = 2,
            technologies = setOf(Technology.POWER_CORE)
        )

        val after = MetaProgressEngine.prestige(before)!!
        assertEquals(5, after.prestigePoints)
        assertEquals(setOf(Technology.POWER_CORE), after.technologies)
    }
}
