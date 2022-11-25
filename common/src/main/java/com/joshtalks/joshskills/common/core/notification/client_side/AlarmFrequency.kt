package com.joshtalks.joshskills.common.core.notification.client_side

enum class AlarmFrequency(val type: String) {
    AT("AT"),
    ONCE("ONCE"),
    HOURLY("HOURLY"),
    TWO_HOUR("TWO_HOUR"),
    THREE_HOUR("THREE_HOUR"),
    FOUR_HOUR("FOUR_HOUR"),
    SIX_HOUR("SIX_HOUR"),
    DAILY("DAILY"),
    DAILY_AT("DAILY_AT"),
    TWO_DAY_AT("TWO_DAY_AT"),
    THREE_DAY_AT("THREE_DAY_AT"),
    FOUR_DAY_AT("FOUR_DAY_AT"),
    WEEKLY_AT("WEEKLY_AT")
}