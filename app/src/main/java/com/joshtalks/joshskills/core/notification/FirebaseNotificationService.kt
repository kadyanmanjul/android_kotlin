package com.joshtalks.joshskills.core.notification

// import com.cometchat.pro.constants.CometChatConstants
// import com.cometchat.pro.helpers.CometChatHelper
// import com.cometchat.pro.models.BaseMessage
// import com.cometchat.pro.models.Group
// import com.cometchat.pro.models.TextMessage
// import com.joshtalks.joshskills.ui.groupchat.constant.StringContract
// import com.joshtalks.joshskills.ui.groupchat.utils.Utils
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
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.API_TOKEN
import com.joshtalks.joshskills.core.ARG_PLACEHOLDER_URL
import com.joshtalks.joshskills.core.ApiRespStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.COURSE_ID
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.DismissNotifEventReceiver
import com.joshtalks.joshskills.core.firestore.FirestoreDB
import com.joshtalks.joshskills.core.io.LastSyncPrefManager
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
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.assessment.AssessmentActivity
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.chat.UPDATED_CHAT_ROOM_OBJECT
import com.joshtalks.joshskills.ui.conversation_practice.ConversationPracticeActivity
import com.joshtalks.joshskills.ui.conversation_practice.PRACTISE_ID
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.launch.LauncherActivity
import com.joshtalks.joshskills.ui.leaderboard.LeaderBoardViewPagerActivity
import com.joshtalks.joshskills.ui.lesson.LessonActivity
import com.joshtalks.joshskills.ui.lesson.SPEAKING_POSITION
import com.joshtalks.joshskills.ui.payment.FreeTrialPaymentActivity
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.reminder.reminder_listing.ReminderListActivity
import com.joshtalks.joshskills.ui.voip.OPPOSITE_USER_UID
import com.joshtalks.joshskills.ui.voip.RTC_CALLER_PHOTO
import com.joshtalks.joshskills.ui.voip.RTC_CALLER_UID_KEY
import com.joshtalks.joshskills.ui.voip.RTC_CALL_ID
import com.joshtalks.joshskills.ui.voip.RTC_CHANNEL_KEY
import com.joshtalks.joshskills.ui.voip.RTC_IS_FAVORITE
import com.joshtalks.joshskills.ui.voip.RTC_NAME
import com.joshtalks.joshskills.ui.voip.RTC_TOKEN_KEY
import com.joshtalks.joshskills.ui.voip.RTC_UID_KEY
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.joshtalks.joshskills.ui.voip.analytics.VoipAnalytics.pushIncomingCallAnalytics
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import kotlin.collections.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

const val FCM_TOKEN = "fcmToken"
const val HAS_NOTIFICATION = "has_notification"
const val HAS_LOCAL_NOTIFICATION = "has_local_notification"
const val NOTIFICATION_ID = "notification_id"
const val HAS_COURSE_REPORT = "has_course_report"
const val QUESTION_ID = "question_id"

class FirebaseNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.tag(FirebaseNotificationService::class.java.name).e(token)
        PrefManager.put(FCM_TOKEN, token)
        CleverTapAPI.getDefaultInstance(this)?.pushFcmRegistrationId(token, true)
        if (AppObjectController.freshChat != null) {
            AppObjectController.freshChat?.setPushRegistrationToken(token)
        }
        CoroutineScope(Dispatchers.IO).launch {
            val userId = Mentor.getInstance().getId()
            val fcmResponse = FCMResponse.getInstance()
            fcmResponse?.apiStatus = ApiRespStatus.POST
            fcmResponse?.update()
            if (fcmResponse != null && userId.isNotBlank()) {
                val data = mutableMapOf("user_id" to userId, "registration_id" to token)
                val resp =
                    AppObjectController.signUpNetworkService.patchFCMToken(
                        fcmResponse.id,
                        data.toMap()
                    )
                if (resp.isSuccessful) {
                    Timber.d("FCMToken asdf : Updated on Server")
                    fcmResponse.userId = userId
                    fcmResponse.apiStatus = ApiRespStatus.PATCH
                    fcmResponse.update()
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.tag(FirebaseNotificationService::class.java.name).e("fcm")
        try {
            if (Freshchat.isFreshchatNotification(remoteMessage)) {
                Freshchat.handleFcmMessage(this, remoteMessage)
            } /*else if (remoteMessage.data.containsKey("message") && remoteMessage.data["message"] != null && Mentor.getInstance()
                    .hasId()
            ) {
                msgCount++
                showGroupChatNotification(remoteMessage.data["message"]!!)
            }*/ else {
                if (BuildConfig.DEBUG) {
                    Timber.tag(FirebaseNotificationService::class.java.simpleName).e(
                        Gson().toJson(remoteMessage.data)
                    )
                }

                if (remoteMessage.data.containsKey("nType")) {
                    val notificationTypeToken: Type =
                        object : TypeToken<ShortNotificationObject>() {}.type
                    val shortNc: ShortNotificationObject = AppObjectController.gsonMapper.fromJson(
                        AppObjectController.gsonMapper.toJson(remoteMessage.data),
                        notificationTypeToken
                    )
                    FirestoreDB.getNotification {
                        val nc = it.toNotificationObject(shortNc.id)
                        if (remoteMessage.data["nType"] == "CR") {
                            nc.actionData?.let {
                                pushIncomingCallAnalytics(it)
                            }
                        }
                        sendNotification(nc)
                    }
                } else {
                    val notificationTypeToken: Type =
                        object : TypeToken<NotificationObject>() {}.type
                    val nc: NotificationObject = AppObjectController.gsonMapper.fromJson(
                        AppObjectController.gsonMapper.toJson(remoteMessage.data),
                        notificationTypeToken
                    )
                    sendNotification(nc)
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
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

                val activityList = arrayOf(this)
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
            if (PrefManager.getStringValue(API_TOKEN).isNotEmpty()) {
                EngagementNetworkHelper.receivedNotification(notificationObject)
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
                Mentor.deleteUserData()
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
                Mentor.deleteUserCredentials()
                return null
            }
            NotificationAction.ACTION_DELETE_USER_AND_DATA -> {
                if (Mentor.getInstance().hasId()) {
                    Mentor.deleteUserCredentials()
                    Mentor.deleteUserData()
                }
                return null
            }
            NotificationAction.ACTION_LOGOUT_USER -> {
                if (Mentor.getInstance().hasId()) {
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
                //if (User.getInstance().isVerified) {
                incomingCallNotificationAction(notificationObject.actionData)
                //}
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
                }
                return null
            }
//            NotificationAction.GROUP_CHAT_REPLY -> {
//                if (Mentor.getInstance().hasId()) {
//                    notificationChannelId = groupChatChannelId
//                    Intent(applicationContext, InboxActivity::class.java).apply {
//                        putExtra(NOTIFICATION_ID, 10112)
//                        putExtra(HAS_NOTIFICATION, true)
//                        putExtra(StringContract.IntentStrings.GUID, actionData)
//                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                    }
//                } else return null
//            }
//            NotificationAction.GROUP_CHAT_VOICE_NOTE_HEARD -> {
//                if (Mentor.getInstance().hasId()) {
//                    notificationChannelId = groupChatChannelId
//                    Intent(applicationContext, InboxActivity::class.java).apply {
//                        putExtra(NOTIFICATION_ID, 10122)
//                        putExtra(HAS_NOTIFICATION, true)
//                        putExtra(StringContract.IntentStrings.GUID, actionData)
//                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                    }
//                } else return null
//            }
//            NotificationAction.GROUP_CHAT_PIN_MESSAGE -> {
//                if (Mentor.getInstance().hasId()) {
//                    notificationChannelId = groupChatChannelId
//                    Intent(applicationContext, InboxActivity::class.java).apply {
//                        putExtra(NOTIFICATION_ID, 10132)
//                        putExtra(HAS_NOTIFICATION, true)
//                        putExtra(StringContract.IntentStrings.GUID, actionData)
//                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                    }
//                } else return null
//            }
            else -> {
                return null
            }
        }
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
    }

    private fun returnDefaultIntent(): Intent {
        return Intent(applicationContext, LauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(HAS_NOTIFICATION, true)
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
            // addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
            // addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
            // addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

//    private fun showGroupChatNotification(data: String) {
//        executor.execute {
//            try {
//                val baseMessage = CometChatHelper.processMessage(JSONObject(data))
//                val group = baseMessage.receiver as Group
//                val message = if (
//                    baseMessage.category == CometChatConstants.CATEGORY_MESSAGE &&
//                    baseMessage.type == CometChatConstants.MESSAGE_TYPE_TEXT
//                ) {
//                    (baseMessage as TextMessage).text.trim()
//                } else if (
//                    baseMessage.category == CometChatConstants.CATEGORY_MESSAGE &&
//                    baseMessage.type == CometChatConstants.MESSAGE_TYPE_AUDIO
//                ) {
//                    var voiceMessage: String? =
//                        String.format(this.resources.getString(R.string.shared_a_audio), "")
//                    if (baseMessage.metadata.has("audioDurationInMs")) {
//                        val audioDurationInMs: Long =
//                            baseMessage.metadata.getLong("audioDurationInMs")
//                        voiceMessage = String.format(
//                            this.resources.getString(R.string.shared_a_audio),
//                            "(" + formatDuration(audioDurationInMs.toInt()) + ")"
//                        )
//                    }
//                    voiceMessage
//                } else {
//                    null
//                }
//                if (Utils.isMessageVisible(baseMessage) && message != null) {
//                    unreadMessageList.add(baseMessage)
//                }
//                val clickIntent =
//                    Intent(applicationContext, InboxActivity::class.java).apply {
//                        putExtra(NOTIFICATION_ID, baseMessage.receiverUid)
//                        putExtra(HAS_NOTIFICATION, true)
//                        putExtra(StringContract.IntentStrings.GUID, baseMessage.receiverUid)
//                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                    }
//                val uniqueRequestCode = (System.currentTimeMillis() and 0xfffffff).toInt()
//                val pendingClickIntent = PendingIntent.getActivities(
//                    applicationContext,
//                    uniqueRequestCode,
//                    arrayOf(clickIntent),
//                    PendingIntent.FLAG_UPDATE_CURRENT
//                )
//                val dismissIntent = Intent(
//                    applicationContext,
//                    DismissNotifEventReceiver::class.java
//                ).apply {
//                    putExtra(NOTIFICATION_ID, baseMessage.receiverUid)
//                    putExtra(HAS_NOTIFICATION, true)
//                }
//                val pendingDismissIntent: PendingIntent = PendingIntent.getBroadcast(
//                    applicationContext,
//                    uniqueRequestCode,
//                    dismissIntent,
//                    0
//                )
//                val defaultSound =
//                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
//                val iconUrl =
//                    "https://s3.ap-south-1.amazonaws.com/www.static.skills.com/skills+logo.png"
//                val notificationManager =
//                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//                val style = NotificationCompat.BigTextStyle()
//                    .setBigContentTitle(baseMessage.sender.name)
//                    .setSummaryText(group.name)
//                    .bigText(message)
//
//                val chatGroupIcon: Bitmap? = if (baseMessage.sender.avatar.isNullOrEmpty()) {
//                    getNameInitialBitmap(baseMessage.sender.name, null)
//                } else {
//                    getBitmapFromURL(baseMessage.sender.avatar)?.run { getCroppedBitmap(this) }
//                }
//
//                val chatGroup = Person.Builder()
//                    .setImportant(true)
//                    .setName(
//                        HtmlCompat.fromHtml(
//                            "<b>${(baseMessage.receiver as Group).name}</b>",
//                            HtmlCompat.FROM_HTML_MODE_COMPACT
//                        )
//                    )
//                    .setKey(baseMessage.receiverUid)
//                    .setIcon(IconCompat.createWithBitmap(chatGroupIcon))
//                    .build()
//
//                val conversationTitle =
//                    if (unreadMessageList.size > 1) group.name + " (${unreadMessageList.size} Messages)" else group.name
//                val messagingStyle = NotificationCompat.MessagingStyle(chatGroup)
//                    .setConversationTitle(conversationTitle)
//                    .setGroupConversation(true)
//
//                unreadMessageList.listIterator().forEach {
//                    val messageText = if (
//                        it.category == CometChatConstants.CATEGORY_MESSAGE &&
//                        it.type == CometChatConstants.MESSAGE_TYPE_TEXT
//                    ) {
//                        (it as TextMessage).text.trim()
//                    } else if (
//                        it.category == CometChatConstants.CATEGORY_MESSAGE &&
//                        it.type == CometChatConstants.MESSAGE_TYPE_AUDIO
//                    ) {
//                        var voiceMessage: String? =
//                            String.format(this.resources.getString(R.string.shared_a_audio), "")
//                        if (it.metadata.has("audioDurationInMs")) {
//                            val audioDurationInMs: Long =
//                                it.metadata.getLong("audioDurationInMs")
//                            voiceMessage = String.format(
//                                this.resources.getString(R.string.shared_a_audio),
//                                "(" + formatDuration(audioDurationInMs.toInt()) + ")"
//                            )
//                        }
//                        voiceMessage
//                    } else {
//                        null
//                    }
//                    val senderColor =
//                        if (it.sender.metadata != null && it.sender.metadata.has("color_code"))
//                            it.sender.metadata.getString("color_code")
//                        else
//                            "#" + Integer.toHexString(
//                                ContextCompat.getColor(
//                                    this,
//                                    R.color.colorPrimary
//                                )
//                            )
//
//                    val senderIcon: Bitmap? = if (it.sender.avatar.isNullOrEmpty()) {
//                        getNameInitialBitmap(it.sender.name, senderColor)
//                    } else {
//                        getBitmapFromURL(it.sender.avatar)?.run { getCroppedBitmap(this) }
//                    }
//
//                    val sender = Person.Builder()
//                        .setImportant(true)
//                        .setName(
//                            HtmlCompat.fromHtml(
//                                "<b><font color=$senderColor>${it.sender.name}</font></b>",
//                                HtmlCompat.FROM_HTML_MODE_COMPACT
//                            )
//                        )
//                        .setKey(it.sender.uid)
//                        .setIcon(IconCompat.createWithBitmap(senderIcon))
//                        .build()
//
//                    val notificationMessage = NotificationCompat.MessagingStyle.Message(
//                        messageText,
//                        it.sentAt * 1000L,
//                        sender
//                    )
//                    messagingStyle.addMessage(notificationMessage)
//                }
//
//                val notificationBuilder = NotificationCompat.Builder(
//                    this@FirebaseNotificationService,
//                    groupChatChannelId
//                ).apply {
//                    setTicker("You have a new message")
//                    setSmallIcon(R.drawable.ic_status_bar_notification)
//                    setLargeIcon(getBitmapFromURL(group.icon)?.run { getCroppedBitmap(this) })
//                    setContentTitle(group.name)
//                    setContentText(message)
//                    setContentIntent(pendingClickIntent) // intent that will fire when user taps the notification
//                    setDeleteIntent(pendingDismissIntent)
//                    setAutoCancel(true)    // automatically removes the notification when the user taps it
//                    setSound(defaultSound)
//                    setStyle(style)
//                    setWhen(baseMessage.sentAt * 1000L)
//                    setDefaults(Notification.DEFAULT_ALL)
//                    setCategory(NotificationCompat.CATEGORY_MESSAGE)
//                    setGroup(groupChatChannelName)
//                    setOnlyAlertOnce(false) // Interrupts the user (with sound, vibration, or visual clues) only the first time
//                    color = ContextCompat.getColor(
//                        this@FirebaseNotificationService,
//                        R.color.colorAccent
//                    )
//                }
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                    notificationBuilder.priority = NotificationManager.IMPORTANCE_HIGH
//                    notificationBuilder.setStyle(messagingStyle)
//                }
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//
//                    var channelIndex: Int =
//                        PrefManager.getIntValue("group_chat_notification_channel_index")
//                    val existingNotificationChannel =
//                        notificationManager.getNotificationChannel(groupChatChannelId + channelIndex)
//                    if (existingNotificationChannel != null) {
//                        notificationManager.deleteNotificationChannel(groupChatChannelId + channelIndex)
//                        channelIndex++
//                        PrefManager.put("group_chat_notification_channel_index", channelIndex)
//                    }
//
//                    // Create the NotificationChannel
//                    val newNotificationChannel = NotificationChannel(
//                        groupChatChannelId + channelIndex,
//                        groupChatChannelName,
//                        NotificationManager.IMPORTANCE_HIGH
//                    ).apply {
//                        description = "Notifications for group chat messages"
//                        enableLights(true)
//                        enableVibration(true)
//                        setBypassDnd(true)
//                    }
//
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                        newNotificationChannel.setAllowBubbles(true)
//                    }
//
//                    try {
//                        // Register the channel with the system
//                        notificationManager.createNotificationChannel(newNotificationChannel)
//                    } catch (e: java.lang.Exception) {
//                        this.stopSelf()
//                    }
//
//                    // Set Channel Id of Notification
//                    notificationBuilder.setChannelId(groupChatChannelId + channelIndex)
//                }
//
// //                val isChatScreenOpen =
// //                    AppObjectController.currentActivityClass == CometChatMessageListActivity::class.simpleName
//                val isChatScreenOpen = false
//                val isNotificationMuted = getBoolValue(IS_GROUP_NOTIFICATION_MUTED, false, false)
//                if (Utils.isMessageVisible(baseMessage) && message != null && isChatScreenOpen.not() && isNotificationMuted.not()) {
//                    notificationManager.notify(
//                        group.guid.hashCode(),
//                        notificationBuilder.build()
//                    )
//                }
//            } catch (e: java.lang.Exception) {
//                e.printStackTrace()
//            }
//        }
//    }

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

    companion object {
        private val executor: ExecutorService by lazy {
            JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Notification")
        }

        private var notificationChannelId = "101111"
        private var notificationChannelName = NotificationChannelNames.DEFAULT.type

        @RequiresApi(Build.VERSION_CODES.N)
        private var importance = NotificationManager.IMPORTANCE_DEFAULT

        public fun sendFirestoreNotification(
            notificationObject: NotificationObject,
            context: Context
        ) {
            executor.execute {

                val intent = getIntentForNotificationAction(
                    notificationObject,
                    notificationObject.action,
                    notificationObject.actionData
                )

                intent?.run {
                    putExtra(HAS_NOTIFICATION, true)
                    putExtra(NOTIFICATION_ID, notificationObject.id)

                    val activityList = arrayOf(this)
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
                if (PrefManager.getStringValue(API_TOKEN).isNotEmpty()) {
                    EngagementNetworkHelper.receivedNotification(notificationObject)
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
                    //if (User.getInstance().isVerified) {
                    incomingCallNotificationAction(notificationObject.actionData)
                    //}
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
                else -> {
                    null
                }
            }
        }

        private fun callForceDisconnect() {
            WebRtcService.forceDisconnect()
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
