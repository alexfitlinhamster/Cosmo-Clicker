package com.example.myapplication

internal object DebrisEngine {
    fun imageIndex(rarity: Rarity, random: RandomProvider): Int = when (rarity) {
        Rarity.COMMON -> random.choose(listOf(1, 2, 7, 8))
        Rarity.UNCOMMON -> random.choose(listOf(3, 9, 10))
        Rarity.RARE -> random.choose(listOf(4, 11, 12))
        Rarity.EPIC -> random.choose(listOf(5, 13))
        Rarity.LEGENDARY, Rarity.VOID -> if (rarity == Rarity.VOID) 6 else random.choose(listOf(6, 14))
    }

    fun reward(rarity: Rarity, planetId: String, random: RandomProvider): Double {
        var reward = random.nextLong(rarity.minReward, rarity.maxReward + 1).toDouble()
        if ((planetId == "p4" || planetId == "p15") && rarity != Rarity.LEGENDARY && random.nextInt(100) < 50) {
            reward *= 2.0
        }
        if (planetId == "p17") reward *= 1.5
        return EconomyBalance.scaledReward(reward, planetId)
    }

    fun rarity(
        planetId: String,
        technologies: Set<Technology>,
        activeEffects: Map<String, Long>,
        nowMillis: Long,
        random: RandomProvider
    ): Rarity {
        val weights = Rarity.entries.map { rarity ->
            val planetMultiplier = if (
                (planetId == "p10" || planetId == "p15") &&
                (rarity == Rarity.EPIC || rarity == Rarity.LEGENDARY)
            ) 2 else 1
            val technologyMultiplier = if (
                Technology.LUCK_MATRIX in technologies &&
                (rarity == Rarity.EPIC || rarity == Rarity.LEGENDARY)
            ) 3 else 2
            val tradeMultiplier = effectMultiplier(
                activeEffects[SkillType.TRADE_LUCK.id],
                nowMillis,
                rarity
            )
            val rushMultiplier = effectMultiplier(
                activeEffects[SkillType.SALVAGE_RUSH.id],
                nowMillis,
                rarity
            )
            rarity.spawnWeight * planetMultiplier * tradeMultiplier * rushMultiplier * technologyMultiplier / 2
        }

        val roll = random.nextInt(weights.sum())
        var cumulative = 0
        Rarity.entries.forEachIndexed { index, rarity ->
            cumulative += weights[index]
            if (roll < cumulative) return rarity
        }
        return Rarity.COMMON
    }

    private fun effectMultiplier(expiresAt: Long?, nowMillis: Long, rarity: Rarity): Int =
        if (
            (expiresAt ?: 0L) > nowMillis &&
            (rarity == Rarity.RARE || rarity == Rarity.EPIC || rarity == Rarity.LEGENDARY)
        ) 2 else 1
}
