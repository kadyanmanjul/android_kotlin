package com.joshtalks.joshskills.core

const val ARG_PHONE_NUMBER = "phone_number"

enum class SignUpStepStatus {
    SignUpStepFirst, SignUpStepSecond, SignUpCompleted,
    RequestForOTP, ReGeneratedOTP, ProfileCompleted,
    ProfileInCompleted, SignUpResendOTP, SignUpWithoutRegister,
    WRONG_OTP, ERROR
}

enum class ApiCallStatus {
    SUCCESS, FAILED, RETRY
}

const val TIMEOUT_TIME = 60_000L
const val MESSAGE_CHAT_SIZE_LIMIT = 2048
const val EMPTY = ""
const val SINGLE_SPACE = " "

const val IMAGE_PATTERN = "([^\\s]+(\\.(?i)(jpg|png|gif|bmp)|WEBP|webp|JPEG|PNG|Jpeg)$)"
//const val VIDEO_PATTERN = "([^\\s]+(\\.(?i)(mp4|MP4)$)"

const val STARTED_FROM = "started_from"
const val COURSE_ID = "course_ID"
const val MIN_LINES = 4
const val RC_HINT = 2
const val MAX_YEAR = 6

val IMAGE_REGEX = Regex(pattern = IMAGE_PATTERN)

const val MINIMUM_VIDEO_DOWNLOAD_PROGRESS = 20
const val ARG_PLACEHOLDER_URL = "placeholder_image_url"

enum class REFERRAL_EVENT(val type: String) {
    CLICK_ON_REFERRAL("click_on_referral"), LONG_PRESS_CODE("long_press_code"), CLICK_ON_SHARE("click_on_share")
}


enum class RegistrationMethods(val type: String) {
    MOBILE_NUMBER("Mobile Number"), TRUE_CALLER("True Caller"), GOOGLE("Google"), FACEBOOK("Facebook")
}

enum class GENDER(val gValue: String) {
    MALE("M"), FEMALE("F"), OTHER("O")
}

enum class VerificationService {
    TRUECALLER, SINCH, SMS_COUNTRY
}

enum class VerificationVia {
    FLASH_CALL, SMS
}

enum class VerificationStatus {
    INITIATED, SUCCESS, FAILED, USER_DENY, TIMEOUT

}


