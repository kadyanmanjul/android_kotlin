package com.joshtalks.joshskills.premium.core.notification.client_side

enum class AlarmFrequency(val type: String) {
    AT("AT"),
    ONCE("ONCE"),
    HOURLY("HOURLY"),
    TWO_HOUR("TWO_HOUR"),
    THREE_HOUR("THREE_HOUR"),
    FOUR_HOUR("FOUR_HOUR"),
    SIX_HOUR("SIX_HOUR"),
    DAILY("DAILY"),
    TWO_DAY("TWO_DAY"),
    THREE_DAY("THREE_DAY"),
    DAILY_AT("DAILY_AT"),
    TWO_DAY_AT("TWO_DAY_AT"),
    THREE_DAY_AT("THREE_DAY_AT"),
    FOUR_DAY_AT("FOUR_DAY_AT"),
    WEEKLY_AT("WEEKLY_AT")
}