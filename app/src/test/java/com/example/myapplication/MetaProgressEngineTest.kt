package com.example.myapplication

import org.junit.Assert.assertEquals
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
}
