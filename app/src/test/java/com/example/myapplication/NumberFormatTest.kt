package com.example.myapplication

import com.example.myapplication.utils.formatNum
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatTest {
    @Test
    fun formatsBoundariesWithoutShowingOneThousandOfPreviousUnit() {
        assertEquals("999", formatNum(999.4))
        assertEquals("1K", formatNum(999.5))
        assertEquals("1M", formatNum(999_500.0))
    }

    @Test
    fun formatsLargeNegativeAndExtendedValuesSymmetrically() {
        assertEquals("-1M", formatNum(-1_000_000.0))
        assertEquals("1Qa", formatNum(1.0e15))
        assertEquals("1Qi", formatNum(1.0e18))
    }

    @Test
    fun handlesInvalidFloatingPointValuesSafely() {
        assertEquals("0", formatNum(Double.NaN))
        assertEquals("∞", formatNum(Double.POSITIVE_INFINITY))
        assertEquals("-∞", formatNum(Double.NEGATIVE_INFINITY))
    }
}
