package com.example.myapplication

import java.util.Calendar
import kotlin.math.pow

object FeatureEngine {
    enum class WeeklyAction { CLICK, PASSIVE_INCOME, PURCHASE }
    fun weekKey(now: Long = System.currentTimeMillis()): Long = Calendar.getInstance().run {
        timeInMillis = now
        getWeekYear() * 100L + get(Calendar.WEEK_OF_YEAR)
    }

    fun weeklyFor(now: Long = System.currentTimeMillis()): WeeklyGalaxy {
        val key = weekKey(now)
        val rule = WeeklyRule.entries[(key % WeeklyRule.entries.size).toInt()]
        return WeeklyGalaxy(weekKey = key, rule = rule, target = when (rule) {
            WeeklyRule.CLICKS_ONLY -> 500.0
            WeeklyRule.FRAGILE_DRONES -> 75_000.0
            WeeklyRule.VOLATILE_MARKET -> 30.0
        })
    }

    fun refreshWeekly(state: GameState, now: Long = System.currentTimeMillis()): GameState =
        if (state.weeklyGalaxy.weekKey == weekKey(now)) state
        else state.copy(weeklyGalaxy = weeklyFor(now))

    fun stationCost(module: StationModule, level: Int): Double =
        when (module) {
            StationModule.HANGAR -> 25_000.0
            StationModule.LABORATORY -> 40_000.0
            StationModule.REACTOR -> 60_000.0
            StationModule.TRADE_HUB -> 90_000.0
        } * 3.0.pow(level.coerceAtLeast(0).toDouble())

    fun stationClickMultiplier(state: GameState) = 1.0 + 0.12 * (state.stationLevels[StationModule.LABORATORY] ?: 0)
    fun stationDpsMultiplier(state: GameState) = 1.0 + 0.15 * (state.stationLevels[StationModule.REACTOR] ?: 0)
    fun stationRewardMultiplier(state: GameState) = 1.0 + 0.10 * (state.stationLevels[StationModule.TRADE_HUB] ?: 0)

    fun volatilePriceMultiplier(now: Long = System.currentTimeMillis()): Double =
        if ((now / 60_000L) % 2L == 0L) 0.65 else 1.35

    fun advanceWeekly(state: GameState, action: WeeklyAction, amount: Double = 1.0): GameState {
        val galaxy = state.weeklyGalaxy
        if (!galaxy.active || galaxy.rewardClaimed) return state
        val matches = when (galaxy.rule) {
            WeeklyRule.CLICKS_ONLY -> action == WeeklyAction.CLICK
            WeeklyRule.FRAGILE_DRONES -> action == WeeklyAction.PASSIVE_INCOME
            WeeklyRule.VOLATILE_MARKET -> action == WeeklyAction.PURCHASE
        }
        if (!matches || amount <= 0.0 || !amount.isFinite()) return state
        return state.copy(weeklyGalaxy = galaxy.copy(
            progress = (galaxy.progress + amount).coerceAtMost(galaxy.target)
        ))
    }

    fun claimWeeklyReward(state: GameState): GameState {
        val galaxy = state.weeklyGalaxy
        if (galaxy.rewardClaimed || galaxy.progress < galaxy.target) return state
        return state.copy(
            weeklyGalaxy = galaxy.copy(rewardClaimed = true, active = false),
            prestigePoints = state.prestigePoints + 2,
            totalDebris = state.totalDebris + 250_000.0 * stationRewardMultiplier(state)
        )
    }

    fun upgradeStation(state: GameState, module: StationModule): GameState? {
        val level = state.stationLevels[module] ?: 0
        val cost = stationCost(module, level)
        if (level >= 5 || state.totalDebris < cost) return null
        return state.copy(
            totalDebris = state.totalDebris - cost,
            stationLevels = state.stationLevels + (module to level + 1)
        )
    }
}
