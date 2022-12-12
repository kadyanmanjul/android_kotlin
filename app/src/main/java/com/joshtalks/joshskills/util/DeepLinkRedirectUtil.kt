package com.joshtalks.joshskills.util

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.onboarding.SpecificOnboardingCourseData
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.group.JoshGroupActivity
import com.joshtalks.joshskills.ui.help.HelpActivity
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.launch.getStringOrNull
import com.joshtalks.joshskills.ui.lesson.LessonActivity
import com.joshtalks.joshskills.ui.lesson.SPEAKING_POSITION
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.BuyPageActivity
import com.joshtalks.joshskills.ui.signup.FreeTrialOnBoardActivity
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.ui.special_practice.utils.COUPON_CODE
import com.joshtalks.joshskills.ui.special_practice.utils.FLOW_FROM
import com.joshtalks.joshskills.ui.voip.favorite.FavoriteListActivity
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.joshtalks.joshskills.util.DeepLinkRedirectUtil.DeepLinkType.*
import com.joshtalks.joshskills.voip.constant.Category
import io.branch.referral.Defines
import org.json.JSONObject

class DeepLinkRedirectUtil(private val jsonParams: JSONObject) {

    //open an activity or return false if an activity is not found
    suspend fun redirectFromDeepLink(
        activity: Activity,
        isFreeTrial: Boolean = false
    ): Boolean {
        try {
            when (DeepLinkRedirect.getDeepLinkAction(jsonParams.getString(DeepLinkData.REDIRECT_TO.key))) {
                DeepLinkRedirect.GROUP_ACTIVITY ->
                    if (isFreeTrial) getConversationActivityIntent(activity)
                    else getGroupActivityIntent(activity)
                DeepLinkRedirect.GROUP_CHAT_ACTIVITY ->
                    if (isFreeTrial) getConversationActivityIntent(activity)
                    else { // TODO: Implement Open Group Chat Activity
                        getGroupActivityIntent(activity)
                    }
                DeepLinkRedirect.CONVERSATION_ACTIVITY ->
                    getConversationActivityIntent(activity)
                DeepLinkRedirect.P2P_ACTIVITY -> getP2PActivityIntent(activity)
                DeepLinkRedirect.FPP_ACTIVITY ->
                    if (isFreeTrial) getP2PActivityIntent(activity)
                    else getFPPActivityIntent(activity)
                DeepLinkRedirect.CUSTOMER_SUPPORT_ACTIVITY -> getCustomerSupportActivityIntent(activity)
                DeepLinkRedirect.LESSON_ACTIVITY -> getLessonActivityIntent(activity)
                DeepLinkRedirect.COURSE_DETAILS -> getCourseDetailsActivityIntent(activity)
                DeepLinkRedirect.P2P_FREE_TRIAL_ACTIVITY -> getP2PActivityFreeTrialIntent(activity)
                DeepLinkRedirect.BUY_PAGE_ACTIVITY -> getBuyPageActivityIntent(activity)
                else -> return false
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun initCourseOnboarding(isSpecialCourse: Boolean) {
        if (isSpecialCourse && jsonParams.has(DeepLinkData.COURSE_ID.key) && jsonParams.has(DeepLinkData.PLAN_ID.key))
            PrefManager.put(
                key = SPECIFIC_ONBOARDING,
                value = AppObjectController.gsonMapper.toJson(
                    SpecificOnboardingCourseData(
                        jsonParams.getString(DeepLinkData.COURSE_ID.key),
                        jsonParams.getString(DeepLinkData.PLAN_ID.key)
                    )
                ),
                isConsistent = true
            )
        else if (jsonParams.has(DeepLinkData.TEST_ID.key))
            PrefManager.put(FT_COURSE_ONBOARDING, jsonParams.getString(DeepLinkData.TEST_ID.key))
    }

    private fun getCourseId(): Int =
        jsonParams.getStringOrNull(DeepLinkData.COURSE_ID.key)?.toInt()
            ?: PrefManager.getStringValue(CURRENT_COURSE_ID).toInt()

    private suspend fun getConversationIdFromCourseId(): String? =
        AppObjectController.appDatabase.courseDao()
            .getConversationIdFromCourseId(getCourseId().toString())


    @Throws(Exception::class)
    fun getCourseDetailsActivityIntent(activity: Activity) = CourseDetailsActivity.getIntent(
        activity,
        testId = jsonParams.getString(DeepLinkData.TEST_ID.key).toInt(),
        startedFrom = "Deep Link"
    ).apply {
        val activityList = mutableListOf<Intent>()
        if (User.getInstance().isVerified.not()) {
            if (PrefManager.getBoolValue(IS_GUEST_ENROLLED, false) &&
                PrefManager.getBoolValue(IS_PAYMENT_DONE, false).not()
            )
                activityList.add(Intent(getInboxActivityIntent(activity)))
            else if (PrefManager.getBoolValue(IS_PAYMENT_DONE, false))
                activityList.add(Intent(activity, SignUpActivity::class.java))
            else if (PrefManager.getBoolValue(IS_FREE_TRIAL, false, false))
                activityList.add(
                    Intent(
                        activity,
                        FreeTrialOnBoardActivity::class.java
                    )
                )
            else
                activityList.add(Intent(activity, SignUpActivity::class.java))
        } else
            activityList.add(getInboxActivityIntent(activity))
        activityList.add(this)
        sendPendingIntentForActivityList(
            activity,
            activityList.toTypedArray()
        )
    }

    @Throws(Exception::class)
    private fun getCustomerSupportActivityIntent(activity: Activity) = Intent(
        activity,
        HelpActivity::class.java
    ).apply {
        sendPendingIntentForActivityList(
            activity,
            arrayOf(getInboxActivityIntent(activity), this)
        )
    }

    @Throws(Exception::class)
    private suspend fun getP2PActivityIntent(activity: Activity) =
        if (PrefManager.getBoolValue(IS_FREE_TRIAL_ENDED))
            getConversationActivityIntent(activity)
        else if (PermissionUtils.isCallingPermissionEnabled(activity)) {
            Intent(activity, VoiceCallActivity::class.java).apply {
                putExtra(
                    INTENT_DATA_COURSE_ID,
                    getCourseId()
                )
                putExtra(INTENT_DATA_TOPIC_ID, jsonParams.getString(DeepLinkData.TOPIC_ID.key))
                putExtra(STARTING_POINT, FROM_ACTIVITY)
                sendPendingIntentForActivityList(
                    activity,
                    arrayOf(
                        getLessonActivityIntent(activity, speakingSection = true),
                        this
                    )
                )
            }
        } else
            getLessonActivityIntent(activity, speakingSection = true)

    @Throws(Exception::class)
    private fun getP2PActivityFreeTrialIntent(activity: Activity) =
        activity.startActivity(
            Intent(activity, VoiceCallActivity::class.java).apply {
                putExtra(INTENT_DATA_COURSE_ID, "151")
                putExtra(INTENT_DATA_TOPIC_ID, "5")
                putExtra(STARTING_POINT, FROM_ACTIVITY)
                putExtra(INTENT_DATA_CALL_CATEGORY, Category.PEER_TO_PEER.ordinal)
            }
        )

    @Throws(Exception::class)
    private suspend fun getFPPActivityIntent(activity: Activity) =
        getConversationIdFromCourseId()?.let {
            Intent(activity, FavoriteListActivity::class.java).apply {
                putExtra(CONVERSATION_ID, it)
                sendPendingIntentForActivityList(
                    activity,
                    arrayOf(getConversationActivityIntent(activity), this)
                )
            }
        } ?: getConversationActivityIntent(activity)

    @Throws(Exception::class)
    private suspend fun getLessonActivityIntent(
        activity: Activity,
        speakingSection: Boolean = false
    ): Intent {
        if (PrefManager.getBoolValue(IS_FREE_TRIAL_ENDED))
            return getConversationActivityIntent(activity)
        val lessonId =
            jsonParams.getStringOrNull(DeepLinkData.LESSON_ID.key)?.toInt()
                ?: AppObjectController.appDatabase.lessonDao().getLastLessonIdForCourse(getCourseId())

        val lesson = AppObjectController.appDatabase.lessonDao().getLesson(lessonId)
        val conversationId = getConversationIdFromCourseId()
        return if (lesson != null && conversationId != null) {
            LessonActivity.getActivityIntent(
                activity,
                lessonId = lesson.id,
                conversationId = conversationId,
                isLessonCompleted = lesson.status == LESSON_STATUS.CO,
            ).apply {
                if (speakingSection) putExtra(LessonActivity.LESSON_SECTION, SPEAKING_POSITION)
                sendPendingIntentForActivityList(
                    activity,
                    arrayOf(getConversationActivityIntent(activity), this)
                )
            }
        } else {
            getConversationActivityIntent(activity)
        }
    }

    @Throws(Exception::class)
    private suspend fun getGroupActivityIntent(activity: Activity) =
        getConversationIdFromCourseId()?.let {
            Intent(activity, JoshGroupActivity::class.java).apply {
                putExtra(CONVERSATION_ID, it)
                sendPendingIntentForActivityList(
                    activity,
                    arrayOf(getConversationActivityIntent(activity), this)
                )
            }
        } ?: getConversationActivityIntent(activity)

    @Throws(Exception::class)
    private suspend fun getConversationActivityIntent(activity: Activity): Intent =
        AppObjectController.appDatabase.courseDao()
            .getCourseFromId(jsonParams.getString(DeepLinkData.COURSE_ID.key))?.let {
                ConversationActivity.getConversationActivityIntent(activity, it).apply {
                    sendPendingIntentForActivityList(
                        activity,
                        arrayOf(getInboxActivityIntent(activity), this)
                    )
                }
            } ?: getInboxActivityIntent(activity)

    @Throws(Exception::class)
    private fun getBuyPageActivityIntent(activity: Activity): Intent =
        if (PrefManager.getBoolValue(IS_FREE_TRIAL)) {
            Intent(activity, BuyPageActivity::class.java).apply {
                putExtra(FLOW_FROM, "Deep Link")
                putExtra(COUPON_CODE, jsonParams.getString(DeepLinkData.COUPON_CODE.key))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                sendPendingIntentForActivityList(
                    activity,
                    arrayOf(getInboxActivityIntent(activity), this)
                )
            }
        } else getInboxActivityIntent(activity)

    private fun getInboxActivityIntent(activity: Activity): Intent =
        InboxActivity.getInboxIntent(activity).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun sendPendingIntentForActivityList(
        activity: Activity,
        activityList: Array<Intent>
    ) = PendingIntent.getActivities(
        activity,
        (System.currentTimeMillis() and 0xfffffff).toInt(),
        activityList,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else
            PendingIntent.FLAG_UPDATE_CURRENT
    ).send()

    suspend fun handleDeepLink(activity: CoreJoshActivity): RedirectAction? =
        when {
            User.getInstance().isVerified.not() -> handleDeepLinkForUnverifiedUser(activity)
            activity.isUserProfileNotComplete() -> RedirectAction.SIGN_UP
            redirectFromDeepLink(activity, PrefManager.getBoolValue(IS_FREE_TRIAL)) -> null
            else -> RedirectAction.INBOX
        }

    private suspend fun handleDeepLinkForUnverifiedUser(
        activity: CoreJoshActivity,
    ): RedirectAction? =
        when {
            //if guest is enrolled and not paid (free trial)
            isGuestEnrolled() -> {
                if (redirectFromDeepLink(activity, true))
                    null
                else
                    RedirectAction.INBOX
            }

            isSpecificOnboarding() -> {
                initCourseOnboarding(true)
                RedirectAction.COURSE_ONBOARDING
            }

            isFreeTrialOnboarding() -> {
                initCourseOnboarding(false)
                RedirectAction.COURSE_ONBOARDING
            }

            isCourseDetailsRedirect() -> {
                getCourseDetailsActivityIntent(activity)
                null
            }
            else -> RedirectAction.INBOX
        }

    private inline fun isCourseDetailsRedirect(): Boolean =
        jsonParams.getStringOrNull(DeepLinkData.REDIRECT_TO.key) == DeepLinkRedirect.COURSE_DETAILS.key

    private inline fun isGuestEnrolled(): Boolean =
        (PrefManager.getBoolValue(IS_GUEST_ENROLLED, false) &&
                PrefManager.getBoolValue(IS_PAYMENT_DONE, false).not())

    private inline fun isSpecificOnboarding(): Boolean =
        PrefManager.hasKey(
            SPECIFIC_ONBOARDING,
            isConsistent = true
        ) || (jsonParams.getStringOrNull(DeepLinkData.REDIRECT_TO.key) == DeepLinkRedirect.ONBOARDING.key)


    private fun isFreeTrialOnboarding(): Boolean =
        PrefManager.hasKey(FT_COURSE_ONBOARDING, isConsistent = true) ||
                (jsonParams.getStringOrNull(DeepLinkData.REDIRECT_TO.key) == DeepLinkRedirect.FT_COURSE.key)

    suspend fun handleBranchAnalytics() {
        when (getDeepLinkType()) {
            NOTIFICATION ->
                addDeepLinkNotificationAnalytics(
                    jsonParams.getString((DeepLinkData.NOTIFICATION_ID.key)),
                    jsonParams.getString(DeepLinkData.NOTIFICATION_CHANNEL.key)
                )
            REFERRAL ->
                saveDeepLinkImpression(
                    deepLink = getDeepLink(),
                    action = DeepLinkImpression.REFERRAL.name
                )
            REDIRECT ->
                saveDeepLinkImpression(
                    deepLink = getDeepLink(),
                    action = getRedirectAction(),
                )
            OTHER ->
                saveDeepLinkImpression(
                    deepLink = getDeepLink(),
                    action = DeepLinkImpression.OTHER.name
                )
        }
    }

    private fun getRedirectAction(): String =
        "${DeepLinkImpression.REDIRECT_}${
            jsonParams.getString(DeepLinkData.REDIRECT_TO.key).uppercase()
        }${
            when (jsonParams.getString(DeepLinkData.REDIRECT_TO.key)) {
                DeepLinkRedirect.ONBOARDING.key -> "_${jsonParams.getString(DeepLinkData.COURSE_ID.key)}"
                DeepLinkRedirect.COURSE_DETAILS.key -> "_${jsonParams.getString(DeepLinkData.TEST_ID.key)}"
                else -> ""
            }
        }"

    private fun getDeepLinkType() =
        when {
            isNotificationLink() -> NOTIFICATION
            jsonParams.has(DeepLinkData.REDIRECT_TO.key) -> REDIRECT
            isReferralLink() -> REFERRAL
            else -> OTHER
        }

    private inline fun isNotificationLink() =
        jsonParams.has(DeepLinkData.NOTIFICATION_ID.key) && jsonParams.has(DeepLinkData.NOTIFICATION_CHANNEL.key)

    fun isReferralLink(): Boolean =
        jsonParams.getStringOrNull(Defines.Jsonkey.UTMMedium.key) == "referral"

    fun isRedirectLink(): Boolean =
        jsonParams.has(DeepLinkData.REDIRECT_TO.key)

    private fun getDeepLink(): String =
        jsonParams.getStringOrNull(DeepLinkData.REFERRING_LINK.key) ?: ""

    suspend fun saveDeepLinkImpression(deepLink: String, action: String) {
        try {
            AppObjectController.commonNetworkService.saveDeepLinkImpression(
                mapOf(
                    "mentor" to Mentor.getInstance().getId(),
                    "deep_link" to deepLink,
                    "link_action" to action
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addDeepLinkNotificationAnalytics(notificationID: String, notificationChannel: String) {
        NotificationAnalytics().addAnalytics(
            notificationId = notificationID,
            mEvent = NotificationAnalytics.Action.CLICKED,
            channel = notificationChannel
        )
    }

    private enum class DeepLinkType {
        NOTIFICATION,
        REFERRAL,
        REDIRECT,
        OTHER
    }
}

enum class RedirectAction {
    SIGN_UP,
    COURSE_ONBOARDING,
    INBOX,
}


enum class DeepLinkRedirect(val key: String) {
    GROUP_ACTIVITY("group_activity"),
    GROUP_CHAT_ACTIVITY("group_chat_activity"),
    CONVERSATION_ACTIVITY("conversation_activity"),
    P2P_ACTIVITY("p2p_activity"),
    FPP_ACTIVITY("fpp_activity"),
    COURSE_DETAILS("course_details"),
    CUSTOMER_SUPPORT_ACTIVITY("customer_support_activity"),
    LESSON_ACTIVITY("lesson_activity"),
    ONBOARDING("onboarding"),
    FT_COURSE("ft_course"),
    P2P_FREE_TRIAL_ACTIVITY("p2p_free_trial_activity"),
    LOGIN("login"),
    BUY_PAGE_ACTIVITY("buy_page_activity"), ;

    companion object {
        fun getDeepLinkAction(key: String): DeepLinkRedirect? {
            return values().firstOrNull { it.key == key }
        }
    }
}

enum class DeepLinkData(val key: String) {
    REDIRECT_TO("redirect_to"),
    REFERRING_LINK("~referring_link"),
    NOTIFICATION_ID("notification_id"),
    NOTIFICATION_CHANNEL("notification_channel"),
    CONVERSATION_ID("conversation_id"),
    GROUP_ID("group_id"),
    LESSON_ID("lesson_id"),
    TEST_ID("test_id"),
    COURSE_ID("course_id"),
    TOPIC_ID("topic_id"),
    PLAN_ID("plan_id"),
    COUPON_CODE("coupon_code"),
}