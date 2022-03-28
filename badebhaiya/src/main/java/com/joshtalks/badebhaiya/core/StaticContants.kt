package com.joshtalks.badebhaiya.core

enum class SignUpStepStatus {
    SignUpStepFirst, SignUpStepSecond, SignUpCompleted,
    RequestForOTP, ReGeneratedOTP, ProfileCompleted,ProfilePicUploaded,StartAfterPicUploaded,ProfilePicSkipped,
    ProfileInCompleted, SignUpResendOTP, SignUpWithoutRegister,
    WRONG_OTP, ERROR, NameMissing, ProfilePicMissing, NameEntered
}

enum class ApiCallStatus {
    START, SUCCESS, FAILED, RETRY, FAILED_PERMANENT, INVALIDED
}

const val EMPTY = ""
