package com.example.myapplication

object EventLogCodec {
    fun encode(entries: List<EventLogEntry>): String = entries.takeLast(30).joinToString(";") {
        "${it.timestamp},${it.eventType.name},${it.outcome.name},${it.reward.toRawBits()}"
    }

    fun decode(raw: String?): List<EventLogEntry> = raw.orEmpty().split(';').mapNotNull { encoded ->
        val parts = encoded.split(',')
        if (parts.size != 4) return@mapNotNull null
        val timestamp = parts[0].toLongOrNull()?.takeIf { it >= 0L } ?: return@mapNotNull null
        val type = GameEventType.entries.firstOrNull { it.name == parts[1] } ?: return@mapNotNull null
        val outcome = EventLogOutcome.entries.firstOrNull { it.name == parts[2] } ?: return@mapNotNull null
        val reward = parts[3].toLongOrNull()?.let(Double::fromBits)
            ?.takeIf(Double::isFinite)?.coerceAtLeast(0.0) ?: return@mapNotNull null
        EventLogEntry(timestamp, type, outcome, reward)
    }.takeLast(30)
}

internal object EventDiscoveryNormalizer {
    fun restore(savedNames: Set<String>, eventLog: List<EventLogEntry>): Set<GameEventType> {
        val saved = savedNames.mapNotNullTo(mutableSetOf()) { name ->
            GameEventType.entries.firstOrNull { it.name == name }
        }
        if (saved.isNotEmpty()) return saved
        return eventLog.asSequence()
            .filter { it.outcome != EventLogOutcome.STARTED }
            .mapTo(mutableSetOf()) { it.eventType }
    }
}
