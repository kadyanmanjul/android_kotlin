package com.joshtalks.badebhaiya.core

enum class SignUpStepStatus {
    SignUpStepFirst, SignUpStepSecond, SignUpCompleted,
    RequestForOTP, ReGeneratedOTP, ProfileCompleted,ProfilePicUploaded,StartAfterPicUploaded,ProfilePicSkipped,
    ProfileInCompleted, SignUpResendOTP, SignUpWithoutRegister,
    WRONG_OTP, ERROR, NameMissing, ProfilePicMissing
}

const val EMPTY = ""
