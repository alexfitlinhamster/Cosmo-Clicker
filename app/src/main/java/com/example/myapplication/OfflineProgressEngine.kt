package com.example.myapplication

object OfflineProgressEngine {
    const val MAX_OFFLINE_SECONDS = 8 * 60 * 60L
    private const val OFFLINE_EFFICIENCY = 0.25

    data class Result(val elapsedSeconds: Long, val reward: Double)

    fun calculate(
        lastActiveAtMillis: Long,
        nowMillis: Long,
        fleetCounts: Map<String, Int>,
        fleetRarities: Map<String, Rarity>,
        rewardMultiplier: Double = 1.0
    ): Result {
        if (lastActiveAtMillis <= 0L || nowMillis <= lastActiveAtMillis) return Result(0L, 0.0)
        val elapsedSeconds = ((nowMillis - lastActiveAtMillis) / 1_000L)
            .coerceIn(0L, MAX_OFFLINE_SECONDS)
        val incomePerSecond = fleetCounts.entries.sumOf { (id, rawCount) ->
            rawCount.coerceAtLeast(0) * incomePerSecond(fleetRarities[id] ?: Rarity.COMMON)
        }
        return Result(elapsedSeconds, incomePerSecond * elapsedSeconds * OFFLINE_EFFICIENCY * rewardMultiplier.coerceAtLeast(0.0))
    }

    private fun incomePerSecond(rarity: Rarity): Double = when (rarity) {
        Rarity.COMMON -> 5.0
        Rarity.UNCOMMON -> 15.0
        Rarity.RARE -> 50.0
        Rarity.EPIC -> 200.0
        Rarity.LEGENDARY -> 1_000.0
        Rarity.VOID -> 2_500.0
    }
}
