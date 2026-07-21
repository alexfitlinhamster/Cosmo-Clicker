package com.example.myapplication

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RarityTest {
    @Test
    fun rareDroneCollectsCommonAndRareDebris() {
        assertTrue(Rarity.RARE.canCollect(Rarity.COMMON))
        assertTrue(Rarity.RARE.canCollect(Rarity.RARE))
    }

    @Test
    fun rareDroneCannotCollectHigherRarityDebris() {
        assertFalse(Rarity.RARE.canCollect(Rarity.EPIC))
        assertFalse(Rarity.RARE.canCollect(Rarity.LEGENDARY))
    }

    @Test
    fun rewardsIncreaseWithRarity() {
        Rarity.entries.forEach { rarity ->
            assertTrue(rarity.minReward <= rarity.maxReward)
        }
        Rarity.entries.zipWithNext().forEach { (lower, higher) ->
            assertTrue(higher.maxReward > lower.maxReward)
        }
    }
}
