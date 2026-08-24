package com.example.myapplication.utils

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

fun formatNum(n: Double): String {
    if (n.isNaN()) return "0"
    if (n == Double.POSITIVE_INFINITY) return "∞"
    if (n == Double.NEGATIVE_INFINITY) return "-∞"

    val magnitude = abs(n)
    if (magnitude < 999.5) return n.roundToLong().toString()

    val suffixes = listOf("K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc")
    var value = magnitude
    var suffixIndex = -1
    while (value >= 999.5 && suffixIndex < suffixes.lastIndex) {
        value /= 1000.0
        suffixIndex++
    }

    if (value >= 999.5) {
        return String.format(Locale.US, "%.2e", n)
            .replace("e+", "e")
    }

    val signedValue = if (n < 0) -value else value
    val decimals = when {
        value >= 100.0 -> 0
        value >= 10.0 -> 1
        else -> 2
    }
    val formatted = String.format(Locale.US, "%.${decimals}f", signedValue)
    val compact = if ('.' in formatted) formatted.trimEnd('0').trimEnd('.') else formatted
    return compact + suffixes[suffixIndex]
}
