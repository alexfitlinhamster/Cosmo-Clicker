package com.example.myapplication

object EconomyEngine {
    fun processTick(state: GameState, nowMillis: Long, passiveIncome: Double = 0.0): GameState {
        val currentState = EventEngine.resolvePendingChainIfNeeded(
            EventEngine.expireEventIfNeeded(state, nowMillis),
            nowMillis
        )
        val income = passiveIncome.coerceAtLeast(0.0)
        val total = when (currentState.activeEvent?.type) {
            GameEventType.BLACK_HOLE -> currentState.totalDebris * 0.995 + income
            GameEventType.PIRATE_RAID ->
                (currentState.totalDebris - EventEngine.pirateRaidTheft(currentState.totalDebris))
                    .coerceAtLeast(0.0) + income
            else -> currentState.totalDebris + income
        }
        return currentState.copy(
            totalDebris = total,
            activeEffects = currentState.activeEffects.filterValues { it > nowMillis }
        )
    }

    fun calculateClickValue(
        state: GameState,
        clickItems: List<ItemConfig>,
        random: RandomProvider
    ): Double {
        var value = 1.0 + clickItems.sumOf { (state.clickLevels[it.id] ?: 0) * it.value }
        when (state.currentPlanetId) {
            "p1" -> if (random.nextInt(100) < 15) value *= 2.0
            "p2" -> value *= 1.2
            "p5" -> value += 15.0
            "p6" -> if (random.nextInt(100) < 30) value *= 4.0
            "p14" -> if (state.activeEvent != null) value *= 2.0
            "p20" -> if (random.nextInt(100) < 10) value *= 10.0
            "p22" -> value *= 1.4
            "p23" -> if (random.nextInt(100) < 20) value *= 5.0
            "p24" -> value *= 2.0
            "p15" -> {
                if (random.nextInt(100) < 25) value *= 3.0
                value += 20.0
                if (state.activeEvent != null) value *= 1.5
            }
        }
        return value * state.eventMultiplier
    }
}
