package com.joshtalks.joshskills.core.analytics

enum class BranchEventName(override val value: String) : Event {

    PAYMENT_SUCCESSFUL("PAYMENT_SUCCESSFUL"), // Payment successful //PURCHASE
    PAYMENT_FAILED("PAYMENT_FAILED"), // Payment failed //SEARCH
    REGISTER_FREE_TRIAL_NAME("REGISTER_FREE_TRIAL_NAME"), // Register free trial name screen //COMPLETE_REGISTRATION
    CALL_COMPLETED_20MIN("CALL_COMPLETED_20MIN"), // Call completed 20 min //VIEW_ITEM
    CALL_COMPLETED_5MIN("CALL_COMPLETED_5MIN"), // Call completed 5 min //EVENT_NAME_CONTACT
    OPEN_INBOX("OPEN_INBOX"), // Open inbox screen //EVENT_NAME_SCHEDULE
    //INITIATE_PURCHASE -> Initialize payment
    //ACHIEVE_LEVEL -> Video watched to level
    //COMPLETE_TUTORIAL -> Lesson completed
    CALL_INITIATED("CALL_INITIATED"), // Call initiated screen //EVENT_PARAM_SEARCH_STRING
    APP_OPENED_FIRST_TIME("APP_OPENED_FIRST_TIME"), // App opened //AppEventsConstants.EVENT_NAME_ACTIVATED_APP
    SPEAKING_COMPLETED("SPEAKING_COMPLETED"), // Speaking completed //EVENT_NAME_CUSTOMIZE_PRODUCT
    OPENED_PRE_CHECKOUT_PAGE("OPENED_PRE_CHECKOUT_PAGE"), // Opened pre-checkout page means when user go by course explore //EVENT_NAME_DONATE
    VIEW_CART("VIEW_CART") //OPEN COURSE EXPLOER
}

// EVENT_NAME_ACHIEVED_LEVEL = > Viewed course