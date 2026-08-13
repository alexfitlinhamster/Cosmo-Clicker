package com.example.myapplication

object FleetController {
    fun activeCapacity(state: GameState): Int =
        DroneTraitEngine.MAX_ACTIVE_DRONES + (state.clickLevels["utility_flight"] ?: 0).coerceIn(0, 2)

    fun sell(state: GameState, item: FleetConfig): GameState? {
        val count = state.fleetCounts[item.id] ?: 0
        if (count <= 0) return null
        return state.copy(
            totalDebris = state.totalDebris + GameRules.droneSaleValue(item.base, count),
            fleetCounts = state.fleetCounts + (item.id to count - 1),
            activeFleetCounts = state.activeFleetCounts +
                (item.id to (state.activeFleetCounts[item.id] ?: 0).coerceAtMost(count - 1))
        )
    }

    fun deploy(state: GameState, id: String): GameState? {
        val owned = state.fleetCounts[id] ?: 0
        val active = state.activeFleetCounts[id] ?: 0
        return if (active > 0 || owned <= 0 || state.activeFleetCounts.values.sum() >= activeCapacity(state)) null
        else state.copy(activeFleetCounts = state.activeFleetCounts + (id to 1))
    }

    fun recall(state: GameState, id: String): GameState? {
        val active = state.activeFleetCounts[id] ?: 0
        return if (active <= 0) null
        else state.copy(activeFleetCounts = state.activeFleetCounts + (id to active - 1))
    }
}
