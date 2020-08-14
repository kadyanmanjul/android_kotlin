package com.joshtalks.joshskills.core

const val ARG_PHONE_NUMBER = "phone_number"

enum class SignUpStepStatus {
    SignUpStepFirst, SignUpStepSecond, SignUpCompleted,
    RequestForOTP, ReGeneratedOTP, ProfileCompleted,
    ProfileInCompleted, SignUpResendOTP, SignUpWithoutRegister,
    WRONG_OTP, ERROR
}

enum class ApiCallStatus {
    START, SUCCESS, FAILED, RETRY
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
const val ALPHA_MAX = 1F
const val ALPHA_MIN = 0.45F


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


enum class PractiseUser(val type: Int) {
    FIRST(0), SECOND(1)
}

enum class ViewTypeForPractiseUser(val type: Int) {
    FIRST(0), SECOND(1)
}

class FirebaseRemoteConfigKey {
    companion object {

        // Inbox Screen (SubscriptionTrail Tip)
        const val SUBSCRIPTION_TRIAL_TIP_DAY0 = "SUBSCRIPTION_TRIAL_TIP_DAY0"
        const val SUBSCRIPTION_TRIAL_TIP_DAY1 = "SUBSCRIPTION_TRIAL_TIP_DAY1"
        const val SUBSCRIPTION_TRIAL_TIP_DAY2 = "SUBSCRIPTION_TRIAL_TIP_DAY2"
        const val SUBSCRIPTION_TRIAL_TIP_DAY3 = "SUBSCRIPTION_TRIAL_TIP_DAY3"
        const val SUBSCRIPTION_TRIAL_TIP_DAY4 = "SUBSCRIPTION_TRIAL_TIP_DAY4"
        const val SUBSCRIPTION_TRIAL_TIP_DAY5 = "SUBSCRIPTION_TRIAL_TIP_DAY5"
        const val SUBSCRIPTION_TRIAL_TIP_DAY6 = "SUBSCRIPTION_TRIAL_TIP_DAY6"
        const val SUBSCRIPTION_TRIAL_TIP_DAY7 = "SUBSCRIPTION_TRIAL_TIP_DAY7"
        const val EXPLORE_TYPE_FFCOURSE_TIP = "EXPLORE_TYPE_FFCOURSE_TIP"
        const val EXPLORE_TYPE_NORMAL_TIP = "EXPLORE_TYPE_NORMAL_TIP"
        const val INBOX_SCREEN_CTA_TEXT_NORMAL = "INBOX_SCREEN_CTA_TEXT_NORMAL"
        const val INBOX_SCREEN_CTA_TEXT_FFCOURSE = "INBOX_SCREEN_CTA_TEXT_FFCOURSE"
        const val INBOX_SCREEN_CTA_TEXT_FREETRIAL = "INBOX_SCREEN_CTA_TEXT_FREETRIAL"
        const val INBOX_SCREEN_CTA_TEXT_SUBSCRIPTION = "INBOX_SCREEN_CTA_TEXT_SUBSCRIPTION"

        // Trail End Screen
        const val TRAIL_END_SCREEN_MESSAGE = "TRAIL_END_SCREEN_MESSAGE"
        const val TRAIL_END_SCREEN_CTA_LABEL = "TRAIL_END_SCREEN_CTA_LABEL"

        // Start Subscription Screen (TRAIL)
        const val START_TRIAL_CTA_LABEL = "start_7_day_trial"
        const val START_TRAIL_TITLE = "START_TRAIL_TITLE"
        const val START_TRAIL_HEADING = "START_TRAIL_HEADING"
        const val START_TRAIL_FEATURE1 = "START_TRAIL_FEATURE1"
        const val START_TRAIL_FEATURE2 = "START_TRAIL_FEATURE2"
        const val START_TRAIL_FEATURE3 = "START_TRAIL_FEATURE3"

        // Start Subscription Screen (SUBSCRIPTION)
        const val START_SUBSCRIPTION_CTA_LABEL = "start_subscription"
        const val START_SUBSCRIPTION_TITLE = "START_SUBSCRIPTION_TITLE"
        const val START_SUBSCRIPTION_HEADING = "START_SUBSCRIPTION_HEADING"
        const val START_SUBSCRIPTION_FEATURE1 = "START_SUBSCRIPTION_FEATURE1"
        const val START_SUBSCRIPTION_FEATURE2 = "START_SUBSCRIPTION_FEATURE2"
        const val START_SUBSCRIPTION_FEATURE3 = "START_SUBSCRIPTION_FEATURE3"

        // Course Explore Screen
        const val FFCOURSE_CARD_CLICK_MSG = "FFCOURSE_CARD_CLICK_MSG"

    }
}
