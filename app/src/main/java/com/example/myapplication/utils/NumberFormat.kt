package com.example.myapplication.utils

import java.util.Locale
import kotlin.math.roundToLong

fun formatNum(n: Double): String {
    if (n < 1000) return n.roundToLong().toString()
    val suffixes = listOf("K", "M", "B", "T")
    var value = n
    var i = -1
    while (value >= 1000 && i < suffixes.size - 1) {
        value /= 1000.0
        i++
    }
    return String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.') + suffixes[i]
}
