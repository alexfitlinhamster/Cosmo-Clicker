package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerTest {
    @Test
    fun fleetControllerKeepsOwnershipAndCapacityConsistent() {
        val item = FleetConfig("drone_1", "", 250.0, 0)
        val state = GameState(
            fleetCounts = mapOf(item.id to 2),
            activeFleetCounts = mapOf(item.id to 1)
        )
        val recalled = FleetController.recall(state, item.id)!!
        assertEquals(0, recalled.activeFleetCounts[item.id])
        val deployed = FleetController.deploy(recalled, item.id)!!
        assertEquals(1, deployed.activeFleetCounts[item.id])
        val sold = FleetController.sell(deployed, item)!!
        assertEquals(1, sold.fleetCounts[item.id])
        assertEquals(1, sold.activeFleetCounts[item.id])
    }

    @Test
    fun caseControllerStartsOnlyAffordableOpening() {
        assertNull(CaseController.startOpening(GameState(totalDebris = 0.0), CaseType.COMMON, 1))
        val started = CaseController.startOpening(GameState(totalDebris = 100_000.0), CaseType.COMMON, 2)!!
        assertTrue(started.isOpeningCase)
        assertEquals(1, started.pendingCaseOpenings)
        assertFalse(started.showCaseBundleSummary)
    }

    @Test
    fun economyControllerPurchasesUpgrade() {
        val item = ItemConfig("magnet", "", 15.0, 1.0, 0)
        val state = GameState(totalDebris = 200.0)
        val purchased = EconomyController.buyClickUpgrade(state, item)!!
        assertEquals(1, purchased.clickLevels[item.id])
        assertEquals(50.0, purchased.totalDebris, 0.0)
    }
}
