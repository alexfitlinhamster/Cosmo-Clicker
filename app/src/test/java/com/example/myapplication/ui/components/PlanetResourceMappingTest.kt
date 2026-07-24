package com.example.myapplication.ui.components

import com.example.myapplication.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlanetResourceMappingTest {

    private val planetIds = (1..20).map { "p$it" }

    @Test
    fun everyPlanetHasLocalizedNameDescriptionAndBonusResources() {
        planetIds.forEach { id ->
            assertNotEquals("$id has no localized name", R.string.unknown_item, planetNameResource(id))
            assertNotEquals(
                "$id has no localized description",
                R.string.unknown_item,
                planetDescriptionResource(id)
            )
            assertNotEquals("$id has no localized bonus", R.string.unknown_item, planetBonusResource(id))
        }
    }

    @Test
    fun planetResourceMappingsAreUniqueForAllTwentyPlanets() {
        assertEquals(20, planetIds.map(::planetNameResource).toSet().size)
        assertEquals(20, planetIds.map(::planetDescriptionResource).toSet().size)
        assertEquals(20, planetIds.map(::planetBonusResource).toSet().size)
    }

    @Test
    fun unknownPlanetUsesFallbackResource() {
        assertEquals(R.string.unknown_item, planetNameResource("missing"))
        assertEquals(R.string.unknown_item, planetDescriptionResource("missing"))
        assertEquals(R.string.unknown_item, planetBonusResource("missing"))
    }
}
