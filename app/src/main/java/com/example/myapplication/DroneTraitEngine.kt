package com.example.myapplication

data class DroneTraitModifiers(
    val clickMultiplier: Double = 1.0,
    val passiveMultiplier: Double = 1.0,
    val speedMultiplier: Float = 1.0f
)

object DroneTraitEngine {
    const val MAX_ACTIVE_DRONES = 3

    fun modifiers(activeFleetCounts: Map<String, Int>): DroneTraitModifiers {
        val active = activeFleetCounts.filterValues { it > 0 }.keys
        var click = 1.0
        var passive = 1.0
        var speed = 1.0f

        if ("drone_5" in active) passive *= 1.20
        if ("drone_9" in active) click *= 1.15
        if ("drone_13" in active) {
            passive *= 1.35
            click *= 0.90
        }
        if ("drone_17" in active) click *= 1.25
        if ("drone_21" in active) {
            speed *= 1.25f
            passive *= 0.90
        }
        return DroneTraitModifiers(click, passive, speed)
    }
}
