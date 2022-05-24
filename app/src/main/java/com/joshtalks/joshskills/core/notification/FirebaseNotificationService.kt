package com.joshtalks.joshskills.core.notification

import android.app.*
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.clevertap.android.sdk.CleverTapAPI
import com.facebook.share.internal.ShareConstants.ACTION_TYPE
import com.freshchat.consumer.sdk.Freshchat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.liveRooms.ConversationLiveRoomActivity
import com.joshtalks.joshskills.core.API_TOKEN
import com.joshtalks.joshskills.core.ARG_PLACEHOLDER_URL
import com.joshtalks.joshskills.core.ApiRespStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.COURSE_ID
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.IS_CONVERSATION_ROOM_ACTIVE_FOR_USER
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.core.ONBOARDING_STAGE
import com.joshtalks.joshskills.core.OnBoardingStage
import com.joshtalks.joshskills.core.PREF_IS_CONVERSATION_ROOM_ACTIVE
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_ACTIVE_IN_GAME
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.DismissNotifEventReceiver
import com.joshtalks.joshskills.core.firestore.FirestoreDB
import com.joshtalks.joshskills.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.core.io.LastSyncPrefManager
import com.joshtalks.joshskills.core.startServiceForWebrtc
import com.joshtalks.joshskills.core.textDrawableBitmap
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.FCMResponse
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
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.leaderboard.LeaderBoardViewPagerActivity
import com.joshtalks.joshskills.ui.lesson.LessonActivity
import com.joshtalks.joshskills.ui.lesson.SPEAKING_POSITION
import com.joshtalks.joshskills.ui.payment.FreeTrialPaymentActivity
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.reminder.reminder_listing.ReminderListActivity
import com.joshtalks.joshskills.ui.signup.FreeTrialOnBoardActivity
import com.joshtalks.joshskills.ui.signup.SHOW_SIGN_UP_FRAGMENT
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
import com.joshtalks.joshskills.ui.voip.analytics.VoipAnalytics.pushIncomingCallAnalytics
import com.joshtalks.joshskills.ui.voip.new_arch.ui.utils.getVoipState
import com.joshtalks.joshskills.voip.constant.State
import com.moengage.firebase.MoEFireBaseHelper
import com.moengage.pushbase.MoEPushHelper
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import kotlin.collections.set
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

const val FCM_TOKEN = "fcmToken"
const val HAS_NOTIFICATION = "has_notification"
const val HAS_LOCAL_NOTIFICATION = "has_local_notification"
const val NOTIFICATION_ID = "notification_id"
const val HAS_COURSE_REPORT = "has_course_report"
const val QUESTION_ID = "question_id"

const val FCM_ACTIVE = "FCM_ACTIVE"
const val FCM_INACTIVE = "FCM_INACTIVE"
const val FCM_REFRESH = "FCM_REFRESH"
const val FCM_NOT_PRESENT = "FCM_NOT_PRESENT"

class FirebaseNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.tag(FirebaseNotificationService::class.java.name).e(token)
        try {
            if (PrefManager.hasKey(FCM_TOKEN)) {
                val fcmResponse = FCMResponse.getInstance()
                fcmResponse?.apiStatus = ApiRespStatus.POST
                fcmResponse?.update()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        PrefManager.put(FCM_TOKEN, token)
        MoEFireBaseHelper.getInstance().passPushToken(applicationContext, token)
        CleverTapAPI.getDefaultInstance(this)?.pushFcmRegistrationId(token, true)
        if (AppObjectController.freshChat != null) {
            AppObjectController.freshChat?.setPushRegistrationToken(token)
        }
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO +
                    CoroutineExceptionHandler { _, _ -> /* Do Nothing */ }
        ).launch {
            val userId = Mentor.getInstance().getId()
            if (userId.isNotBlank()) {
                try {
                    val data = mutableMapOf(
                        "user_id" to userId,
                        "registration_id" to token,
                        "name" to Utils.getDeviceName(),
                        "device_id" to Utils.getDeviceId(),
                        "active" to "true",
                        "type" to "android",
                        "gaid" to PrefManager.getStringValue(USER_UNIQUE_ID),
                        "newToken" to "true"
                    )
                    val resp = AppObjectController.signUpNetworkService.postFCMToken(data.toMap())
                    if (resp.isSuccessful) {
                        resp.body()?.update()
                    }
                } catch (ex: Exception) {
                    try {
                        AppAnalytics.create(AnalyticsEvent.FCM_TOKEN_CRASH_EVENT.NAME)
                            .addBasicParam()
                            .addUserDetails()
                            .push()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                    ex.printStackTrace()
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.tag(FirebaseNotificationService::class.java.name)
            .e("fcm onMessageReceived data: ${remoteMessage.data}  remote body: ${remoteMessage.notification?.body}  title : ${remoteMessage.notification?.title}")

        try {
            if (Freshchat.isFreshchatNotification(remoteMessage))
                Freshchat.handleFcmMessage(this, remoteMessage)
            else if (MoEPushHelper.getInstance().isFromMoEngagePlatform(remoteMessage.data) && JSONObject(remoteMessage.data["gcm_alert"]).has("isCustom")) {
                val dataJson = JSONObject(remoteMessage.data["gcm_alert"])
                remoteMessage.data["title"] = dataJson["title"].toString()
                remoteMessage.data["body"] = dataJson["body"].toString()
                remoteMessage.data["action"] = dataJson["client_action"].toString()
                remoteMessage.data["action_data"] = dataJson["action_data"].toString()
                remoteMessage.data["notification_id"] = dataJson["notification_id"].toString()
                processRemoteMessage(remoteMessage, NotificationAnalytics.Channel.MOENGAGE)
                MoEPushHelper.getInstance().logNotificationReceived(this, remoteMessage.data)
                return
            } else if (MoEPushHelper.getInstance().isFromMoEngagePlatform(remoteMessage.data)) {
                MoEFireBaseHelper.getInstance().passPushPayload(applicationContext, remoteMessage.data)
                return
            } else {
                processRemoteMessage(remoteMessage, NotificationAnalytics.Channel.FCM)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun processRemoteMessage(remoteData: RemoteMessage, channel: NotificationAnalytics.Channel) {
        if (remoteData.data.containsKey("nType")) {
            if(remoteData.data["nType"] == "CR" && application.getVoipState() != State.IDLE)
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
                        pushIncomingCallAnalytics(it)
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
            nc.id = nc.notificationId.toString()
            CoroutineScope(Dispatchers.IO).launch {
                val isFirstTimeNotification = NotificationAnalytics().addAnalytics(
                    notificationId = nc.notificationId.toString(),
                    mEvent = NotificationAnalytics.Action.RECEIVED,
                    channel = channel
                )
                if (isFirstTimeNotification) {
                    sendNotification(nc)
//                    pushToDatabase(nc, channel = channel)
                }
            }
        }
    }

    private fun sendNotification(notificationObject: NotificationObject) {
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
                            InboxActivity.getInboxIntent(this@FirebaseNotificationService)
                        inboxIntent.putExtra(HAS_NOTIFICATION, true)
                        inboxIntent.putExtra(NOTIFICATION_ID, notificationObject.id)
                        arrayOf(inboxIntent, this)
                    } else {
                        arrayOf(this)
                    }

                /*   val activityList = if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
                       arrayOf(this)
                   } else {
                       *//* if (isAppRunning().not()) {
                         arrayOf(this)
                     } else {*//*
                    val backIntent =
                        Intent(
                            this@FirebaseNotificationService,
                            InboxActivity::class.java
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    arrayOf( this)
                    //  }
                }*/

                if (notificationObject.action == NotificationAction.JOIN_CONVERSATION_ROOM) {
                    val obj = JSONObject(notificationObject.actionData)
                    val name = obj.getString("moderator_name")
                    val topic = obj.getString("topic")
                    notificationObject.contentTitle = getString(R.string.room_title)
                    notificationObject.contentText =
                        getString(R.string.convo_notification_title, name, topic)
                }

                val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()
                val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val pendingIntent = PendingIntent.getActivities(
                    applicationContext,
                    uniqueInt, activityList,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val style = NotificationCompat.BigTextStyle()
                style.setBigContentTitle(notificationObject.contentTitle)
                style.bigText(notificationObject.contentText)
                style.setSummaryText("")

                val notificationBuilder =
                    NotificationCompat.Builder(
                        this@FirebaseNotificationService,
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
                                this@FirebaseNotificationService,
                                R.color.colorAccent
                            )
                        )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    notificationBuilder.priority = NotificationManager.IMPORTANCE_DEFAULT
                }

                val dismissIntent =
                    Intent(applicationContext, DismissNotifEventReceiver::class.java).apply {
                        putExtra(NOTIFICATION_ID, notificationObject.id)
                        putExtra(HAS_NOTIFICATION, true)
                    }
                val dismissPendingIntent: PendingIntent =
                    PendingIntent.getBroadcast(applicationContext, uniqueInt, dismissIntent, 0)

                notificationBuilder.setDeleteIntent(dismissPendingIntent)

                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
                    applicationContext,
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
                return Intent(applicationContext, CourseExploreActivity::class.java).apply {
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
                Intent(applicationContext, InboxActivity::class.java).apply {
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
                Intent(applicationContext, InboxActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(HAS_NOTIFICATION, true)
                    putExtra(NOTIFICATION_ID, notificationObject.id)
                    putExtra(COURSE_ID, actionData)
                    putExtra(ACTION_TYPE, action)
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
                return Intent(applicationContext, ReferralActivity::class.java).apply {
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
                    println("action = ${action}")
                    println("actionData = ${actionData}")
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
                return Intent(applicationContext, ReminderListActivity::class.java).apply {
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
                        val intent = Intent(this, HeadsUpNotificationService::class.java).apply {
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
                //if (User.getInstance().isVerified) {
                callForceConnect(notificationObject.actionData)
                //}
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
                Intent(applicationContext, LeaderBoardViewPagerActivity::class.java).apply {
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
            else -> {
                return null
            }
        }
    }
/*

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

                WebRtcService.currentCallingGroupName = data[RTC_WEB_GROUP_CALL_GROUP_NAME] ?: ""
                WebRtcService.forceConnect(data)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    private fun callForceDisconnect() {
        WebRtcService.forceDisconnect()
    }

    private fun callDisconnectNotificationAction() {
        WebRtcService.disconnectCallFromCallie()
    }
*/

    private fun declineCallWhenInConversationRoom(actionData: String?) {
        actionData?.let {
            try {
                val obj = JSONObject(it)
                val data = HashMap<String, String?>()
                data[RTC_CHANNEL_KEY] = obj.getString("channel_name")
                data[RTC_UID_KEY] = obj.getString("uid")
                data["call_response"] = "DECLINE"

                WebRtcService.rejectCall()
                CoroutineScope(Dispatchers.IO).launch {
                    AppObjectController.p2pNetworkService.getAgoraCallResponse(data)
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }

    }
/*
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
                        AppObjectController.appDatabase.favoriteCallerDao().getFavoriteCaller(id)
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
    }*/

//    private fun returnDefaultIntent(): Intent {
//        return Intent(applicationContext, LauncherActivity::class.java).apply {
//            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//            putExtra(HAS_NOTIFICATION, true)
//        }
//    }

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
            AppObjectController.getFirebaseRemoteConfig().getBoolean("IS_NOTIFICATION_CRASH")
        return if (isNotificationCrash) {
            InboxActivity::class.java
        } else {
            ConversationActivity::class.java
        }
    }

    private fun isOpenPaymentNotificationCrash(): Class<*> {
        val isNotificationCrash =
            AppObjectController.getFirebaseRemoteConfig()
                .getBoolean("IS_OPEN_PAYMENT_PAGE_NOTIFICATION_CRASH")
        return if (isNotificationCrash) {
            InboxActivity::class.java
        } else {
            FreeTrialPaymentActivity::class.java
        }
    }

    private fun isOpenLessonNotificationCrash(): Class<*> {
        val isNotificationCrash =
            AppObjectController.getFirebaseRemoteConfig()
                .getBoolean("IS_OPEN_LESSON_NOTIFICATION_CRASH")
        return if (isNotificationCrash) {
            InboxActivity::class.java
        } else {
            LessonActivity::class.java
        }
    }

    private fun isOpenSpeakingSectionNotificationCrash(): Class<*> {
        val isNotificationCrash =
            AppObjectController.getFirebaseRemoteConfig()
                .getBoolean("IS_OPEN_SPEAKING_SECTION_NOTIFICATION_CRASH")
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
                    return Intent(applicationContext, AssessmentActivity::class.java).apply {
                        putExtra(AssessmentActivity.KEY_ASSESSMENT_ID, question.assessmentId)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                question.type == BASE_MESSAGE_TYPE.CP -> {
                    return Intent(
                        applicationContext,
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
        /*
        JobScheduler 100 job limit exceeded issue
        obj?.run {
            WorkManagerAdmin.updatedCourseForConversation(this.conversation_id)
        }*/

        val rIntnet = Intent(applicationContext, isNotificationCrash()).apply {
            putExtra(UPDATED_CHAT_ROOM_OBJECT, obj)
            putExtra(ACTION_TYPE, action)
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

        val rIntent = Intent(applicationContext, isOpenLessonNotificationCrash()).apply {
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
            putExtra(ACTION_TYPE, action)
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

        val rIntent = Intent(applicationContext, isOpenLessonNotificationCrash()).apply {
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
            putExtra(ACTION_TYPE, action)
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

        val rIntent = Intent(applicationContext, isOpenPaymentNotificationCrash()).apply {
            putExtra(PaymentSummaryActivity.TEST_ID_PAYMENT, actionData)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ACTION_TYPE, action)
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

    private fun isAppRunning(): Boolean {
        try {
            val activityManager: ActivityManager? =
                applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            if (activityManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Timber.tag(FirebaseNotificationService::class.java.simpleName)
                        .e(activityManager.appTasks[0].taskInfo.topActivity?.className)
                }
                //  activityManager.appTasks[0].taskInfo.topActivity?.className== InboxActivity::class.java.name)){
                return true
            }
        } catch (ex: Exception) {
        }
        return false
    }

    private fun getBitmapFromURL(strURL: String?): Bitmap? {
        return if (strURL != null) {
            try {
                val url = URL(strURL)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input: InputStream = connection.inputStream
                BitmapFactory.decodeStream(input)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    private fun getCroppedBitmap(bitmap: Bitmap): Bitmap? {
        val output = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(
            bitmap.width / 2f, bitmap.height / 2f,
            bitmap.height / 2f, paint
        )
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        // Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
        // return _bmp;
        return output
    }

    private fun getNameInitialBitmap(name: String, bgColorCode: String?): Bitmap {

        val nameSplitArray = name.split(" ".toRegex()).toTypedArray()
        val text = if (nameSplitArray.size > 1) {
            nameSplitArray[0].substring(0, 1) + nameSplitArray[1].substring(0, 1)
        } else {
            name.substring(0, 1)
        }

        val color = if (bgColorCode == null) {
            ContextCompat.getColor(this, R.color.colorPrimary)
        } else {
            Color.parseColor(bgColorCode)
        }
        return text.textDrawableBitmap(bgColor = color)!!
    }

    fun pushToDatabase(
        nc: NotificationObject,
        channel: NotificationAnalytics.Channel
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            NotificationAnalytics().addAnalytics(
                notificationId = nc.notificationId.toString(),
                mEvent = NotificationAnalytics.Action.RECEIVED,
                channel = channel
            )
        }
    }

    public fun sendFirestoreNotificationNew(nc: NotificationObject) {
        sendNotification(nc)
    }

    companion object {
        private val executor: ExecutorService by lazy {
            JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Notification")
        }

        private var notificationChannelId = "101111"
        private var notificationChannelName = NotificationChannelNames.DEFAULT.type

        @RequiresApi(Build.VERSION_CODES.N)
        private var importance = NotificationManager.IMPORTANCE_DEFAULT

        fun sendFirestoreNotification(
            notificationObject: NotificationObject,
            context: Context
        ) {
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
                        ) {
                            val inboxIntent =
                                InboxActivity.getInboxIntent(context)
                            inboxIntent.putExtra(HAS_NOTIFICATION, true)
                            inboxIntent.putExtra(NOTIFICATION_ID, notificationObject.id)
                            arrayOf(inboxIntent, this)
                        } else {
                            arrayOf(this)
                        }
                    val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()
                    val defaultSound =
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
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
                        Intent(
                            context.applicationContext,
                            DismissNotifEventReceiver::class.java
                        ).apply {
                            putExtra(NOTIFICATION_ID, notificationObject.id)
                            putExtra(HAS_NOTIFICATION, true)
                        }
                    val dismissPendingIntent: PendingIntent =
                        PendingIntent.getBroadcast(
                            context.applicationContext,
                            uniqueInt,
                            dismissIntent,
                            0
                        )

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
                    notificationManager.notify(uniqueInt, notificationBuilder.build())
                }
            }
        }

        private fun getIntentForNotificationAction(
            notificationObject: NotificationObject,
            action: NotificationAction?,
            actionData: String?
        ): Intent? {
            return when (action) {
                NotificationAction.INCOMING_CALL_NOTIFICATION -> {
                    if (!PrefManager.getBoolValue(PREF_IS_CONVERSATION_ROOM_ACTIVE)
                        && !PrefManager.getBoolValue(USER_ACTIVE_IN_GAME)
                    ) {
                        incomingCallNotificationAction(notificationObject.actionData)
                    }
                    null
                }
                NotificationAction.CALL_DISCONNECT_NOTIFICATION -> {
                    callDisconnectNotificationAction()
                    null
                }
                NotificationAction.CALL_FORCE_CONNECT_NOTIFICATION -> {
                    //if (User.getInstance().isVerified) {
                    callForceConnect(notificationObject.actionData)
                    //}
                    null
                }
                NotificationAction.CALL_FORCE_DISCONNECT_NOTIFICATION -> {
                    callForceDisconnect()
                    null
                }
                NotificationAction.CALL_DECLINE_NOTIFICATION -> {
                    callDeclineDisconnect()
                    null
                }
                NotificationAction.CALL_NO_USER_FOUND_NOTIFICATION -> {
                    WebRtcService.noUserFoundCallDisconnect()
                    null
                }
                NotificationAction.CALL_ON_HOLD_NOTIFICATION -> {
                    WebRtcService.holdCall()
                    null
                }
                NotificationAction.CALL_RESUME_NOTIFICATION -> {
                    WebRtcService.resumeCall()
                    null
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
                    null
                }
                NotificationAction.ACTION_COMPLETE_ONBOARDING -> {
                    val onBoardingStage = PrefManager.getStringValue(ONBOARDING_STAGE)
                    if (onBoardingStage == OnBoardingStage.APP_INSTALLED.value ||
                        onBoardingStage == OnBoardingStage.START_NOW_CLICKED.value ||
                        onBoardingStage == OnBoardingStage.JI_HAAN_CLICKED.value
                    ) {
                        Intent(
                            AppObjectController.joshApplication,
                            FreeTrialOnBoardActivity::class.java
                        ).apply {
                            if (onBoardingStage == OnBoardingStage.JI_HAAN_CLICKED.value) {
                                putExtra(SHOW_SIGN_UP_FRAGMENT, true)
                            }
                        }
                    } else {
                        null
                    }
                }
                NotificationAction.JOIN_CONVERSATION_ROOM -> {

                    if (!PrefManager.getBoolValue(PREF_IS_CONVERSATION_ROOM_ACTIVE) && actionData != null
                        && User.getInstance().isVerified && PrefManager.getBoolValue(
                            IS_CONVERSATION_ROOM_ACTIVE_FOR_USER
                        )
                    ) {
                        val roomId = JSONObject(actionData).getString("room_id")
                        val topic = JSONObject(actionData).getString("topic") ?: EMPTY
                        if (roomId.isNotBlank()) {
                            ConversationLiveRoomActivity.getIntentForNotification(
                                AppObjectController.joshApplication,
                                roomId,
                                topicName = topic
                            )
                        } else null
                    } else null
                }
                else -> {
                    null
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
                        AppObjectController.joshApplication,
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
                    return null
                }
                NotificationAction.ACTION_OPEN_LESSON -> {
                    if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
                        return null
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        importance = NotificationManager.IMPORTANCE_HIGH
                    }
                    notificationChannelId = action.name
                    return null
                }
                NotificationAction.ACTION_OPEN_SPEAKING_SECTION -> {
                    if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
                        return null
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        importance = NotificationManager.IMPORTANCE_HIGH
                    }
                    notificationChannelId = action.name
                    return null
                }
                NotificationAction.ACTION_OPEN_PAYMENT_PAGE -> {
                    if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
                        return null
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        importance = NotificationManager.IMPORTANCE_HIGH
                    }
                    notificationChannelId = action.name
                    return null
                }
                NotificationAction.ACTION_OPEN_COURSE_EXPLORER -> {
                    notificationChannelId = NotificationAction.ACTION_OPEN_COURSE_EXPLORER.name
                    return Intent(AppObjectController.joshApplication, CourseExploreActivity::class.java).apply {
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
                    Intent(AppObjectController.joshApplication, InboxActivity::class.java).apply {
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
                    Intent(AppObjectController.joshApplication, InboxActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        putExtra(HAS_NOTIFICATION, true)
                        putExtra(NOTIFICATION_ID, notificationObject.id)
                        putExtra(COURSE_ID, actionData)
                        putExtra(ACTION_TYPE, action)
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
                    return Intent(AppObjectController.joshApplication, ReferralActivity::class.java).apply {
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
                        println("action = ${action}")
                        println("actionData = ${actionData}")
                        //deleteConversationData(it)
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
                    return Intent(AppObjectController.joshApplication, ReminderListActivity::class.java).apply {
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
                            val intent = Intent(AppObjectController.joshApplication, HeadsUpNotificationService::class.java).apply {
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
                    //if (User.getInstance().isVerified) {
                    callForceConnect(notificationObject.actionData)
                    //}
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
                    Intent(AppObjectController.joshApplication, LeaderBoardViewPagerActivity::class.java).apply {
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
                else -> {
                    return null
                }
            }
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
}
