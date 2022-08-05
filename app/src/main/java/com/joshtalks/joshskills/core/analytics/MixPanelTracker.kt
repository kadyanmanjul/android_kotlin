package com.joshtalks.joshskills.core.analytics

import android.app.AlertDialog
import android.content.Context
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.mixpanel.android.mpmetrics.MixpanelAPI
import kotlinx.coroutines.sync.Mutex
import org.json.JSONObject

object MixPanelTracker {

    private val mutex = Mutex()
    private var eventName: String = ""
    private var params = JSONObject()

    val mixPanel: MixpanelAPI by lazy {
        MixpanelAPI.getInstance(
            AppObjectController.joshApplication,
            "4c574e3a5e6b933a0e55c88239f6e994"
        )
    }

    fun publishEvent(event: Event): MixPanelTracker {
        eventName = event.value
        return this
    }

//    fun registerSuperProperties(event: Event) : MixPanelTracker{
//        eventName = event.value
//        return this
//    }

    fun addParam(key: ParamKeys, value: String?): MixPanelTracker {
        params.put(key.value, value ?: EMPTY)
        return this
    }

    fun addParam(key: ParamKeys, value: Int?): MixPanelTracker {
        params.put(key.value, value ?: 0)
        return this
    }

    fun addParam(key: ParamKeys, value: Float?): MixPanelTracker {
        params.put(key.value, value ?: 0.0)
        return this
    }

    fun addParam(key: ParamKeys, value: Long?): MixPanelTracker {
        params.put(key.value, value ?: 0)
        return this
    }

    fun addParam(key: ParamKeys, value: Boolean?): MixPanelTracker {
        params.put(key.value, value ?: false)
        return this
    }

    fun addParam(key: ParamKeys, value: Double?): MixPanelTracker {
        params.put(key.value, value ?: 0.0)
        return this
    }

    fun push() {
//        mixPanel.track(eventName, params)
//        mixPanel.flush()
//        params = JSONObject()
//        eventName = ""
    }
}

enum class MixPanelEvent(override val value: String) : Event {
    /*login-registration*/
    START_NOW("start now"),
    LOGIN("login"),
    JI_HAAN("ji haan"),
    CONTINUE_WITH_NUMBER("continue with number"),
    USE_ANOTHER_METHOD("use another method"),
    REGISTER_WITH_NAME("register with name"),
    SEND_OTP("send otp"),
    SUBMIT_OTP("submit otp"),
    EDIT_NUMBER("edit number"),
    TRUECALLER_VERIFICATION("truecaller verification"),
    FACEBOOK_VERIFICATION("facebook verification"),
    GOOGLE_VERIFICATION("google verification"),
    INBOX_OPENED("inbox opened"),
    OPEN_COURSE_CHAT("open course chat"),
    FIND_MORE_COURSES("find more courses"),
    REGISTER_WITH_INFO("register with info"),
    ADD_PROFILE_PHOTO("add profile photo"),
    CAMERA_CLICKED("camera clicked"),
    GALLERY_CLICKED("gallery clicked"),
    SKIP_CLICKED("skip clicked"),
    LOGIN_FREE_TRIAL_CLICKED("login free trial clicked"),
    LOGIN_START_FREE_TRIAL("login start free trial"),
    TRUECALLER_VERIFICATION_CONTD("truecaller verification contd"),
    TRUECALLER_VERIFICATION_SKIP("truecaller verification skip"),
    LOGOUT_CLICKED("logout clicked"),
    REGISTRATION_START_COURSE("registration start course"),

    /*PAYMENTS*/
    SHOW_COURSE_DETAILS("show course details"),
    PAYMENT_STARTED("payment started"),
    PAYMENT_SUCCESS("payment success"),
    PAYMENT_FAILED("payment failed"),
    APPLY_COUPON_CLICKED("apply coupon clicked"),
    COUPON_APPLIED("coupon applied"),
    APPLY_COUPON_FAILED("apply coupon failed"),
    BUY_ENGLISH_COURSE("buy english course"),
    COURSE_START_NOW("course start now"),
    COURSE_UPGRADED("course upgraded"),
    RETRY_PAYMENT("retry payment"),
    WHATSAPP_CLICKED_PAYMENT_FAILED("whatsapp clicked payment failed"),
    COURSE_QNA_CLICKED("course qna clicked"),
    COURSE_QNA_QUESTION_CLICKED("course qna question clicked"),
    COURSE_VIEW_RATING("course view rating"),
    COURSE_PLAY_DEMO("course play demo"),
    COURSE_MEET_INSTRUCTOR("course meet instructor"),
    COURSE_DOWNLOAD_SYLLABUS("course download syllabus"),
    COURSE_CHECK_LOCATION("course check location"),
    COURSE_MEET_STUDENTS("course meet students"),
    COURSE_WHATSAPP_CLICKED("course whatsapp clicked"),
    SEE_COURSE_LIST("see course list"),
    PAYMENT_REGISTER_NOW("payment register now"),
    PAYMENT_START_MY_COURSE("payment start my course"),

    /*3 dots*/
    THREE_DOTS("3 dots"),
    PERSONAL_INFORMATION("personal information"),
    SETTINGS("settings"),
    LANGUAGE_CHANGED("language changed"),
    HINDI("hindi"),
    HINGLISH("hinglish"),
    REFERRAL_OPENED("referral opened"),
    COPY_REFERRAL("copy referral"),
    SHARE_REFERRAL_WHATSAPP("share referral whatsapp"),
    SHARE_REFERRAL_OTHERS("share referral others"),
    HELP("help"),
    CALL_HELPLINE("call helpline"),
    CHAT_WITH_AGENT("chat with agent"),
    FAQ("FAQ"),
    FAQ_COURSES("FAQ courses"),
    FAQ_PAYMENT_AND_REFUND("FAQ payment and refund"),
    FAQ_ACC_SETUP("FAQ acc setup"),
    FAQ_TECHNICAL_ISSUES("FAQ technical issues"),
    DOWNLOAD_QUALITY("download quality"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    SPEAKING_PARTNER_NOTIFICATION("speaking partner notification"),
    CLEAR_ALL_DOWNLOADS_CLICKED("clear all downloads clicked"),
    CLEAR_ALL_DOWNLOADS("clear all downloads"),
    PRIVACY_PROFILE("privacy profile"),
    RATE_US("rate us"),
    SIGN_OUT_CLICKED("sign out clicked"),
    SIGN_OUT("sign out"),
    USER_LOGGED_OUT("user logged out"),
    FAQ_QUESTION_CLICKED("FAQ question clicked"),
    FAQ_ANSWER_HELPFUL_YES("FAQ answer helpful yes"),
    FAQ_ANSWER_HELPFUL_NO("FAQ answer helpful no"),
    FAQ_GO_HOME("FAQ go home"),
    NOTIFICATIONS("notifications"),
    AUTO_START("auto start"),
    NOTIFICATION_GO_TO_SETTINGS("notification go to settings"),
    NOTIFICATION_NOT_NOW("notification not now"),
    AUTO_START_GO_TO_SETTINGS("auto start go to settings"),
    AUTO_START_NOT_NOW("auto start not now"),
    RESTART_COURSE_CLICKED("restart course clicked"),
    RESTART_COURSE_NOW("restart course now"),
    RESTART_COURSE_CANCEL("restart course cancel"),

    //Activity feed , leaderboard , profile
    LEADERBOARD_OPENED("leaderboard opened"),
    LEADERBOARD_CANCEL("leaderboard cancel"),
    CONVERSATION_POINTS_CLICKED("conversation points clicked"),
    LEADERBOARD_TODAY("leaderboard today"),
    LEADERBOARD_WEEK("leaderboard week"),
    LEADERBOARD_MONTH("leaderboard month"),
    LEADERBOARD_LIFETIME("leaderboard lifetime"),
    LEADERBOARD_MY_BATCH("leaderboard my batch"),
    YESTERDAY_STOD("yerterday's STOD"),
    PREV_MONTH_STOM("prev month STOM"),
    PREV_WEEK_STOW("prev week STOW"),
    VIEW_AWARDS("view awards"),
    VIEW_GROUPS("view groups"),
    VIEW_POINTS_HISTORY("view points history"),
    VIEW_MINUTES_SPOKEN("view minutes spoken"),
    HOW_TO_EARN_POINTS("how to earn points"),
    VIEW_PROFILE_PHOTO("view profile photo"),
    VIEW_PREVIOUS_PROFILE_PHOTO("view previous profile photo"),
    VIEW_PROFILE("view profile"),
    CLEAR_ALL_MEDIA_CLICKED("clear all media clicked"),
    CLEAR_ALL_MEDIA("clear all media"),
    FAVORITE_LIST("favorite list"),
    SEARCH_LEADERBOARD_CLICKED("search leaderboard clicked"),
    EDIT_PROFILE_CLICKED("edit profile clicked"),
    PROFILE_EDITED("profile edited"),
    EDIT_PROFILE_PHOTO_CLICKED("edit profile photo clicked"),
    ADD_PROFILE_PHOTO_CLICKED("add profile photo clicked"),
    REMOVE_PROFILE_PHOTO("remove profile photo"),
    VIEW_PHOTO("view photo"),


    //course completion
    LESSON_OPENED("lesson opened"),
    LESSON_COMPLETE("lesson complete"),
    UNLOCK_NEXT_LESSON("unlock next lesson"),
    LESSON_CONTINUE("lesson continue"),

    //grammar
    GRAMMAR_OPENED("grammar opened"),
    GRAMMAR_COMPLETE("grammar complete"),
    GRAMMAR_VIDEO_COMPLETE("grammar video complete"),
    GRAMMAR_PLAY_VIDEO("grammar play video"),
    GRAMMAR_NOTES("grammar notes"),
    GRAMMAR_QUIZ_START("grammar quiz start"),
    GRAMMAR_QUIZ_SUBMIT("grammar quiz submit"),
    GRAMMAR_QUIZ_REDO("grammar quiz redo"),
    GRAMMAR_QUIZ_SHOW_EXPLANATION("grammar quiz show explanation"),
    GRAMMAR_QUIZ_HIDE_EXPLANATION("grammar quiz hide explanation"),
    GRAMMAR_QUIZ_CONTINUE("grammar quiz continue"),
    GRAMMAR_CONTINUE("grammar continue"),
    GRAMMAR_QUIZ_NEXT_QUESTION("grammar quiz next question"),
    GRAMMAR_QUIZ_PREVIOUS_QUESTION("grammar quiz previous question"),

    //speaking
    SPEAKING_OPENED("speaking opened"),
    SPEAKING_COMPLETED("speaking completed"),
    CALL_PRACTICE_PARTNER("call pp"),
    CALL_FAV_PRACTICE_PARTNER("call fav pp"),
    CALL_NEW_STUDENT("call new student"),
    SPEAKING_CONTINUE("speaking continue"),
    CALL_CONNECTED("call connected"),
    MUTE("mute"),
    UNMUTE("unmute"),
    SPEAKER_ON("speaker on"),
    SPEAKER_OFF("speaker off"),
    CALL_END("call end"),
    ADD_FPP("add fpp"),
    REPORT_AND_BLOCK("report and block"),
    REPORT_REASON_SUBMIT("report reason submit"),
    CALL_END_REASON("call end reason"),

    //vocabulary
    VOCABULARY_OPENED("vocabulary opened"),
    VOCABULARY_COMPLETED("vocabulary completed"),
    VOCAB_EXPAND("vocab expand"),
    VOCAB_COLLAPSE("vocab collapse"),
    VOCAB_WORD_PLAY_PRONOUNCIATION("vocab word play pronounciation"),
    VOCAB_WORD_RECORD("vocab word record"),
    VOCAB_WORD_RECORDING_PLAY("vocab word recording play"),
    VOCAB_WORD_RECORDING_PAUSE("vocab word recording pause"),
    VOCAB_WORD_SUBMIT("vocab word submit"),
    VOCAB_REV_SUBMIT("vocab rev submit"),
    VOCAB_REV_CONTINUE("vocab rev continue"),
    VOCAB_RECORDING_DELETE("vocab recording delete"),
    VOCAB_RECORDING_PAUSE("vocab recording pause"),
    VOCAB_CLOSE("vocab close"),
    VOCAB_CONTINUE("vocab continue"),
    VOCAB_SHOW_EXPLANATION("vocab show explanation"),

    //reading
    READING_OPENED("reading opened"),
    READING_COMPLETED("reading completed"),
    READING_PLAY("reading play"),
    READING_RECORD("reading record"),
    READING_RECORDING_PLAY("reading recording play"),
    READING_SUBMIT("reading submit"),
    READING_RECORDING_DELETE("reading recording delete"),
    READING_RECORDING_PAUSE("reading recording pause"),
    READING_PAUSE("reading pause"),
    READING_CONTINUE("reading continue"),

    //COURSE OVERVIEW
    COURSE_OVERVIEW("course overview"),
    COURSE_OVERVIEW_LESSON_CLICKED("course overview lesson clicked"),
    COURSE_OVERVIEW_EXAM_CLICKED("course overview exam clicked"),
    LESSON_CLICK_OKAY("lesson click okay"),
    EXAM_CLICK_OKAY("exam click okay"),

    //EXAM
    CERTIFICATION_EXAM_OPENED("certification exam opened"),
    EXAM_STARTED("exam started"),
    EXAM_PAUSED("exam paused"),
    RESUME_EXAM("resume exam"),
    EXAM_FINISHED("exam finished"),
    EXAM_QUESTION_ATTEMPTED("exam question attempted"),
    EXAM_OVERVIEW("exam overview"),
    PREVIOUS_RESULTS("previous results"),
    CHECK_EXAM_RESULTS("check exam results"),
    CHECK_EXAM_RESULTS_CORRECT("check exam results - correct"),
    CHECK_EXAM_RESULTS_INCORRECT("check exam results - incorrect"),
    CHECK_EXAM_RESULTS_UNANSWERED("check exam results unanswered"),
    VIEW_QUESTION_RESULTS("view question results"),
    BOOKMARK_QUESTION("bookmark question"),
    EXAM_REATTEMPT("exam reattempt"),
    VIEW_QUESTION("view question"),
    CANCEL("cancel"),
    VIEW_ATTEMPT_1("view attempt 1"),
    VIEW_ATTEMPT_2("view attempt 2"),
    VIEW_ATTEMPT_3("view attempt 3"),
    VIEW_ATTEMPT_4("view attempt 4"),
    VIEW_ATTEMPT_5("view attempt 5"),

    //GROUPS
    GROUP_ICON_CLICKED("groups icon clicked"),
    FIND_GROUPS_TO_JOIN("find groups to join"),
    SEARCH_GROUPS("search groups"),
    OPEN_GROUP_SEARCH("open group from search"),
    OPEN_GROUP_INBOX("open group from inbox"),
    JOIN_GROUP("join group"),
    GROUP_MESSAGE_SENT("group message sent"),
    CALL_PP_FROM_GROUP_LESSON("call pp from group clicked"),
    CALL_PP_FROM_GROUP_CHAT("call pp from group"),
    NEW_GROUP_CLICKED("new group clicked"),
    NEW_GROUP_CREATED("new group created"),
    ADD_GROUP_PHOTO("add group photo clicked"),
    EDIT_GROUP_PHOTO("edit group photo clicked"),
    CHOOSE_GROUP_TYPE("choose group type"),
    OPEN_ADMIN_RESPONSIBILITY("open admin responsibility"),
    EXIT_GROUP_CLICKED("exit group clicked"),
    EXIT_GROUP("exit group"),
    VIEW_GROUP_INFO("view group info"),
    EDIT_GROUP_INFO_CLICKED("edit group info clicked"),
    GROUP_INFO_EDITED("group info edited"),
    OPEN_GROUP_REQUESTS("groups requests to join"),
    GROUP_REQUEST_ALLOW("groups allow to join"),
    GROUP_REQUEST_DECLINE("groups decline request"),
    REQUEST_TO_JOIN_GROUP("request to join clicked"),
    REQUEST_TO_JOIN_SUBMIT("request to join submit"),
    VIEW_PROFILE_GROUP("profile clicked"),
    VIEW_MORE_MEMBERS("view more group members"),

    //NOTIFICATIONS , TOOLTIP
    NOTIFICATION_RECEIVED("notification received"),
    NOTIFICATION_CLICKED("notification clicked"),
    NOTIFICATION_SEEN("notification seen"),
    CALL_ACCEPTED("call accepted"),
    CALL_DECLINED("call declined"),
    CALL_IGNORED("call ignored"),
    PERMISSION_POPUPS("permission popups"),

    //OTHER
    IS_REGISTRATION_FOR_FIRST_TIME("is registration for first time"),
    CHAT_ENTERED("chat entered"),
    GOAL("goal"),
    CHANGE_PROFILE_PHOTO_CLICKED("change profile photo clicked"),

    //back
    BACK("back"),

    //profile
    VIEW_ENROLLED_COURSES("view enrolled courses"),
    ADD_FUTURE_GOALS_CLICKED("add future goals clicked"),
    ADD_EDUCATION_CLICKED("add education clicked"),
    ADD_PROFILE_INFO_EXPAND("add profile info expand"),
    ADD_PROFILE_INFO_COLLAPSE("add profile info collapse"),
    FPP_REQUEST_SEND("fpp request send"),
    FPP_REQUEST_CANCEL("fpp request cancel"),
    FPP_REQUEST_CONFIRM("fpp request confirm"),
    FPP_REQUEST_NOT_NOW("fpp request not now"),

    //MORE BUTTON
    MORE_BUTTON_CLICKED("more button clicked"),
    FPP_REQUESTS_ICON_CLICKED("fpp requests icon clicked"),
    GROUPS_ICON_CLICKED("groups icon clicked"),
    SEE_ALL_REQUESTS("see all requests"),

    //FPP
    VIEW_RECENT_CALLS("view recent calls"),
    FPP_REQUEST_RESPOND("fpp request respond"),
    BLOCK_USER_CLICKED("block user clicked"),
    BLOCK_USER_YES("block user yes"),
    BLOCK_USER_NO("block user no"),

    //ACTIVITY_FEED
    ACTIVITY_FEED("activity feed"),
    ACTIVITY_FEED_PLAY_BUTTON_CLICKED("activity feed play button clicked"),

    FREE_TRIAL_ENDED_BUY_NOW("free trial ended buy now"),
    SHARE_WITH_FRIENDS_BUTTON("share with friends button"),
    SHARE_WITH_FRIENDS_ICON("share with friends icon"),
    GAME_BUTTON_CLICKED("game button clicked"),

    // Buy Layout Changed
    BUY_PAGE_COURSE_INFO_EXPAND("buy page course info expand"),
    BUY_PAGE_COURSE_INFO_COLLAPSE("buy page course info collapse"),

    //English Syllabus
    DOWNLOAD_SYLLABUS("download syllabus"),

    //Extend Free Trial
    EXTEND_FREE_TRIAL_OPENED("extend free trial opened"),
    FREE_TRIAL_EXTENDED("free trial extended"),

    //Speaking Introduction Video
    SPEAKING_VIDEO_PLAY("speaking video play"),
    SPEAKING_VIDEO_COMPLETE("speaking video complete"),
    HOW_TO_SPEAK("how to speak"),

    LANGUAGE_SELECTED("language selected"),

    EXPERIMENT_STARTED("experiment started"),
    APP_OPENED_FOR_FIRST_TIME("app opened for first time")
}

enum class ParamKeys(val value: String) {
    LESSON_ID("lesson id"),
    LESSON_NUMBER("lesson number"),
    NAME_ENTERED("name entered"),
    NAME_CHANGED("name changed"),
    IS_SUCCESS("is success"),
    COURSE_ID("course id"),
    COURSE_NAME("course name"),
    COURSE_PRICE("course price"),
    LOGOUT("logout"),
    TEST_ID("test id"),
    IS_COUPON_APPLIED("is coupon applied"),
    AMOUNT_PAID("amount paid"),
    DISCOUNTED_AMOUNT("discounted amount"),
    CATEGORY_NAME("category name"),
    CATEGORY_POSITION("category position"),
    CATEGORY_ID("category id"),
    QUESTION_ID("question id"),
    QUESTION_NAME("question name"),
    STUDENT_ID("student id"),
    QUESTION("question"),
    MENTOR_ID("mentor id"),
    ANSWER_SELECTED("answer selected"),
    IS_CORRECT_ANSWER("is correct answer"),
    INSTRUCTOR_NAME("instructor name"),
    STUDENT_NAME("student name"),
    IS_CHECKED("is checked"),
    GROUP_ID("group id"),
    IS_ADMIN("is admin"),
    SEARCH_KEY("search key"),
    GROUP_TYPE("group type"),
    ICON_ADDED("icon added"),
    CALL_TYPE("call type"),
    IS_FAV_CALL("is fav call"),
    IS_NEW_CALL("is new call"),
    IS_GROUP_CALL("is group call"),
    CALLIE_ID("callie id"),
    CALLER_ID("caller id"),
    CALL_ID("call id"),
    DURATION("duration"),
    ADD_FPP("add_fpp"),
    OPTION_ID("option id"),
    OPTION_NAME("option name"),
    FEEDBACK_OPTION("feedback option"),
    REPORTED_BY_ID("reported by id"),
    REPORTED_AGAINST_ID("reported against id"),
    EXAM_ID("exam id"),
    EXAM_TYPE("exam type"),
    ATTEMPT_NUMBER("attempt number"),
    LAST_ATTEMPT_DATE("late attempt date"),
    ATTEMPT_LEFT("attempt left"),
    MARKS("marks"),
    ANSWER_ID("answer id"),
    RECORD_DURATION("record duration"),
    CORRECT("correct"),
    INCORRECT("incorrect"),
    UNANSWERED("unanswered"),
    RESULT("result"),
    TITLE("title"),
    NAME("name"),
    ID("id"),
    NOTIFICATION_TYPE("notification type"),
    CONTENT("content"),
    AGORA_MENTOR_UID("agora mentor uid"),
    AGORA_CALL_ID("agora call id"),
    TIMESTAMP("timestamp"),
    FIRST_NAME("first name"),
    CHAT_TEXT("chat text"),
    ADDED_FPP("added fpp"),
    YOUR_AGORA_ID("your agora id"),
    VARIANT("variant"),
    VARIABLE("variable"),
    CAMPAIGN("campaign"),
    GOAL("goal"),
    CHANNEL_NAME("channel name"),
    IS_UNLOCKED("is unlocked"),
    CARD_NAME("card name"),
    HOMETOWN("hometown"),
    FUTURE_GOALS("future goals"),
    FAV_JOSH_TALKS("fav josh talks"),
    SUBJECT("subject"),
    SCHOOL("school"),
    YEAR_OF_COMPLETION("year of completion"),
    OCCUPATION("occupation"),
    COMPANY("company"),
    PHOTO("photo"),
    VIA("via"),
    IS_FIRST_TIME("is first time"),
    CLICKED_FROM("clicked from"),
    ANSWER("answer"),
    VIDEO_ID("video id"),
    SELECTED_LANGUAGE("selected language"),
    SCREEN_NAME("screen name"),
    IS_100_POINTS_OBTAINED_IN_FREE_TRIAL("is 100 points obtained in free trial"),
    PAYMENT_ID("payment_id"),
    DEVICE_ID("device_id")
}

interface Event {
    val value: String
}
