package com.example.myapplication.ui.components

import com.example.myapplication.EventLogOutcome
import com.example.myapplication.GameEventType
import com.example.myapplication.R

internal fun eventLogNameResource(type: GameEventType): Int = when (type) {
    GameEventType.STORM -> R.string.event_space_storm
    GameEventType.METEOR_SHOWER -> R.string.event_debris_shower
    GameEventType.BLACK_HOLE -> R.string.event_black_hole
    GameEventType.SOLAR_FLARE -> R.string.event_solar_flare
    GameEventType.CYBER_VIRUS -> R.string.event_cyber_virus
    GameEventType.DISTRESS_SIGNAL -> R.string.event_distress_signal
    GameEventType.ABANDONED_STATION -> R.string.event_abandoned_station
    GameEventType.PIRATE_RAID -> R.string.event_pirate_raid
    GameEventType.TRADING_SHIP -> R.string.event_trading_ship
}

internal fun eventLogOutcomeResource(outcome: EventLogOutcome): Int = when (outcome) {
    EventLogOutcome.STARTED -> R.string.event_log_started
    EventLogOutcome.COMPLETED -> R.string.event_log_completed
    EventLogOutcome.EXPIRED -> R.string.event_log_expired
    EventLogOutcome.CHOICE -> R.string.event_log_choice
    EventLogOutcome.SUCCESS -> R.string.event_log_success
    EventLogOutcome.FAILURE -> R.string.event_log_failure
}
