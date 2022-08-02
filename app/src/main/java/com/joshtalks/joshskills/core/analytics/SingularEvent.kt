package com.joshtalks.joshskills.core.analytics

enum class SingularEvent(override val value: String) : Event {

    APP_OPENED_FIRST_TIME("APP_OPENED_FIRST_TIME"), // App opened
    OPENED_PRE_CHECKOUT_PAGE("OPENED_PRE_CHECKOUT_PAGE"), // Opened pre-checkout page means when user go by course explore
    OPENED_FREE_TRIAL_PAYMENT("OPENED_FREE_TRIAL_PAYMENT"), // Opened buy page
    OPENED_CHECKOUT_PAGE("OPENED_CHECKOUT_PAGE"), // Opened payment screen
    INITIATED_PAYMENT("INITIATED_PAYMENT"), // Open Razorpay screen
    PAYMENT_SUCCESSFUL("PAYMENT_SUCCESSFUL"), // Payment successful
    PAYMENT_SUCCESS_EVENT("PAYMENT_SUCCESS_EVENT"), // Payment successful event
    PAYMENT_FAILED("PAYMENT_FAILED"), // Payment failed
    JI_HAA_CLICK("JI_HAA_CLICK"), // Clicked on JI HAA button
    REGISTER_FREE_TRIAL_NAME("REGISTER_FREE_TRIAL_NAME"), // Register free trial name screen
    CALL_INITIATED("CALL_INITIATED"), // Call initiated screen
    SPEAKING_COMPLETED("SPEAKING_COMPLETED"), // Speaking completed
    CALL_COMPLETED_5MIN("CALL_COMPLETED_5MIN"), // Call completed 5 min
    CALL_COMPLETED_20MIN("CALL_COMPLETED_20MIN"), // Call completed 20 min
}