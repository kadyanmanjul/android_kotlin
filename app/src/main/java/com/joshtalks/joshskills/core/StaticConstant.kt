package com.joshtalks.joshskills.core

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
const val SHOW_OVERLAY = "show_overlay"
const val MIN_LINES = 4
const val RC_HINT = 2
const val MAX_YEAR = 6
const val ALPHA_MAX = 1F
const val ALPHA_MIN = 0.45F


val IMAGE_REGEX = Regex(pattern = IMAGE_PATTERN)
const val MINIMUM_VIDEO_DOWNLOAD_PROGRESS = 20
const val ARG_PLACEHOLDER_URL = "placeholder_image_url"

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

        // Inbox Screen (SubscriptionTrial Tip)
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
        const val INBOX_SCREEN_CTA_TEXT_ONBOARD_FLOW = "INBOX_SCREEN_CTA_TEXT_ONBOARD_FLOW"
        const val INBOX_SCREEN_CTA_TEXT_SUBSCRIPTION = "INBOX_SCREEN_CTA_TEXT_SUBSCRIPTION"
        const val INBOX_SCREEN_COURSE_PROGRESS = "INBOX_SCREEN_COURSE_PROGRESS_ENABLE"
        const val SUBSCRIPTION_COURSE_IDS = "SUBSCRIPTION_COURSE_IDS"


        // Trial End Screen
        const val TRAIL_END_SCREEN_MESSAGE = "TRAIL_END_SCREEN_MESSAGE"
        const val TRAIL_END_SCREEN_CTA_LABEL = "TRAIL_END_SCREEN_CTA_LABEL"
        const val SUBSCRIPTION_END_SCREEN_MESSAGE = "SUBSCRIPTION_END_SCREEN_MESSAGE"
        const val SUBSCRIPTION_END_SCREEN_CTA_LABEL = "SUBSCRIPTION_END_SCREEN_CTA_LABEL"

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

        //subscription bb tip

        const val SUBSCRIPTION_BB_TIP = "SUBSCRIPTION_BB_TIP"
        const val SUBSCRIPTION_BB_TEXT = "SUBSCRIPTION_BB_TEXT"

        // Course Explore Screen
        const val FFCOURSE_CARD_CLICK_MSG = "FFCOURSE_CARD_CLICK_MSG"

        // Payment Summary Screen
        const val PAYMENT_SUMMARY_CTA_LABEL_FREE = "PAYMENT_SUMMARY_CTA_LABEL_FREE"
        const val CTA_PAYMENT_SUMMARY = "CTA_PAYMENT_SUMMARY"

        // Reminder screen
        const val SET_REMINDER_DESCRIPTION = "SET_REMINDER_DESCRIPTION"
        const val REMINDERS_SCREEN_DESCRIPTION = "REMINDERS_SCREEN_DESCRIPTION"
        const val REMINDER_NOTIFIER_SCREEN_DESCRIPTION = "REMINDER_NOTIFIER_SCREEN_DESCRIPTION"
        const val REMINDER_BOTTOM_POPUP_DESCRIPTION = "REMINDER_BOTTOM_POPUP_DESCRIPTION"
        const val REMINDER_NOTIFICATION_TITLE = "REMINDER_NOTIFICATION_TITLE"
        const val REMINDER_NOTIFICATION_DESCRIPTION = "REMINDER_NOTIFICATION_DESCRIPTION"

        const val IS_APPLY_COUPON_ENABLED = "IS_APPLY_COUPON_ENABLED"
        const val APPLY_COUPON_TEXT = "APPLY_COUPON_TEXT"
        const val NEW_ONBOARD_FLOW_TEXT_ON_ENROLLED = "NEW_ONBOARD_FLOW_TEXT_ON_ENROLLED"

        //Course Detail Screen
        const val COURSE_MAX_OFFER_PER = "COURSE_MAX_OFFER_PER"
        const val BUY_COURSE_OFFER_HINT = "BUY_COURSE_OFFER_HINT"
        const val BUY_COURSE_LAST_DAY_OFFER_HINT = "BUY_COURSE_LAST_DAY_OFFER_HINT"
        const val SHOW_DETAILS_LABEL = "show_details_label"


        //bb tool tip
        const val BB_TOOL_TIP_EXPIRY_TEXT = "BB_TOOL_TIP_EXPIRY_TEXT"
        const val BB_TOOL_TIP_BELOW_FIND_COURSE_TEXT = "BB_TOOL_TIP_BELOW_FIND_COURSE_TEXT"
        const val BB_TOOL_TIP_BELOW_FIND_COURSE_TEXT_FREE =
            "BB_TOOL_TIP_BELOW_FIND_COURSE_TEXT_FREE"
        const val INBOX_OVERLAY_TOOLTIP_D0_1 = "INBOX_OVERLAY_TOOLTIP_D0_1"
        const val INBOX_OVERLAY_TOOLTIP_D2_3 = "INBOX_OVERLAY_TOOLTIP_D2_3"
        const val BUY_COURSE_LABEL = "buy_course_label"

        //Settings
        const val SETTINGS_LOGOUT_CONFIRMATION = "SETTINGS_LOGOUT_CONFIRMATION"
        const val SETTINGS_SIGN_IN_PROMPT = "SETTINGS_SIGN_IN_PROMPT"
        const val SETTINGS_CLEAR_DATA_CONFIRMATION = "SETTINGS_CLEAR_DATA_CONFIRMATION"
        const val NOTIFICATION_DESCRIPTION_DISABLED = "NOTIFICATION_DESCRIPTION_DISABLED"
        const val NOTIFICATION_DESCRIPTION_ENABLED = "NOTIFICATION_DESCRIPTION_ENABLED"
        const val NOTIFICATION_SETTING_DESCRIPTION = "NOTIFICATION_SETTING_DESCRIPTION"
        const val LANGUAGES_SUPPORTED = "LANGUAGES_SUPPORTED"


        const val BB_TOOL_TIP_FIRST_TIME_TEXT = "BB_TOOL_TIP_FIRST_TIME_TEXT"
        const val BB_TOOL_TIP_FIRST_TIME_BTN_TEXT = "BB_TOOL_TIP_FIRST_TIME_BTN_TEXT"
        const val SHOW_BB_TOOL_TIP_FIRST_TIME = "SHOW_BB_TOOL_TIP_FIRST_TIME"

        //signin dialod desccriptiom
        const val SIGNIN_DIALOG_DESCRIPTION = "SIGNIN_DIALOG_DESCRIPTION"


        const val UX_CAM_FEATURE_ENABLE = "UX_CAM_FEATURE_ENABLE"

        const val FORCE_SIGN_IN_FEATURE_ENABLE = "FORCE_SIGN_IN_FEATURE_ENABLE"

        //In app review
        const val MINIMUM_TIME_TO_SHOW_REVIEW = "MINIMUM_TIME_TO_SHOW_REVIEW"
    }
}


sealed class AchievementLevel

data class Level1(val action: String = "level_1", val watchTime: Int = 10,val levelPosition:Int=1) : AchievementLevel()
data class Level2(val action: String = "level_2", val watchTime: Int = 20,val levelPosition:Int=2) : AchievementLevel()
data class Level3(val action: String = "level_3", val watchTime: Int = 30,val levelPosition:Int=3) : AchievementLevel()
data class Level4(val action: String = "level_4", val watchTime: Int = 40,val levelPosition:Int=4) : AchievementLevel()
data class Level5(val action: String = "level_5", val watchTime: Int = 50,val levelPosition:Int=5) : AchievementLevel()
data class Level6(val action: String = "level_6", val watchTime: Int = 60,val levelPosition:Int=6) : AchievementLevel()
data class Level7(val action: String = "level_7", val watchTime: Int = 70,val levelPosition:Int=7) : AchievementLevel()
