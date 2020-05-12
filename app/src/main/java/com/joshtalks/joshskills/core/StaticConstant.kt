package com.joshtalks.joshskills.core

const val ARG_PHONE_NUMBER = "phone_number"

enum class SignUpStepStatus {
    SignUpStepFirst, SignUpStepSecond, SignUpCompleted, SignUpResendOTP, SignUpWithoutRegister, CoursesNotExist
}

enum class ApiCallStatus {
    SUCCESS, FAILED
}

const val MESSAGE_CHAT_SIZE_LIMIT = 2048
const val EMPTY = ""

const val IMAGE_PATTERN = "([^\\s]+(\\.(?i)(jpg|png|gif|bmp)|WEBP|webp|JPEG|PNG|Jpeg)$)"
//const val VIDEO_PATTERN = "([^\\s]+(\\.(?i)(mp4|MP4)$)"

val IMAGE_REGEX = Regex(pattern = IMAGE_PATTERN)

const val MINIMUM_VIDEO_DOWNLOAD_PROGRESS = 20
const val COURSE_OFFER = "50%"
const val ARG_PLACEHOLDER_URL = "placeholder_image_url"

enum class REFERRAL_EVENT(val type: String) {
    CLICK_ON_REFERRAL("click_on_referral"), LONG_PRESS_CODE("long_press_code"), CLICK_ON_SHARE("click_on_share")
}


enum class RegistrationMethods(val type: String) {
    MOBILE_NUMBER("Mobile Number"), TRUE_CALLER("True Caller")
}



