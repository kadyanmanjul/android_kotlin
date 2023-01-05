package com.joshtalks.joshskills.deeplink

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import com.joshtalks.joshskills.auth.freetrail.FreeTrialOnBoardActivity
import com.joshtalks.joshskills.auth.freetrail.SignUpActivity
import com.joshtalks.joshskills.buypage.new_buy_page_layout.BuyPageActivity
import com.joshtalks.joshskills.common.constants.SPEAKING_POSITION
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.common.repository.local.model.User
import com.joshtalks.joshskills.common.repository.server.onboarding.SpecificOnboardingCourseData
import com.joshtalks.joshskills.common.track.CONVERSATION_ID
import com.joshtalks.joshskills.common.ui.chat.ConversationActivity
import com.joshtalks.joshskills.common.ui.help.HelpActivity
import com.joshtalks.joshskills.common.ui.inbox.InboxActivity
import com.joshtalks.joshskills.common.ui.special_practice.utils.COUPON_CODE
import com.joshtalks.joshskills.common.ui.special_practice.utils.FLOW_FROM
import com.joshtalks.joshskills.common.ui.voip.favorite.FavoriteListActivity
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.joshtalks.joshskills.explore.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.groups.JoshGroupActivity
import com.joshtalks.joshskills.lesson.LessonActivity
import com.joshtalks.joshskills.lesson.LessonActivity.Companion.IS_DEMO
import com.joshtalks.joshskills.lesson.LessonActivity.Companion.IS_LESSON_COMPLETED
import com.joshtalks.joshskills.lesson.LessonActivity.Companion.LESSON_ID
import com.joshtalks.joshskills.voip.base.constants.*
import com.joshtalks.joshskills.voip.constant.Category
import org.json.JSONObject

object DeepLinkRedirectUtil {

    suspend fun getIntent(
        activity: Activity,
        jsonParams: JSONObject,
        isFreeTrial: Boolean = false
    ): Boolean {
        try {
            when (DeepLinkRedirect.getDeepLinkAction(jsonParams.getString(DeepLinkData.REDIRECT_TO.key))) {
                DeepLinkRedirect.GROUP_ACTIVITY ->
                    if (isFreeTrial) getConversationActivityIntent(activity, jsonParams)
                    else getGroupActivityIntent(activity, jsonParams)
                DeepLinkRedirect.GROUP_CHAT_ACTIVITY ->
                    if (isFreeTrial) getConversationActivityIntent(activity, jsonParams)
                    else { // TODO: Implement Open Group Chat Activity
                        getGroupActivityIntent(
                            activity,
                            jsonParams
                        )
                    }
                DeepLinkRedirect.CONVERSATION_ACTIVITY ->
                    getConversationActivityIntent(
                        activity,
                        jsonParams
                    )
                DeepLinkRedirect.P2P_ACTIVITY -> getP2PActivityIntent(activity, jsonParams)
                DeepLinkRedirect.FPP_ACTIVITY ->
                    if (isFreeTrial) getP2PActivityIntent(activity, jsonParams)
                    else getFPPActivityIntent(activity, jsonParams)
                DeepLinkRedirect.CUSTOMER_SUPPORT_ACTIVITY ->
                    getCustomerSupportActivityIntent(
                        activity
                    )
                DeepLinkRedirect.LESSON_ACTIVITY -> getLessonActivityIntent(activity, jsonParams)
                DeepLinkRedirect.COURSE_DETAILS ->
                    getCourseDetailsActivityIntent(
                        activity,
                        jsonParams
                    )
                DeepLinkRedirect.P2P_FREE_TRIAL_ACTIVITY -> {
                    getP2PActivityFreeTrialIntent(activity, jsonParams)
                }
                DeepLinkRedirect.BUY_PAGE_ACTIVITY -> getBuyPageActivityIntent(activity, jsonParams)
                else -> return false
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun getIntentForCourseOnboarding(
        activity: Activity,
        jsonParams: JSONObject? = null,
        isSpecialCourse: Boolean
    ): Intent {
        if (jsonParams != null) {
            if (isSpecialCourse && jsonParams.has(DeepLinkData.COURSE_ID.key) && jsonParams.has(
                    DeepLinkData.PLAN_ID.key))
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
        return Intent(activity, FreeTrialOnBoardActivity::class.java)
    }

    private fun getCourseId(jsonParams: JSONObject): Int =
        if (jsonParams.has(DeepLinkData.COURSE_ID.key))
            jsonParams.getString(DeepLinkData.COURSE_ID.key).toInt()
        else
            PrefManager.getStringValue(CURRENT_COURSE_ID).toInt()

    private suspend fun getConversationIdFromCourseId(jsonParams: JSONObject): String? =
        AppObjectController.appDatabase.courseDao()
            .getConversationIdFromCourseId(getCourseId(jsonParams).toString())


    @Throws(Exception::class)
    fun getCourseDetailsActivityIntent(
        activity: Activity,
        jsonParams: JSONObject
    ) = CourseDetailsActivity.getIntent(
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
                    Intent(activity, FreeTrialOnBoardActivity::class.java)
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
    private suspend fun getP2PActivityIntent(activity: Activity, jsonParams: JSONObject) =
        if (PrefManager.getBoolValue(IS_FREE_TRIAL_ENDED))
            getConversationActivityIntent(activity, jsonParams)
        else if (PermissionUtils.isCallingPermissionEnabled(activity)) {
            Intent(activity, VoiceCallActivity::class.java).apply {
                putExtra(
                    INTENT_DATA_COURSE_ID,
                    getCourseId(jsonParams)
                )
                putExtra(INTENT_DATA_TOPIC_ID, jsonParams.getString(DeepLinkData.TOPIC_ID.key))
                putExtra(STARTING_POINT, FROM_ACTIVITY)
                sendPendingIntentForActivityList(
                    activity,
                    arrayOf(
                        getLessonActivityIntent(activity, jsonParams, speakingSection = true),
                        this
                    )
                )
            }
        } else
            getLessonActivityIntent(activity, jsonParams, speakingSection = true)

    @Throws(Exception::class)
    private fun getP2PActivityFreeTrialIntent(activity: Activity, jsonParams: JSONObject) =
        activity.startActivity(
            Intent(activity, VoiceCallActivity::class.java).apply {
                putExtra(INTENT_DATA_COURSE_ID, "151")
                putExtra(INTENT_DATA_TOPIC_ID, "10")
                putExtra(
                    STARTING_POINT,
                    FROM_ACTIVITY
                )
                putExtra(INTENT_DATA_CALL_CATEGORY, Category.PEER_TO_PEER.ordinal)
            }
        )

    @Throws(Exception::class)
    private suspend fun getFPPActivityIntent(activity: Activity, jsonParams: JSONObject) =
        getConversationIdFromCourseId(jsonParams)?.let {
            Intent(activity, FavoriteListActivity::class.java).apply {
                putExtra(CONVERSATION_ID, it)
                sendPendingIntentForActivityList(
                    activity,
                    arrayOf(getConversationActivityIntent(activity, jsonParams), this)
                )
            }
        } ?: getConversationActivityIntent(activity, jsonParams)

    @Throws(Exception::class)
    private suspend fun getLessonActivityIntent(
        activity: Activity,
        jsonParams: JSONObject,
        speakingSection: Boolean = false
    ): Intent {
        if (PrefManager.getBoolValue(IS_FREE_TRIAL_ENDED))
            return getConversationActivityIntent(activity, jsonParams)
        val lessonId =
            if (jsonParams.has(DeepLinkData.LESSON_ID.key) &&
                jsonParams.getString(DeepLinkData.LESSON_ID.key).isNullOrEmpty().not()
            )
                jsonParams.getString(DeepLinkData.LESSON_ID.key).toInt()
            else
                AppObjectController.appDatabase.lessonDao()
                    .getLastLessonIdForCourse(getCourseId(jsonParams))
        val lesson = AppObjectController.appDatabase.lessonDao().getLesson(lessonId)
        val conversationId = getConversationIdFromCourseId(jsonParams)
        return if (lesson != null && conversationId != null) {
            Intent(activity, LessonActivity::class.java).apply {
                putExtra(LESSON_ID, lesson.id)
                putExtra(IS_DEMO, false)
                putExtra(IS_LESSON_COMPLETED, lesson.status == LESSON_STATUS.CO)
                putExtra(CONVERSATION_ID, conversationId)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

                if (speakingSection) putExtra(LessonActivity.LESSON_SECTION, SPEAKING_POSITION)
                sendPendingIntentForActivityList(
                    activity,
                    arrayOf(getConversationActivityIntent(activity, jsonParams), this)
                )
            }
        } else {
            getConversationActivityIntent(activity, jsonParams)
        }
    }

    @Throws(Exception::class)
    private suspend fun getGroupActivityIntent(activity: Activity, jsonParams: JSONObject) =
        getConversationIdFromCourseId(jsonParams)?.let {
            Intent(activity, JoshGroupActivity::class.java).apply {
                putExtra(CONVERSATION_ID, it)
                sendPendingIntentForActivityList(
                    activity,
                    arrayOf(getConversationActivityIntent(activity, jsonParams), this)
                )
            }
        } ?: getConversationActivityIntent(activity, jsonParams)

    @Throws(Exception::class)
    private suspend fun getConversationActivityIntent(activity: Activity, jsonParams: JSONObject): Intent =
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
    private fun getBuyPageActivityIntent(activity: Activity, jsonParams: JSONObject): Intent =
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