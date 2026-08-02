package com.example.myapplication

import kotlin.math.sqrt

object DroneEngine {
    fun distanceSquared(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return dx * dx + dy * dy
    }

    fun movePatrol(
        x: Float,
        y: Float,
        targetX: Float,
        targetY: Float,
        id: Long,
        step: Float,
        home: Float,
        avoidRadiusSquared: Float
    ): Pair<Float, Float> {
        val dx = targetX - x
        val dy = targetY - y
        val distance = sqrt(dx * dx + dy * dy)
        if (distance <= step) return targetX to targetY
        var stepX = dx / distance * step
        var stepY = dy / distance * step
        if (distanceSquared(x + stepX, y + stepY, home, home) < avoidRadiusSquared) {
            val radialX = x - home
            val radialY = y - home
            val clockwise = if (id and 1L == 0L) 1f else -1f
            val radialLength = sqrt(radialX * radialX + radialY * radialY).coerceAtLeast(0.001f)
            stepX = -radialY / radialLength * step * clockwise
            stepY = radialX / radialLength * step * clockwise
        }
        return (x + stepX) to (y + stepY)
    }
}
