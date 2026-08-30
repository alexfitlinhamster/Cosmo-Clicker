package com.example.myapplication

/** Keeps route progress usable when loading saves from longer legacy routes. */
internal object ProgressionSaveNormalizer {
    fun ownedPlanets(rawIds: Collection<String>): Set<String> = rawIds.mapNotNullTo(mutableSetOf()) {
        normalizedPlanetId(it)
    }.plus("p1")

    fun currentPlanet(rawId: String?, ownedPlanets: Set<String>): String {
        val normalized = rawId?.let(::normalizedPlanetId)
        return normalized?.takeIf { it in ownedPlanets } ?: "p1"
    }

    private fun normalizedPlanetId(rawId: String): String? {
        val index = rawId.removePrefix("p").toIntOrNull()?.takeIf { it >= 1 } ?: return null
        return "p${index.coerceAtMost(EconomyBalance.MAX_PLANET_INDEX)}"
    }
}
