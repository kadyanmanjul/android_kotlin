package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.core.EMPTY

data class LoginViaEventBus(
    var loginViaStatus: LoginViaStatus,
    var countryCode: String = EMPTY,
    var mNumber: String = EMPTY
)

enum class LoginViaStatus {
    GMAIL, FACEBOOK, TRUECALLER, SMS_VERIFY, NUMBER_VERIFY
}

enum class CreatedSource {
    FB, GML, OTP, TC
}