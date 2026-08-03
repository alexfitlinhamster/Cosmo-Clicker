package com.example.myapplication.ui.components

import com.example.myapplication.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DebrisResourceMappingTest {
    @Test
    fun allFourteenDebrisIndicesHaveUniqueResources() {
        val resources = (1..14).map(::debrisDrawable)

        assertEquals(14, resources.toSet().size)
        resources.forEach { assertNotEquals(0, it) }
    }

    @Test
    fun invalidIndexUsesFirstDebrisAsSafeFallback() {
        assertEquals(R.drawable.debris_01, debrisDrawable(0))
        assertEquals(R.drawable.debris_01, debrisDrawable(15))
    }
}
