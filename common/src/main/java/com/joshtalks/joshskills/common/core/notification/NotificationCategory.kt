package com.joshtalks.joshskills.common.core.notification

enum class NotificationCategory(val category: String) {
    APP_OPEN("AO"),
    AFTER_LOGIN("AL"),
    AFTER_FIRST_CALL("AFC"),
    AFTER_FIVE_MIN_CALL("AF5MC"),
    PAYMENT_INITIATED("PI"),
    AFTER_BUY_PAGE("ABP")
}