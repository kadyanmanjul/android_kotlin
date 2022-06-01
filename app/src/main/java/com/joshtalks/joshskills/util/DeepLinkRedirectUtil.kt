package com.joshtalks.joshskills.util

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import com.joshtalks.joshskills.base.constants.FROM_ACTIVITY
import com.joshtalks.joshskills.base.constants.INTENT_DATA_COURSE_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_TOPIC_ID
import com.joshtalks.joshskills.base.constants.STARTING_POINT
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.group.JoshGroupActivity
import com.joshtalks.joshskills.ui.help.HelpActivity
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.lesson.LessonActivity
import com.joshtalks.joshskills.ui.lesson.SPEAKING_POSITION
import com.joshtalks.joshskills.ui.voip.favorite.FavoriteListActivity
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity
import org.json.JSONObject

object DeepLinkRedirectUtil {

    fun getIntent(
        activity: Activity,
        jsonParams: JSONObject
    ): Intent? {
        return try {
            when (DeepLinkRedirect.getDeepLinkAction(jsonParams.getString(DeepLinkData.REDIRECT_TO.key))) {
                DeepLinkRedirect.GROUP_ACTIVITY -> getGroupActivityIntent(activity, jsonParams)
                DeepLinkRedirect.GROUP_CHAT_ACTIVITY -> getGroupActivityIntent(
                    activity,
                    jsonParams
                ) // TODO: Implement Group Chat Activity
                DeepLinkRedirect.CONVERSATION_ACTIVITY -> getConversationActivityIntent(
                    activity,
                    jsonParams
                )
                DeepLinkRedirect.P2P_ACTIVITY -> getP2PActivityIntent(activity, jsonParams)
                DeepLinkRedirect.FPP_ACTIVITY -> getFPPActivityIntent(activity, jsonParams)
                DeepLinkRedirect.CUSTOMER_SUPPORT_ACTIVITY -> getCustomerSupportActivityIntent(
                    activity
                )
                DeepLinkRedirect.LESSON_ACTIVITY -> getLessonActivityIntent(activity, jsonParams)
                DeepLinkRedirect.COURSE_DETAILS -> getCourseDetailsActivityIntent(
                    activity,
                    jsonParams
                )
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getConversationIdFromCourseId(courseId: String): String =
        AppObjectController.appDatabase.courseDao()
            .getConversationIdFromCourseId(courseId)


    @Throws(Exception::class)
    private fun getCourseDetailsActivityIntent(
        activity: Activity,
        jsonParams: JSONObject
    ) = CourseDetailsActivity.getIntent(
        activity,
        testId = jsonParams.getString(DeepLinkData.TEST_ID.key).toInt(),
        startedFrom = "Deep Link"
    ).apply {
        sendPendingIntentForActivityList(
            activity,
            arrayOf(getInboxActivityIntent(activity), this)
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
    private fun getP2PActivityIntent(activity: Activity, jsonParams: JSONObject) =
        if (PermissionUtils.isCallingPermissionEnabled(activity)) {
            Intent(activity, VoiceCallActivity::class.java).apply {
                putExtra(
                    INTENT_DATA_COURSE_ID,
                    jsonParams.getString(DeepLinkData.COURSE_ID.key)
                )
                putExtra(INTENT_DATA_TOPIC_ID, jsonParams.getString(DeepLinkData.TOPIC_ID.key))
                putExtra(STARTING_POINT, FROM_ACTIVITY)
                val lastLessonId = AppObjectController.appDatabase.lessonDao()
                    .getLastLessonIdForCourse(
                        jsonParams.getString(DeepLinkData.COURSE_ID.key).toInt()
                    )
                jsonParams.put(DeepLinkData.LESSON_ID.key, lastLessonId)
                sendPendingIntentForActivityList(
                    activity,
                    arrayOf(
                        getLessonActivityIntent(activity, jsonParams, speakingSection = true),
                        this
                    )
                )
            }
        } else getLessonActivityIntent(activity, jsonParams, speakingSection = true)

    @Throws(Exception::class)
    private fun getFPPActivityIntent(activity: Activity, jsonParams: JSONObject) =
        Intent(activity, FavoriteListActivity::class.java).apply {
            putExtra(
                CONVERSATION_ID,
                getConversationIdFromCourseId(jsonParams.getString(DeepLinkData.COURSE_ID.key))
            )
            sendPendingIntentForActivityList(
                activity,
                arrayOf(getConversationActivityIntent(activity, jsonParams), this)
            )
        }

    @Throws(Exception::class)
    private fun getLessonActivityIntent(
        activity: Activity,
        jsonParams: JSONObject,
        speakingSection: Boolean = false
    ): Intent {
        val lesson = AppObjectController.appDatabase.lessonDao()
            .getLesson(jsonParams.getString(DeepLinkData.LESSON_ID.key).toInt())
        val conversationId =
            getConversationIdFromCourseId(jsonParams.getString(DeepLinkData.COURSE_ID.key))
        return if (lesson != null) {
            LessonActivity.getActivityIntent(
                activity,
                lessonId = lesson.id,
                isNewGrammar = lesson.isNewGrammar,
                isLessonCompleted = lesson.status == LESSON_STATUS.CO,
                conversationId = conversationId,
            ).apply {
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
    private fun getGroupActivityIntent(activity: Activity, jsonParams: JSONObject) =
        Intent(activity, JoshGroupActivity::class.java).apply {
            putExtra(
                CONVERSATION_ID,
                getConversationIdFromCourseId(jsonParams.getString(DeepLinkData.COURSE_ID.key))
            )
            sendPendingIntentForActivityList(
                activity,
                arrayOf(getConversationActivityIntent(activity, jsonParams), this)
            )
        }

    @Throws(Exception::class)
    private fun getConversationActivityIntent(activity: Activity, jsonParams: JSONObject) =
        AppObjectController.appDatabase.courseDao()
            .getCourseAccordingId(jsonParams.getString(DeepLinkData.COURSE_ID.key))?.let {
                ConversationActivity.getConversationActivityIntent(activity, it).apply {
                    sendPendingIntentForActivityList(
                        activity,
                        arrayOf(getInboxActivityIntent(activity), this)
                    )
                }
            } ?: getInboxActivityIntent(activity)

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
    LESSON_ACTIVITY("lesson_activity");

    companion object {
        fun getDeepLinkAction(key: String): DeepLinkRedirect? {
            return values().firstOrNull { it.key == key }
        }
    }
}

enum class DeepLinkData(val key: String) {
    REDIRECT_TO("redirect_to"),
    NOTIFICATION_ID("notification_id"),
    NOTIFICATION_CHANNEL("notification_channel"),
    CONVERSATION_ID("conversation_id"),
    GROUP_ID("group_id"),
    LESSON_ID("lesson_id"),
    TEST_ID("test_id"),
    COURSE_ID("course_id"),
    TOPIC_ID("topic_id"),
}