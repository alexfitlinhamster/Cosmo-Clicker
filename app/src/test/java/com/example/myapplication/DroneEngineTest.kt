package com.example.myapplication

import org.junit.Assert.assertTrue
import org.junit.Test

class DroneEngineTest {
    @Test
    fun droneAtPlanetCenterCanFlyTowardItsPatrolTarget() {
        val moved = DroneEngine.movePatrol(
            x = 0.5f,
            y = 0.5f,
            targetX = 0.85f,
            targetY = 0.75f,
            id = 1L,
            step = 0.02f,
            home = 0.5f,
            avoidRadiusSquared = 0.18f * 0.18f
        )

        assertTrue(moved.first > 0.5f)
        assertTrue(moved.second > 0.5f)
    }
}
