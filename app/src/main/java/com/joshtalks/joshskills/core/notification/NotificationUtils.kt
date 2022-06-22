package com.joshtalks.joshskills.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.facebook.share.internal.ShareConstants
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_INCOMING_CALL
import com.joshtalks.joshskills.conversationRoom.liveRooms.ConversationLiveRoomActivity
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.DismissNotifEventReceiver
import com.joshtalks.joshskills.core.firestore.FirestoreDB
import com.joshtalks.joshskills.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.core.io.LastSyncPrefManager
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.NotificationAction
import com.joshtalks.joshskills.repository.local.model.NotificationChannelNames
import com.joshtalks.joshskills.repository.local.model.NotificationObject
import com.joshtalks.joshskills.repository.local.model.ShortNotificationObject
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.assessment.AssessmentActivity
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.chat.UPDATED_CHAT_ROOM_OBJECT
import com.joshtalks.joshskills.ui.conversation_practice.ConversationPracticeActivity
import com.joshtalks.joshskills.ui.conversation_practice.PRACTISE_ID
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.fpp.SeeAllRequestsActivity
import com.joshtalks.joshskills.ui.group.JoshGroupActivity
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.launch.LauncherActivity
import com.joshtalks.joshskills.ui.leaderboard.LeaderBoardViewPagerActivity
import com.joshtalks.joshskills.ui.lesson.LessonActivity
import com.joshtalks.joshskills.ui.lesson.SPEAKING_POSITION
import com.joshtalks.joshskills.ui.payment.FreeTrialPaymentActivity
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.reminder.reminder_listing.ReminderListActivity
import com.joshtalks.joshskills.ui.signup.FreeTrialOnBoardActivity
import com.joshtalks.joshskills.ui.voip.OPPOSITE_USER_UID
import com.joshtalks.joshskills.ui.voip.RTC_CALLER_PHOTO
import com.joshtalks.joshskills.ui.voip.RTC_CALLER_UID_KEY
import com.joshtalks.joshskills.ui.voip.RTC_CALL_ID
import com.joshtalks.joshskills.ui.voip.RTC_CHANNEL_KEY
import com.joshtalks.joshskills.ui.voip.RTC_IS_FAVORITE
import com.joshtalks.joshskills.ui.voip.RTC_IS_GROUP_CALL
import com.joshtalks.joshskills.ui.voip.RTC_NAME
import com.joshtalks.joshskills.ui.voip.RTC_TOKEN_KEY
import com.joshtalks.joshskills.ui.voip.RTC_UID_KEY
import com.joshtalks.joshskills.ui.voip.RTC_WEB_GROUP_CALL_GROUP_NAME
import com.joshtalks.joshskills.ui.voip.RTC_WEB_GROUP_PHOTO
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.joshtalks.joshskills.ui.voip.analytics.VoipAnalytics
import com.joshtalks.joshskills.ui.voip.favorite.FavoriteListActivity
import com.joshtalks.joshskills.ui.voip.new_arch.ui.utils.getVoipState
import com.joshtalks.joshskills.voip.constant.INCOMING_CALL_CATEGORY
import com.joshtalks.joshskills.voip.constant.INCOMING_CALL_ID
import com.joshtalks.joshskills.voip.constant.REMOTE_USER_NAME
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.CallingRemoteService
import java.lang.reflect.Type
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

class NotificationUtils(val context: Context) {
    companion object {
        private val executor: ExecutorService by lazy {
            JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Notification")
        }

        private var notificationChannelId = "101111"
        private var notificationChannelName = NotificationChannelNames.DEFAULT.type

        @RequiresApi(Build.VERSION_CODES.N)
        private var importance = NotificationManager.IMPORTANCE_DEFAULT
    }

    fun processRemoteMessage(remoteData: RemoteMessage, channel: NotificationAnalytics.Channel) {
        if (remoteData.data.containsKey("nType")) {
            if(remoteData.data["nType"] == "CR" && (context.applicationContext as JoshApplication)?.getVoipState() != State.IDLE)
                return
            val notificationTypeToken: Type = object : TypeToken<ShortNotificationObject>() {}.type
            val shortNc: ShortNotificationObject = AppObjectController.gsonMapper.fromJson(
                AppObjectController.gsonMapper.toJson(remoteData.data),
                notificationTypeToken
            )

            FirestoreDB.getNotification {
                val nc = it.toNotificationObject(shortNc.id)
                if (remoteData.data["nType"] == "CR") {
                    nc.actionData?.let {
                        VoipAnalytics.pushIncomingCallAnalytics(it)
                    }
                }
                sendNotification(nc)
            }
        } else {
            val notificationTypeToken: Type = object : TypeToken<NotificationObject>() {}.type
            val nc: NotificationObject = AppObjectController.gsonMapper.fromJson(
                AppObjectController.gsonMapper.toJson(remoteData.data),
                notificationTypeToken
            )
            nc.contentTitle = remoteData.notification?.title ?: remoteData.data["title"]
            nc.contentText = remoteData.notification?.body ?: remoteData.data["body"]

            if (channel == NotificationAnalytics.Channel.GROUPS) {
                sendNotification(nc)
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                val isFirstTimeNotification = NotificationAnalytics().addAnalytics(
                    notificationId = nc.id.toString(),
                    mEvent = NotificationAnalytics.Action.RECEIVED,
                    channel = channel
                )
                if (isFirstTimeNotification)
                    sendNotification(nc)
            }
        }
    }

    fun sendNotification(notificationObject: NotificationObject) {
        executor.execute {
            val intent = getIntentAccordingAction(
                notificationObject,
                notificationObject.action,
                notificationObject.actionData
            )
            intent?.run {
                putExtra(HAS_NOTIFICATION, true)
                putExtra(NOTIFICATION_ID, notificationObject.id)

                val activityList =
                    if (notificationObject.action == NotificationAction.ACTION_OPEN_PAYMENT_PAGE
                        || notificationObject.action == NotificationAction.ACTION_OPEN_SPEAKING_SECTION
                        || notificationObject.action == NotificationAction.ACTION_OPEN_LESSON
                        || notificationObject.action == NotificationAction.ACTION_OPEN_CONVERSATION
                        || notificationObject.action == NotificationAction.JOIN_CONVERSATION_ROOM
                    ) {
                        val inboxIntent =
                            InboxActivity.getInboxIntent(context)
                        inboxIntent.putExtra(HAS_NOTIFICATION, true)
                        inboxIntent.putExtra(NOTIFICATION_ID, notificationObject.id)
                        arrayOf(inboxIntent, this)
                    } else {
                        arrayOf(this)
                    }

                if (notificationObject.action == NotificationAction.JOIN_CONVERSATION_ROOM) {
                    val obj = JSONObject(notificationObject.actionData)
                    val name = obj.getString("moderator_name")
                    val topic = obj.getString("topic")
                    notificationObject.contentTitle = context.getString(R.string.room_title)
                    notificationObject.contentText =
                        context.getString(R.string.convo_notification_title, name, topic)
                }

                val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()
                val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val pendingIntent = PendingIntent.getActivities(
                    context.applicationContext,
                    uniqueInt, activityList,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val style = NotificationCompat.BigTextStyle()
                style.setBigContentTitle(notificationObject.contentTitle)
                style.bigText(notificationObject.contentText)
                style.setSummaryText("")

                val notificationBuilder =
                    NotificationCompat.Builder(
                        context,
                        notificationChannelId
                    )
                        .setTicker(notificationObject.ticker)
                        .setSmallIcon(R.drawable.ic_status_bar_notification)
                        .setContentTitle(notificationObject.contentTitle)
                        .setAutoCancel(true)
                        .setSound(defaultSound)
                        .setContentText(notificationObject.contentText)
                        .setContentIntent(pendingIntent)
                        .setStyle(style)
                        .setWhen(System.currentTimeMillis())
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setColor(
                            ContextCompat.getColor(
                                context,
                                R.color.colorAccent
                            )
                        )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    notificationBuilder.priority = NotificationManager.IMPORTANCE_DEFAULT
                }

                val dismissIntent =
                    Intent(context, DismissNotifEventReceiver::class.java).apply {
                        putExtra(NOTIFICATION_ID, notificationObject.id)
                        putExtra(HAS_NOTIFICATION, true)
                    }
                val dismissPendingIntent: PendingIntent =
                    PendingIntent.getBroadcast(context, uniqueInt, dismissIntent, 0)

                notificationBuilder.setDeleteIntent(dismissPendingIntent)

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val notificationChannel = NotificationChannel(
                        notificationChannelId,
                        notificationChannelName,
                        importance
                    )
                    notificationChannel.enableLights(true)
                    notificationChannel.enableVibration(true)
                    notificationBuilder.setChannelId(notificationChannelId)
                    notificationManager.createNotificationChannel(notificationChannel)
                }
                when (notificationObject.action) {
                    NotificationAction.GROUP_CHAT_REPLY -> {
                        notificationManager.notify(10112, notificationBuilder.build())
                    }
                    NotificationAction.GROUP_CHAT_VOICE_NOTE_HEARD -> {
                        notificationManager.notify(10122, notificationBuilder.build())
                    }
                    NotificationAction.GROUP_CHAT_PIN_MESSAGE -> {
                        notificationManager.notify(10132, notificationBuilder.build())
                    }
                    else -> {
                        notificationManager.notify(uniqueInt, notificationBuilder.build())
                    }
                }
            }
        }
    }

    private fun getIntentAccordingAction(
        notificationObject: NotificationObject,
        action: NotificationAction?,
        actionData: String?
    ): Intent? {

        return when (action) {
            NotificationAction.ACTION_OPEN_TEST -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    importance = NotificationManager.IMPORTANCE_HIGH
                }
                notificationChannelId = NotificationAction.ACTION_OPEN_TEST.type
                CourseDetailsActivity.getIntent(
                    context.applicationContext,
                    actionData!!.toInt(),
                    "Notification",
                    arrayOf(Intent.FLAG_ACTIVITY_CLEAR_TOP, Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
            }
            NotificationAction.ACTION_OPEN_CONVERSATION,
            NotificationAction.ACTION_OPEN_COURSE_REPORT,
            NotificationAction.ACTION_OPEN_QUESTION -> {
                if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
                    return null
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    importance = NotificationManager.IMPORTANCE_HIGH
                }
                notificationChannelId = action.name
                return processChatTypeNotification(notificationObject, action, actionData)
            }
            NotificationAction.ACTION_OPEN_LESSON -> {
                if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
                    return null
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    importance = NotificationManager.IMPORTANCE_HIGH
                }
                notificationChannelId = action.name
                return processOpenLessonNotification(notificationObject, action, actionData)
            }
            NotificationAction.ACTION_OPEN_SPEAKING_SECTION -> {
                if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
                    return null
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    importance = NotificationManager.IMPORTANCE_HIGH
                }
                notificationChannelId = action.name
                return processOpenSpeakingSectionNotification(
                    notificationObject,
                    action,
                    actionData
                )
            }
            NotificationAction.ACTION_OPEN_PAYMENT_PAGE -> {
                if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
                    return null
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    importance = NotificationManager.IMPORTANCE_HIGH
                }
                notificationChannelId = action.name
                return processOpenPaymentNotification(notificationObject, action, actionData)
            }
            NotificationAction.ACTION_OPEN_COURSE_EXPLORER -> {
                notificationChannelId = NotificationAction.ACTION_OPEN_COURSE_EXPLORER.name
                return Intent(context, CourseExploreActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
            }
            NotificationAction.ACTION_OPEN_URL -> {
                notificationChannelId = NotificationAction.ACTION_OPEN_URL.name
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (actionData!!.trim().startsWith("http://").not()) {
                    intent.data = Uri.parse("http://" + actionData.replace("https://", "").trim())
                } else {
                    intent.data = Uri.parse(actionData.trim())
                }
                return intent
            }
            NotificationAction.ACTION_OPEN_CONVERSATION_LIST -> {
                if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
                    return null
                }
                notificationChannelId = NotificationAction.ACTION_OPEN_CONVERSATION_LIST.name
                Intent(context, InboxActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(HAS_NOTIFICATION, true)
                    putExtra(NOTIFICATION_ID, notificationObject.id)
                }
            }
            NotificationAction.ACTION_UP_SELLING_POPUP -> {
                if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
                    return null
                }
                notificationChannelId = NotificationAction.ACTION_UP_SELLING_POPUP.name
                Intent(context, InboxActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(HAS_NOTIFICATION, true)
                    putExtra(NOTIFICATION_ID, notificationObject.id)
                    putExtra(COURSE_ID, actionData)
                    putExtra(ShareConstants.ACTION_TYPE, action)
                    putExtra(ARG_PLACEHOLDER_URL, notificationObject.bigPicture)
                    notificationObject.bigPicture?.run {
                        Glide.with(AppObjectController.joshApplication).load(this)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .submit()
                    }
                }
            }
            NotificationAction.ACTION_OPEN_REFERRAL -> {
                notificationChannelId = NotificationAction.ACTION_OPEN_REFERRAL.name
                return Intent(context, ReferralActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
            }
            /* NotificationAction.ACTION_OPEN_QUESTION -> {

                 if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
                     return null
                 }
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                     importance = NotificationManager.IMPORTANCE_HIGH
                 }
                 notificationChannelId = action.name
                 return processQuestionTypeNotification(notificationObject, action, actionData)

             }*/
            NotificationAction.ACTION_DELETE_DATA -> {
                if (User.getInstance().isVerified) {
                    Mentor.deleteUserData()
                }
                return null
            }
            NotificationAction.ACTION_DELETE_CONVERSATION_DATA -> {
                actionData?.let {
                    println("action = $action")
                    println("actionData = $actionData")
                    deleteConversationData(it)
                }
                return null
            }
            NotificationAction.ACTION_DELETE_USER -> {
                if (User.getInstance().isVerified) {
                    Mentor.deleteUserCredentials()
                }
                return null
            }
            NotificationAction.ACTION_DELETE_USER_AND_DATA -> {
                if (Mentor.getInstance().hasId() && User.getInstance().isVerified) {
                    Mentor.deleteUserCredentials()
                    Mentor.deleteUserData()
                }
                return null
            }
            NotificationAction.ACTION_LOGOUT_USER -> {
                if (Mentor.getInstance().hasId() && User.getInstance().isVerified) {
                    Mentor.deleteUserCredentials(true)
                    Mentor.deleteUserData()
                }
                return null
            }
            NotificationAction.ACTION_OPEN_REMINDER -> {
                if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
                    return null
                }
                notificationChannelId = NotificationAction.ACTION_OPEN_REFERRAL.name
                return Intent(context, ReminderListActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
            }
            NotificationAction.INCOMING_CALL_NOTIFICATION -> {
                if (!PrefManager.getBoolValue(
                        PREF_IS_CONVERSATION_ROOM_ACTIVE
                    ) && !PrefManager.getBoolValue(USER_ACTIVE_IN_GAME)
                ) {
                    incomingCallNotificationAction(notificationObject.actionData)
                }
                return null
            }
            NotificationAction.JOIN_CONVERSATION_ROOM -> {
                if (!PrefManager.getBoolValue(PREF_IS_CONVERSATION_ROOM_ACTIVE) && User.getInstance().isVerified
                    && PrefManager.getBoolValue(IS_CONVERSATION_ROOM_ACTIVE_FOR_USER)
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val intent = Intent(context, HeadsUpNotificationService::class.java).apply {
                            putExtra(ConfigKey.ROOM_DATA, actionData)
                        }
                        intent.startServiceForWebrtc()
                    } else {
                        val roomId = JSONObject(actionData).getString("room_id")
                        val topic = JSONObject(actionData).getString("topic") ?: EMPTY

                        if (roomId.isNotBlank()) {
                            return ConversationLiveRoomActivity.getIntentForNotification(
                                AppObjectController.joshApplication,
                                roomId, topicName = topic
                            )
                        } else return null
                    }
                }
                return null
            }
            NotificationAction.CALL_DISCONNECT_NOTIFICATION -> {
                callDisconnectNotificationAction()
                return null
            }
            NotificationAction.CALL_FORCE_CONNECT_NOTIFICATION -> {
                callForceConnect(notificationObject.actionData)
                return null
            }
            NotificationAction.CALL_FORCE_DISCONNECT_NOTIFICATION -> {
                callForceDisconnect()
                return null
            }
            NotificationAction.CALL_DECLINE_NOTIFICATION -> {
                callDeclineDisconnect()
                return null
            }
            NotificationAction.CALL_NO_USER_FOUND_NOTIFICATION -> {
                WebRtcService.noUserFoundCallDisconnect()
                return null
            }
            NotificationAction.CALL_ON_HOLD_NOTIFICATION -> {
                WebRtcService.holdCall()
                return null
            }
            NotificationAction.CALL_RESUME_NOTIFICATION -> {
                WebRtcService.resumeCall()
                return null
            }
            NotificationAction.CALL_CONNECTED_NOTIFICATION -> {
                if (notificationObject.actionData != null) {
                    try {
                        val obj = JSONObject(notificationObject.actionData!!)
                        WebRtcService.userJoined(obj.getInt(OPPOSITE_USER_UID))
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
                return null
            }
            NotificationAction.AUDIO_FEEDBACK_REPORT -> {
                // deleteUserCredentials()
                // deleteUserData()
                return null
            }
            NotificationAction.AWARD_DECLARE -> {
                Intent(context, LeaderBoardViewPagerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(HAS_NOTIFICATION, true)
                    putExtra(NOTIFICATION_ID, notificationObject.id)
                }
                return null
            }
            NotificationAction.ACTION_OPEN_FREE_TRIAL_SCREEN -> {
                Intent(
                    AppObjectController.joshApplication,
                    FreeTrialOnBoardActivity::class.java
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(HAS_NOTIFICATION, true)
                    putExtra(NOTIFICATION_ID, notificationObject.id)
                }
            }
            NotificationAction.ACTION_OPEN_GROUPS -> {
                return Intent(context, JoshGroupActivity::class.java).apply {
                    putExtra(CONVERSATION_ID, actionData)
                }
            }
            NotificationAction.ACTION_OPEN_FPP_REQUESTS -> {
                return Intent(context, SeeAllRequestsActivity::class.java)
            }
            NotificationAction.ACTION_OPEN_FPP_LIST -> {
                return Intent(context, FavoriteListActivity::class.java).apply {
                    putExtra(CONVERSATION_ID, actionData)
                    putExtra(IS_COURSE_BOUGHT, true)
                }
            }
            NotificationAction.ACTION_OPEN_GROUP_CHAT_CLIENT -> {
                if (actionData.equals(Mentor.getInstance().getId()))
                    return null
                return Intent(context, JoshGroupActivity::class.java).apply {
                    putExtra(CONVERSATION_ID, actionData)
                }
            }
            NotificationAction.EMERGENCY_NOTIFICATION -> {
                lateinit var intent: Intent
                if (isValidFullNumber("+91", actionData)) {
                     intent = Intent(Intent.ACTION_DIAL).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    intent.data = Uri.parse("tel:$actionData")
                } else {
                    intent = Intent(context, LauncherActivity::class.java)
                }
                return intent
            }
            NotificationAction.ACTION_P2P_INCOMING_CALL -> {
                val remoteServiceIntent = Intent(com.joshtalks.joshskills.voip.Utils.context, CallingRemoteService::class.java)
                remoteServiceIntent.putExtra(INCOMING_CALL_ID, JSONObject(actionData).getString(INCOMING_CALL_ID))
                remoteServiceIntent.putExtra(INCOMING_CALL_CATEGORY,JSONObject(actionData).getString(INCOMING_CALL_CATEGORY))
                remoteServiceIntent.action = SERVICE_ACTION_INCOMING_CALL
                com.joshtalks.joshskills.voip.Utils.context?.startService(remoteServiceIntent)
                return null
            }
            NotificationAction.ACTION_FPP_INCOMING_CALL -> {
                try {
                    val jsonObj = JSONObject(actionData ?: EMPTY)
                    val callContext = context
                    val remoteServiceIntent = Intent(callContext, CallingRemoteService::class.java)
                    remoteServiceIntent.putExtra(INCOMING_CALL_ID, jsonObj.getString(INCOMING_CALL_ID))
                    Log.d("smsms", "naman: 2   ${JSONObject(actionData).getString(INCOMING_CALL_ID)}")
                    remoteServiceIntent.putExtra(INCOMING_CALL_CATEGORY, jsonObj.getString(INCOMING_CALL_CATEGORY))
                    remoteServiceIntent.putExtra(REMOTE_USER_NAME, jsonObj.getString(REMOTE_USER_NAME))
                    remoteServiceIntent.action = SERVICE_ACTION_INCOMING_CALL
                    callContext.startService(remoteServiceIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return null
            }
            NotificationAction.ACTION_GROUP_INCOMING_CALL -> {
                return null
            }
            else -> {
                return null
            }
        }
    }

    private fun deleteConversationData(courseId: String) {
        try {
            AppObjectController.appDatabase.run {
                val conversationId = this.courseDao().getConversationIdFromCourseId(courseId)
                conversationId?.let {
                    PrefManager.removeKey(it)
                    LastSyncPrefManager.removeKey(it)
                }
                val lessons = lessonDao().getLessonIdsForCourse(courseId.toInt())
                lessons.forEach {
                    LastSyncPrefManager.removeKey(it.toString())
                }
                commonDao().deleteConversationData(courseId.toInt())
            }
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    private fun isNotificationCrash(): Class<*> {
        val isNotificationCrash =
            false
        return if (isNotificationCrash) {
            InboxActivity::class.java
        } else {
            ConversationActivity::class.java
        }
    }

    private fun isOpenPaymentNotificationCrash(): Class<*> {
        val isNotificationCrash =
            false
        return if (isNotificationCrash) {
            InboxActivity::class.java
        } else {
            FreeTrialPaymentActivity::class.java
        }
    }

    private fun isOpenLessonNotificationCrash(): Class<*> {
        val isNotificationCrash =
            false
        return if (isNotificationCrash) {
            InboxActivity::class.java
        } else {
            LessonActivity::class.java
        }
    }

    private fun isOpenSpeakingSectionNotificationCrash(): Class<*> {
        val isNotificationCrash =
            false
        return if (isNotificationCrash) {
            InboxActivity::class.java
        } else {
            LessonActivity::class.java
        }
    }

    private fun processQuestionTypeNotification(
        notificationObject: NotificationObject?,
        action: NotificationAction?,
        actionData: String?
    ): Intent? {

        notificationChannelId = action?.name ?: EMPTY
        var questionId = ""

        notificationObject?.extraData?.let {
            val mapTypeToken: Type = object : TypeToken<Map<String, String>>() {}.type
            val map: Map<String, String> = Gson().fromJson(it, mapTypeToken)
            questionId = map["question_id"] ?: EMPTY
        }
        val question: Question? =
            AppObjectController.appDatabase.chatDao().getQuestionOnIdV2(questionId)

        return if (question == null) {
            processChatTypeNotification(notificationObject, action, actionData)
        } else {
            when {
                question.type == BASE_MESSAGE_TYPE.QUIZ || question.type == BASE_MESSAGE_TYPE.TEST -> {
                    return Intent(context.applicationContext, AssessmentActivity::class.java).apply {
                        putExtra(AssessmentActivity.KEY_ASSESSMENT_ID, question.assessmentId)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                question.type == BASE_MESSAGE_TYPE.CP -> {
                    return Intent(
                        context.applicationContext,
                        ConversationPracticeActivity::class.java
                    ).apply {
                        putExtra(PRACTISE_ID, question.conversationPracticeId)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }

                question.type == BASE_MESSAGE_TYPE.PR || question.material_type == BASE_MESSAGE_TYPE.VI -> {
                    return processChatTypeNotification(notificationObject, action, actionData)
                }
                else -> {
                    return null
                }
            }
        }
    }

    private fun processChatTypeNotification(
        notificationObject: NotificationObject?,
        action: NotificationAction?,
        actionData: String?
    ): Intent? {
        val obj: InboxEntity = AppObjectController.appDatabase.courseDao()
            .chooseRegisterCourseMinimal(actionData!!) ?: return null

        val rIntnet = Intent(context.applicationContext, isNotificationCrash()).apply {
            putExtra(UPDATED_CHAT_ROOM_OBJECT, obj)
            putExtra(ShareConstants.ACTION_TYPE, action)
            putExtra(HAS_NOTIFICATION, true)
            putExtra(NOTIFICATION_ID, notificationObject?.id)
            putExtra(QUESTION_ID, actionData)
            // addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (NotificationAction.ACTION_OPEN_COURSE_REPORT == action) {
            rIntnet.putExtra(HAS_COURSE_REPORT, true)
        }
        notificationObject?.extraData?.let {
            val mapTypeToken: Type = object : TypeToken<Map<String, String>>() {}.type
            val map: Map<String, String> = Gson().fromJson(it, mapTypeToken)
            if (map.containsKey("question_id")) {
                rIntnet.putExtra(QUESTION_ID, map["question_id"] ?: EMPTY)
            }
        }
        return rIntnet
    }

    private fun processOpenLessonNotification(
        notificationObject: NotificationObject?,
        action: NotificationAction?,
        actionData: String?
    ): Intent {

        val rIntent = Intent(context.applicationContext, isOpenLessonNotificationCrash()).apply {
            val obj = JSONObject(actionData)
            Timber.d("ghjk12 : actionData -> $obj")
            val lessonId = obj.getInt(LessonActivity.LESSON_ID)
            Timber.d("ghjk12 : NotifLessonId -> $lessonId")
            putExtra(LessonActivity.LESSON_ID, lessonId)
            putExtra(LessonActivity.IS_DEMO, false)
            putExtra(LessonActivity.IS_NEW_GRAMMAR, obj.getBoolean(LessonActivity.IS_NEW_GRAMMAR))
            putExtra(
                LessonActivity.IS_LESSON_COMPLETED,
                obj.getBoolean(LessonActivity.IS_LESSON_COMPLETED)
            )
            putExtra(CONVERSATION_ID, obj.getString(CONVERSATION_ID))
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(ShareConstants.ACTION_TYPE, action)
            putExtra(HAS_NOTIFICATION, true)
            putExtra(NOTIFICATION_ID, notificationObject?.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
//        notificationObject?.extraData?.let {
//            val mapTypeToken: Type = object : TypeToken<Map<String, String>>() {}.type
//            val map: Map<String, String> = Gson().fromJson(it, mapTypeToken)
//            if (map.containsKey(LessonActivity.LESSON_ID)) {
//                rIntnet.putExtra(LessonActivity.LESSON_ID, map[LessonActivity.LESSON_ID] ?: EMPTY)
//            }
//        }
        return rIntent
    }

    private fun processOpenSpeakingSectionNotification(
        notificationObject: NotificationObject?,
        action: NotificationAction?,
        actionData: String?
    ): Intent {

        val rIntent = Intent(context.applicationContext, isOpenLessonNotificationCrash()).apply {
            val obj = JSONObject(actionData)
            putExtra(LessonActivity.LESSON_ID, obj.getInt(LessonActivity.LESSON_ID))
            putExtra(LessonActivity.IS_DEMO, false)
            putExtra(LessonActivity.IS_NEW_GRAMMAR, obj.getBoolean(LessonActivity.IS_NEW_GRAMMAR))
            putExtra(
                LessonActivity.IS_LESSON_COMPLETED,
                obj.getBoolean(LessonActivity.IS_LESSON_COMPLETED)
            )
            putExtra(CONVERSATION_ID, obj.getString(CONVERSATION_ID))
            putExtra(LessonActivity.LESSON_SECTION, SPEAKING_POSITION)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(ShareConstants.ACTION_TYPE, action)
            putExtra(HAS_NOTIFICATION, true)
            putExtra(NOTIFICATION_ID, notificationObject?.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
//        notificationObject?.extraData?.let {
//            val mapTypeToken: Type = object : TypeToken<Map<String, String>>() {}.type
//            val map: Map<String, String> = Gson().fromJson(it, mapTypeToken)
//            if (map.containsKey(LessonActivity.LESSON_ID)) {
//                rIntnet.putExtra(LessonActivity.LESSON_ID, map[LessonActivity.LESSON_ID] ?: EMPTY)
//            }
//        }
        return rIntent
    }

    private fun processOpenPaymentNotification(
        notificationObject: NotificationObject?,
        action: NotificationAction?,
        actionData: String?
    ): Intent {

        val rIntent = Intent(context.applicationContext, isOpenPaymentNotificationCrash()).apply {
            putExtra(PaymentSummaryActivity.TEST_ID_PAYMENT, actionData)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ShareConstants.ACTION_TYPE, action)
            putExtra(HAS_NOTIFICATION, true)
            putExtra(NOTIFICATION_ID, notificationObject?.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
//        notificationObject?.extraData?.let {
//            val mapTypeToken: Type = object : TypeToken<Map<String, String>>() {}.type
//            val map: Map<String, Int> = Gson().fromJson(it, mapTypeToken)
//            if (map.containsKey(PaymentSummaryActivity.TEST_ID_PAYMENT)) {
//                rIntent.putExtra(PaymentSummaryActivity.TEST_ID_PAYMENT, map[PaymentSummaryActivity.TEST_ID_PAYMENT])
//            }
//        }
        return rIntent
    }

    private fun callForceDisconnect() {
        WebRtcService.forceDisconnect()
    }

    private fun callDeclineDisconnect() {
        WebRtcService.declineDisconnect()
    }

    private fun callForceConnect(actionData: String?) {
        actionData?.let {
            try {
                val obj = JSONObject(it)
                val data = HashMap<String, String>()
                data[RTC_TOKEN_KEY] = obj.getString("token")
                data[RTC_CHANNEL_KEY] = obj.getString("channel_name")
                data[RTC_UID_KEY] = obj.getString("uid")
                data[RTC_CALLER_UID_KEY] = obj.getString("caller_uid")
                try {
                    data[RTC_CALL_ID] = obj.getString("agoraCallId")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (obj.has("group_name"))
                    data[RTC_WEB_GROUP_CALL_GROUP_NAME] = obj.getString("group_name")

                if (obj.has("is_group_call"))
                    data[RTC_IS_GROUP_CALL] = obj.getString("is_group_call")

                if (obj.has("group_url"))
                    data[RTC_WEB_GROUP_PHOTO] = obj.getString("group_url")

                WebRtcService.currentCallingGroupName =
                    data[RTC_WEB_GROUP_CALL_GROUP_NAME] ?: ""
                WebRtcService.forceConnect(data)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    private fun callDisconnectNotificationAction() {
        WebRtcService.disconnectCallFromCallie()
    }

    private fun incomingCallNotificationAction(actionData: String?) {
        actionData?.let {
            try {
                val obj = JSONObject(it)
                val data = HashMap<String, String?>()
                data[RTC_TOKEN_KEY] = obj.getString("token")
                data[RTC_CHANNEL_KEY] = obj.getString("channel_name")
                data[RTC_UID_KEY] = obj.getString("uid")
                data[RTC_CALLER_UID_KEY] = obj.getString("caller_uid")
                try {
                    data[RTC_CALL_ID] = obj.getString("agoraCallId")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (obj.has("group_name"))
                    data[RTC_WEB_GROUP_CALL_GROUP_NAME] = obj.getString("group_name")

                if (obj.has("is_group_call"))
                    data[RTC_IS_GROUP_CALL] = obj.getString("is_group_call")

                if (obj.has("group_url"))
                    data[RTC_WEB_GROUP_PHOTO] = obj.getString("group_url")

                if (obj.has("f")) {
                    val id = obj.getInt("caller_uid")
                    val caller =
                        AppObjectController.appDatabase.favoriteCallerDao()
                            .getFavoriteCaller(id)
                    Thread.sleep(25)
                    if (caller != null) {
                        data[RTC_NAME] = caller.name
                        data[RTC_CALLER_PHOTO] = caller.image
                        data[RTC_IS_FAVORITE] = "true"
                    }
                }
                WebRtcService.startOnNotificationIncomingCall(data)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

}