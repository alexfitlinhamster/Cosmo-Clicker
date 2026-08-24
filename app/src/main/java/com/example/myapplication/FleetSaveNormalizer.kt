package com.example.myapplication

/** Repairs fleet counts loaded from older or malformed saves. */
internal object FleetSaveNormalizer {
    fun owned(
        counts: Map<String, Int>,
        capacity: Int = EconomyBalance.MAX_DRONES
    ): Map<String, Int> = limitCounts(counts, capacity, maxPerEntry = Int.MAX_VALUE)

    fun active(
        counts: Map<String, Int>,
        capacity: Int = DroneTraitEngine.MAX_ACTIVE_DRONES
    ): Map<String, Int> = limitCounts(counts, capacity, maxPerEntry = 1)

    private fun limitCounts(
        counts: Map<String, Int>,
        capacity: Int,
        maxPerEntry: Int
    ): Map<String, Int> {
        var slotsLeft = capacity.coerceAtLeast(0)
        return counts.mapValues { (_, rawCount) ->
            val accepted = rawCount.coerceIn(0, maxPerEntry).coerceAtMost(slotsLeft)
            slotsLeft -= accepted
            accepted
        }
    }
}
