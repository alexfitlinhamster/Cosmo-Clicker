package com.example.myapplication

import kotlin.random.Random

interface RandomProvider {
    fun nextFloat(): Float
    fun nextInt(until: Int): Int
    fun nextLong(until: Long): Long
    fun nextLong(from: Long, until: Long): Long

    fun <T> choose(values: List<T>): T = values[nextInt(values.size)]
    fun <T> chooseOrNull(values: List<T>): T? = if (values.isEmpty()) null else choose(values)
}

object KotlinRandomProvider : RandomProvider {
    override fun nextFloat(): Float = Random.nextFloat()
    override fun nextInt(until: Int): Int = Random.nextInt(until)
    override fun nextLong(until: Long): Long = Random.nextLong(until)
    override fun nextLong(from: Long, until: Long): Long = Random.nextLong(from, until)
}
