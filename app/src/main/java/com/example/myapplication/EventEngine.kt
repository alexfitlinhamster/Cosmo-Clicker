package com.example.myapplication

object EventEngine {
    fun title(type: GameEventType): String = when (type) {
        GameEventType.STORM -> "Space Storm!"
        GameEventType.ASTEROID -> "Gold Asteroid!"
        GameEventType.METEOR_SHOWER -> "Debris Shower!"
        GameEventType.BLACK_HOLE -> "Black Hole!"
        GameEventType.SOLAR_FLARE -> "Solar Flare!"
        GameEventType.CYBER_VIRUS -> "Cyber Virus!"
    }
}
