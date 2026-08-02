package com.example.myapplication

enum class NegativeEventResistanceOutcome { REDIRECT_TO_POSITIVE, CANCEL }

data class PlanetEventModifiers(
    val frequencyMultiplier: Double = 1.0,
    val durationMultiplier: Double = 1.0,
    val negativeEventResistance: Double = 0.0,
    val blockedEvents: Set<GameEventType> = emptySet(),
    val resistanceOutcome: NegativeEventResistanceOutcome =
        NegativeEventResistanceOutcome.REDIRECT_TO_POSITIVE,
    val blackHoleRareTargetRewardCount: Int = 0
) {
    init {
        require(frequencyMultiplier > 0.0)
        require(durationMultiplier > 0.0)
        require(negativeEventResistance in 0.0..1.0)
        require(blackHoleRareTargetRewardCount >= 0)
    }

    companion object {
        private val defaults = PlanetEventModifiers()
        private val byPlanetId = mapOf(
            "p3" to PlanetEventModifiers(frequencyMultiplier = 1.3),
            "p7" to PlanetEventModifiers(blockedEvents = setOf(GameEventType.CYBER_VIRUS)),
            "p8" to PlanetEventModifiers(durationMultiplier = 2.0),
            "p12" to PlanetEventModifiers(negativeEventResistance = 0.25),
            "p13" to PlanetEventModifiers(blackHoleRareTargetRewardCount = 5),
            "p15" to PlanetEventModifiers(
                frequencyMultiplier = 1.3,
                durationMultiplier = 2.0,
                negativeEventResistance = 0.25,
                blockedEvents = setOf(GameEventType.CYBER_VIRUS),
                blackHoleRareTargetRewardCount = 5
            ),
            "p16" to PlanetEventModifiers(
                negativeEventResistance = 0.40,
                resistanceOutcome = NegativeEventResistanceOutcome.CANCEL
            )
        )

        fun forPlanet(planetId: String): PlanetEventModifiers = byPlanetId[planetId] ?: defaults
    }
}
