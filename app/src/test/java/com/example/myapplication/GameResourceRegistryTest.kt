package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GameResourceRegistryTest {
    @Test
    fun everyDroneHasAnExplicitUniqueResource() {
        val resources = (1..29).map(GameResourceRegistry::drone)
        resources.forEach { assertNotEquals(R.drawable.upgrade_magnet, it) }
        assertEquals(29, resources.toSet().size)
    }

    @Test
    fun invalidDroneAndCaseNumbersUseSafeFallbacks() {
        assertEquals(R.drawable.upgrade_magnet, GameResourceRegistry.drone(0))
        assertEquals(R.drawable.upgrade_magnet, GameResourceRegistry.drone(30))
        assertEquals(R.drawable.case_08, GameResourceRegistry.caseFrame(0))
        assertEquals(R.drawable.case_08, GameResourceRegistry.caseFrame(9))
    }
}
