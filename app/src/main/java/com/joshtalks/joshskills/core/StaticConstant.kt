package com.joshtalks.joshskills.core

const val ARG_PHONE_NUMBER = "phone_number"
const val INDIA_COUNTRY_CODE = "+91"
const val EDITTEXT = "edittext"

const val RC_ACCOUNT_KIT = 16

enum class SignUpStepStatus(val dayNumber: Int) {
    SignUpStepFirst(1), SignUpStepSecond(2), SignUpStepThird(3)
}
