package com.joshtalks.joshskills.core.notification

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
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.helpers.CometChatHelper
import com.cometchat.pro.models.BaseMessage
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
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.COURSE_ID
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.DismissNotifEventReceiver
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.NotificationAction
import com.joshtalks.joshskills.repository.local.model.NotificationObject
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.assessment.AssessmentActivity
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.chat.UPDATED_CHAT_ROOM_OBJECT
import com.joshtalks.joshskills.ui.conversation_practice.ConversationPracticeActivity
import com.joshtalks.joshskills.ui.conversation_practice.PRACTISE_ID
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.groupchat.constant.StringContract
import com.joshtalks.joshskills.ui.groupchat.messagelist.CometChatMessageListActivity
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.launch.LauncherActivity
import com.joshtalks.joshskills.ui.leaderboard.LeaderBoardViewPagerActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.reminder.reminder_listing.ReminderListActivity
import com.joshtalks.joshskills.ui.voip.RTC_CALLER_UID_KEY
import com.joshtalks.joshskills.ui.voip.RTC_CHANNEL_KEY
import com.joshtalks.joshskills.ui.voip.RTC_TOKEN_KEY
import com.joshtalks.joshskills.ui.voip.RTC_UID_KEY
import com.joshtalks.joshskills.ui.voip.WebRtcService
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import org.json.JSONObject
import timber.log.Timber


const val FCM_TOKEN = "fcmToken"
const val HAS_NOTIFICATION = "has_notification"
const val NOTIFICATION_ID = "notification_id"
const val HAS_COURSE_REPORT = "has_course_report"
const val QUESTION_ID = "question_id"


class FirebaseNotificationService : FirebaseMessagingService() {

    private var notificationChannelId = "101111"
    private var notificationChannelName = "JoshTalksDefault"
    private var groupChatChannelId = NotificationAction.GROUP_CHAT_MESSAGE_NOTIFICATION.name
    private var groupChatChannelName = "Group Chat"
    private var msgCount = 0

    @RequiresApi(Build.VERSION_CODES.N)
    private var importance = NotificationManager.IMPORTANCE_DEFAULT
    private val executor: ExecutorService =
        JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Notification")


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.tag(FirebaseNotificationService::class.java.name).e(token)
        PrefManager.put(FCM_TOKEN, token)
        if (AppObjectController.freshChat != null) {
            AppObjectController.freshChat?.setPushRegistrationToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.tag(FirebaseNotificationService::class.java.name).e("fcm")
        try {
            if (Freshchat.isFreshchatNotification(remoteMessage)) {
                Freshchat.handleFcmMessage(this, remoteMessage)
            } else if (remoteMessage.data.containsKey("message") && remoteMessage.data["message"] != null) {
                msgCount++
                val baseMessage =
                    CometChatHelper.processMessage(JSONObject(remoteMessage.data["message"]!!))
                val json = JSONObject(remoteMessage.data as Map<String, String>)
                showGroupChatNotification(baseMessage, json)
            } else {
                if (BuildConfig.DEBUG) {
                    Timber.tag(FirebaseNotificationService::class.java.simpleName).e(
                        Gson().toJson(remoteMessage.data)
                    )
                }
                val notificationTypeToken: Type =
                    object : TypeToken<NotificationObject>() {}.type
                val nc: NotificationObject = AppObjectController.gsonMapper.fromJson(
                    AppObjectController.gsonMapper.toJson(remoteMessage.data),
                    notificationTypeToken
                )
                sendNotification(nc)
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
                style.setSummaryText(notificationObject.contentText)

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
                deleteUserData()
                return null
            }
            NotificationAction.ACTION_DELETE_USER -> {
                deleteUserCredentials()
                return null
            }
            NotificationAction.ACTION_DELETE_USER_AND_DATA -> {
                deleteUserCredentials()
                deleteUserData()
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
                incomingCallNotificationAction(notificationObject.actionData)
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
            NotificationAction.CALL_NO_USER_FOUND_NOTIFICATION -> {
                WebRtcService.noUserFoundCallDisconnect()
                return null
            }
            NotificationAction.AUDIO_FEEDBACK_REPORT -> {
                //deleteUserCredentials()
                //deleteUserData()
                return null
            }
            NotificationAction.AWARD_DECLARE -> {
                Intent(applicationContext, LeaderBoardViewPagerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(HAS_NOTIFICATION, true)
                }
                return null
            }
            NotificationAction.GROUP_CHAT_REPLY -> {
                notificationChannelId = groupChatChannelId
                Intent(applicationContext, CometChatMessageListActivity::class.java).apply {
                    putExtra(NOTIFICATION_ID, 10112)
                    putExtra(HAS_NOTIFICATION, true)
                    putExtra(StringContract.IntentStrings.GUID, actionData)
                    putExtra(
                        StringContract.IntentStrings.TYPE,
                        CometChatConstants.RECEIVER_TYPE_GROUP
                    )
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            }
            NotificationAction.GROUP_CHAT_VOICE_NOTE_HEARD -> {
                notificationChannelId = groupChatChannelId
                Intent(applicationContext, CometChatMessageListActivity::class.java).apply {
                    putExtra(NOTIFICATION_ID, 10122)
                    putExtra(HAS_NOTIFICATION, true)
                    putExtra(StringContract.IntentStrings.GUID, actionData)
                    putExtra(
                        StringContract.IntentStrings.TYPE,
                        CometChatConstants.RECEIVER_TYPE_GROUP
                    )
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            }
            NotificationAction.GROUP_CHAT_PIN_MESSAGE -> {
                notificationChannelId = groupChatChannelId
                Intent(applicationContext, CometChatMessageListActivity::class.java).apply {
                    putExtra(NOTIFICATION_ID, 10132)
                    putExtra(HAS_NOTIFICATION, true)
                    putExtra(StringContract.IntentStrings.GUID, actionData)
                    putExtra(
                        StringContract.IntentStrings.TYPE,
                        CometChatConstants.RECEIVER_TYPE_GROUP
                    )
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            }
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

    private fun incomingCallNotificationAction(actionData: String?) {
        actionData?.let {
            try {
                val obj = JSONObject(it)
                val data = HashMap<String, String>()
                data[RTC_TOKEN_KEY] = obj.getString("token")
                data[RTC_CHANNEL_KEY] = obj.getString("channel_name")
                data[RTC_UID_KEY] = obj.getString("uid")
                data[RTC_CALLER_UID_KEY] = obj.getString("caller_uid")
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

    private fun deleteUserData() {
        AppObjectController.appDatabase.run {
            courseDao().getAllConversationId().forEach {
                PrefManager.removeKey(it)
            }
            clearAllTables()
        }
    }

    private fun deleteUserCredentials() {
        PrefManager.logoutUser()
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

    private fun processQuestionTypeNotification(
        notificationObject: NotificationObject?,
        action: NotificationAction?, actionData: String?
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
        action: NotificationAction?, actionData: String?
    ): Intent? {
        val obj: InboxEntity? = AppObjectController.appDatabase.courseDao()
            .chooseRegisterCourseMinimal(actionData!!)
        obj?.run {
            WorkManagerAdmin.updatedCourseForConversation(this.conversation_id)
        }

        if (obj == null)
            return null
        val rIntnet = Intent(applicationContext, isNotificationCrash()).apply {
            putExtra(UPDATED_CHAT_ROOM_OBJECT, obj)
            putExtra(ACTION_TYPE, action)
            putExtra(HAS_NOTIFICATION, true)
            putExtra(QUESTION_ID, actionData)
            // addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            //addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

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

    private fun showGroupChatNotification(baseMessage: BaseMessage, json: JSONObject) {
        try {

            val clickIntent =
                Intent(applicationContext, CometChatMessageListActivity::class.java).apply {
                    putExtra(NOTIFICATION_ID, baseMessage.receiverUid)
                    putExtra(HAS_NOTIFICATION, true)
                    putExtra(StringContract.IntentStrings.GUID, baseMessage.receiverUid)
                    putExtra(
                        StringContract.IntentStrings.TYPE,
                        CometChatConstants.RECEIVER_TYPE_GROUP
                    )
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            val uniqueRequestCode = (System.currentTimeMillis() and 0xfffffff).toInt()
            val pendingClickIntent = PendingIntent.getActivities(
                applicationContext,
                uniqueRequestCode,
                arrayOf(clickIntent),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val dismissIntent = Intent(
                applicationContext,
                DismissNotifEventReceiver::class.java
            ).apply {
                putExtra(NOTIFICATION_ID, baseMessage.receiverUid)
                putExtra(HAS_NOTIFICATION, true)
            }
            val pendingDismissIntent: PendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                uniqueRequestCode,
                dismissIntent,
                0
            )
            val defaultSound =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val iconUrl =
                "https://s3.ap-south-1.amazonaws.com/www.static.skills.com/skills+logo.png"
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val style = NotificationCompat.BigTextStyle()
                .setBigContentTitle(json.getString("title"))
                .bigText(json.getString("alert"))
                .setSummaryText(json.getString("title"))

            val personColor =
                if (baseMessage.sender.metadata != null && baseMessage.sender.metadata.has("color_code"))
                    baseMessage.sender.metadata.getString("color_code")
                else
                    "#" + Integer.toHexString(ContextCompat.getColor(this, R.color.colorPrimary))
            val chatGroupBuilder = Person.Builder()
                .setImportant(true)
                .setName(
                    HtmlCompat.fromHtml(
                        String.format(
                            "<b><font color=\"%s\">${baseMessage.sender.name}</font></b>",
                            personColor
                        ), HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                )
                .setKey(baseMessage.sender.uid)

            val bitmap: Bitmap? = if (baseMessage.sender.avatar.isNullOrEmpty()) {
                getNameInitialBitmap(baseMessage.sender.name, personColor)
            } else {
                getBitmapFromURL(baseMessage.sender.avatar)
            }

            bitmap?.let {
                chatGroupBuilder.setIcon(
                    IconCompat.createWithBitmap(getCroppedBitmap(it))
                )
            }

            val chatGroup = chatGroupBuilder.build()

            val person2 = Person.Builder()
                .setImportant(true)
                .setName(HtmlCompat.fromHtml("<b>You</b>", HtmlCompat.FROM_HTML_MODE_COMPACT))
                .setIcon(IconCompat.createWithResource(this, R.drawable.round_rectangle))
                .setKey("1234")
                .build()
            val message1 = NotificationCompat.MessagingStyle.Message(
                "Hi",
                System.currentTimeMillis() - 50000,
                chatGroup
            )
            val message2 = NotificationCompat.MessagingStyle.Message(
                "What's up?",
                System.currentTimeMillis() - 40000,
                person2
            )
            val message3 = NotificationCompat.MessagingStyle.Message(
                "Not much",
                System.currentTimeMillis() - 20000,
                chatGroup
            )
            val message4 = NotificationCompat.MessagingStyle.Message(
                json.getString("alert"),
                System.currentTimeMillis(),
                chatGroup
            )
            val messagingStyle = NotificationCompat.MessagingStyle(chatGroup)
                .setConversationTitle("ConvoTitle - GroupName")
                .addMessage(message1)
                .addMessage(message2)
                .addMessage(message3)
                .addMessage(message4)

            val notificationBuilder = NotificationCompat.Builder(
                this@FirebaseNotificationService,
                groupChatChannelId
            ).apply {
                setTicker(baseMessage.sender.name + " : " + json.getString("alert"))
                setSmallIcon(R.drawable.ic_status_bar_notification)
                setLargeIcon(getBitmapFromURL(iconUrl))
                setContentTitle(json.getString("title"))
                setContentText(json.getString("alert"))
                setContentIntent(pendingClickIntent) // intent that will fire when user taps the notification
                setDeleteIntent(pendingDismissIntent)
                setAutoCancel(true)    // automatically removes the notification when the user taps it
                setSound(defaultSound)
                setStyle(style)
                setWhen(System.currentTimeMillis())
                setDefaults(Notification.DEFAULT_ALL)
                setCategory(NotificationCompat.CATEGORY_MESSAGE)
                setGroup(groupChatChannelName)
                setOnlyAlertOnce(true) // Interupts the user (with sound, vibration, or visual clues) only the first time
                color = ContextCompat.getColor(
                    this@FirebaseNotificationService,
                    R.color.colorAccent
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationBuilder.priority = NotificationManager.IMPORTANCE_HIGH
                notificationBuilder.setStyle(messagingStyle)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the NotificationChannel, but only on API 26+ because
                // the NotificationChannel class is new and not in the support library
                val notificationChannel = NotificationChannel(
                    groupChatChannelId,
                    groupChatChannelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for group chat messages"
                    enableLights(true)
                    enableVibration(true)
                }
                // Register the channel with the system
                notificationManager.createNotificationChannel(notificationChannel)
                // Set Channel Id of Notification
                notificationBuilder.setChannelId(groupChatChannelId)
            }

            notificationManager.notify(
                baseMessage.receiverUid.hashCode(),
                notificationBuilder.build()
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
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

    fun getCroppedBitmap(bitmap: Bitmap): Bitmap? {
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
        //Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
        //return _bmp;
        return output
    }

    private fun getNameInitialBitmap(name: String, bgColorCode: String?): Bitmap {
        val width = 100
        val height = 100
        val centerX = Math.round(width * 0.5f)
        val centerY = Math.round(height * 0.5f)

        val nameSplitArray = name.split(" ".toRegex()).toTypedArray()
        val text = if (nameSplitArray.size > 1) {
            nameSplitArray[0].substring(0, 1) + nameSplitArray[1].substring(0, 1)
        } else {
            name.substring(0, 1)
        }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        textPaint.textSize = 16f * resources.displayMetrics.scaledDensity
        textPaint.color = Color.WHITE

        val textWidth: Float = textPaint.measureText(text) * 0.5f
        val textBaseLineHeight: Float = textPaint.fontMetrics.ascent * -0.4f
        val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(b)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (bgColorCode == null) {
            paint.color = ContextCompat.getColor(this, R.color.colorPrimary)
        } else {
            paint.color = Color.parseColor(bgColorCode)
        }

        canvas.drawCircle(
            centerX.toFloat(),
            centerY.toFloat(),
            Math.max((height / 2).toFloat(), textWidth / 2),
            paint
        )

        canvas.drawText(
            text,
            centerX - textWidth,
            centerY + textBaseLineHeight,
            textPaint
        )
        return b
    }

}
