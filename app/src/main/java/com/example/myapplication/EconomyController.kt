package com.example.myapplication

import kotlin.math.pow

object EconomyController {
    fun buyClickUpgrade(state: GameState, item: ItemConfig): GameState? {
        val currentLevel = (state.clickLevels[item.id] ?: 0).coerceAtLeast(0)
        if (currentLevel >= EconomyBalance.MAX_CLICK_UPGRADE_LEVEL) return null
        val marketMultiplier = if (state.weeklyGalaxy.active && state.weeklyGalaxy.rule == WeeklyRule.VOLATILE_MARKET) {
            FeatureEngine.volatilePriceMultiplier()
        } else 1.0
        val cost = EconomyBalance.clickUpgradeCost(item.base, currentLevel, marketMultiplier)
        if (state.totalDebris < cost) return null
        var next = state.copy(
            totalDebris = state.totalDebris - cost,
            clickLevels = state.clickLevels + (item.id to currentLevel + 1),
            activeQuests = QuestEngine.advance(state.activeQuests, QuestType.BUY_UPGRADE)
        )
        if (next.weeklyGalaxy.active && next.weeklyGalaxy.rule == WeeklyRule.VOLATILE_MARKET) {
            next = next.copy(weeklyGalaxy = next.weeklyGalaxy.copy(
                progress = (next.weeklyGalaxy.progress + 1.0).coerceAtMost(next.weeklyGalaxy.target)
            ))
        }
        return next
    }

    fun utilityUpgradeCost(id: String, level: Int): Double {
        val base = when (id) {
            "flight" -> 5_000_000.0
            "spawn" -> 2_500_000.0
            "autoclick" -> 25_000.0
            else -> 3_500_000.0
        }
        return base * (if (id == "autoclick") 3.0 else 4.0).pow(level.toDouble())
    }

    fun utilityUpgradeMaxLevel(id: String): Int = when (id) {
        "flight" -> 2
        "autoclick" -> 10
        else -> 5
    }

    fun buyUtilityUpgrade(state: GameState, id: String): GameState? {
        val key = "utility_$id"
        val level = state.clickLevels[key] ?: 0
        val max = utilityUpgradeMaxLevel(id)
        val cost = utilityUpgradeCost(id, level)
        return if (level >= max || state.totalDebris < cost) null else state.copy(
            totalDebris = state.totalDebris - cost,
            clickLevels = state.clickLevels + (key to level + 1)
        )
    }
}
