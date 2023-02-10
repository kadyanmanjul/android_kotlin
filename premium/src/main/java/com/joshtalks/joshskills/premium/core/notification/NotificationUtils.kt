package com.joshtalks.joshskills.premium.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.facebook.share.internal.ShareConstants
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.core.COURSE_ID
import com.joshtalks.joshskills.premium.core.analytics.DismissNotifEventReceiver
import com.joshtalks.joshskills.premium.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.premium.core.io.LastSyncPrefManager
import com.joshtalks.joshskills.premium.core.notification.client_side.AlarmUtil
import com.joshtalks.joshskills.premium.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.premium.repository.local.entity.Question
import com.joshtalks.joshskills.premium.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.premium.repository.local.model.*
import com.joshtalks.joshskills.premium.track.CONVERSATION_ID
import com.joshtalks.joshskills.premium.ui.assessment.AssessmentActivity
import com.joshtalks.joshskills.premium.ui.chat.ConversationActivity
import com.joshtalks.joshskills.premium.ui.chat.UPDATED_CHAT_ROOM_OBJECT
import com.joshtalks.joshskills.premium.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.premium.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.premium.ui.fpp.SeeAllRequestsActivity
import com.joshtalks.joshskills.premium.ui.group.JoshGroupActivity
import com.joshtalks.joshskills.premium.ui.group.analytics.GroupAnalytics
import com.joshtalks.joshskills.premium.ui.inbox.InboxActivity
import com.joshtalks.joshskills.premium.ui.launch.LauncherActivity
import com.joshtalks.joshskills.premium.ui.leaderboard.LeaderBoardViewPagerActivity
import com.joshtalks.joshskills.premium.ui.lesson.LessonActivity
import com.joshtalks.joshskills.premium.ui.lesson.SPEAKING_POSITION
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.BuyPageActivity
import com.joshtalks.joshskills.premium.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.premium.ui.referral.ReferralActivity
import com.joshtalks.joshskills.premium.ui.reminder.reminder_listing.ReminderListActivity
import com.joshtalks.joshskills.premium.ui.signup.FreeTrialOnBoardActivity
import com.joshtalks.joshskills.premium.ui.voip.favorite.FavoriteListActivity
import com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.views.CallRecordingShare
import com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.voip.constant.INCOMING_CALL_ID
import com.joshtalks.joshskills.voip.constant.REMOTE_USER_NAME
import com.joshtalks.joshskills.voip.data.CallingRemoteService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.lang.reflect.Type
import java.util.concurrent.ExecutorService

class NotificationUtils(val context: Context) {
    companion object {
        private val executor: ExecutorService by lazy {
            JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Notification")
        }

        private var notificationChannelId = NotificationChannelData.DEFAULT.id
        private var notificationChannelName = NotificationChannelData.DEFAULT.type

        @RequiresApi(Build.VERSION_CODES.N)
        private var importance = NotificationManager.IMPORTANCE_DEFAULT
    }

    fun processRemoteMessage(remoteData: RemoteMessage, channel: NotificationAnalytics.Channel) {
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
                        || notificationObject.action == NotificationAction.CALL_RECORDING_NOTIFICATION
                        || notificationObject.action == NotificationAction.INITIATE_RANDOM_CALL
                    ) {
                        val inboxIntent = InboxActivity.getInboxIntent(context)
                        inboxIntent.putExtra(HAS_NOTIFICATION, true)
                        inboxIntent.putExtra(NOTIFICATION_ID, notificationObject.id)
                        arrayOf(inboxIntent, this)
                    } else {
                        arrayOf(this)
                    }

                val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()
                val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val pendingIntent = PendingIntent.getActivities(
                    context.applicationContext,
                    uniqueInt, activityList,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    else
                        PendingIntent.FLAG_UPDATE_CURRENT
                )
                val style = NotificationCompat.BigTextStyle()
                style.setBigContentTitle(notificationObject.contentTitle)
                style.bigText(notificationObject.contentText)
                style.setSummaryText("")

                val notificationBuilder =
                    NotificationCompat.Builder(context, notificationChannelId)
                        .setTicker(notificationObject.ticker)
                        .setSmallIcon(com.joshtalks.joshskills.R.drawable.ic_status_bar_notification)
                        .setContentTitle(notificationObject.contentTitle)
                        .setAutoCancel(true)
                        .setSound(defaultSound)
                        .setContentText(notificationObject.contentText)
                        .setContentIntent(pendingIntent)
                        .setStyle(style)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setWhen(System.currentTimeMillis())
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setColor(ContextCompat.getColor(context, R.color.primary_500))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    notificationBuilder.priority = NotificationManager.IMPORTANCE_HIGH
                }

                val dismissIntent = Intent(context, DismissNotifEventReceiver::class.java).apply {
                    putExtra(NOTIFICATION_ID, notificationObject.id)
                    putExtra(HAS_NOTIFICATION, true)
                }
                val dismissPendingIntent: PendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        uniqueInt,
                        dismissIntent,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            PendingIntent.FLAG_IMMUTABLE
                        else
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
                //TODO: Change this uniqueInt to notificationID
                notificationManager.notify(uniqueInt, notificationBuilder.build())
            }
        }
    }

    private fun getIntentAccordingAction(
        notificationObject: NotificationObject,
        action: NotificationAction?,
        actionData: String?
    ): Intent? {
        if (PrefManager.getBoolValue(IS_USER_LOGGED_IN, isConsistent = true, defValue = false).not()) {
            return Intent(context, LauncherActivity::class.java)
        }

        return when (action) {
            NotificationAction.OPEN_APP -> {
                return Intent(context, LauncherActivity::class.java)
            }
            NotificationAction.ACTION_OPEN_TEST -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    importance = NotificationManager.IMPORTANCE_HIGH
                }
                //TODO: notificationChannelId = NotificationAction.ACTION_OPEN_TEST.type
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
                notificationChannelId = NotificationChannelData.UPDATES.id
                notificationChannelName = NotificationChannelData.UPDATES.type
                return processChatTypeNotification(notificationObject, action, actionData)
            }
            NotificationAction.ACTION_OPEN_LESSON -> {
                if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
                    return null
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    importance = NotificationManager.IMPORTANCE_HIGH
                }
                notificationChannelId = NotificationChannelData.UPDATES.id
                notificationChannelName = NotificationChannelData.UPDATES.type
                return processOpenLessonNotification(notificationObject, action, actionData)
            }
            NotificationAction.ACTION_OPEN_SPEAKING_SECTION -> {
                if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
                    return null
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    importance = NotificationManager.IMPORTANCE_HIGH
                }
                notificationChannelId = NotificationChannelData.UPDATES.id
                notificationChannelName = NotificationChannelData.UPDATES.type
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
                notificationChannelId = NotificationChannelData.UPDATES.id
                notificationChannelName = NotificationChannelData.UPDATES.type
                return processOpenPaymentNotification(notificationObject, action, actionData)
            }
            NotificationAction.ACTION_OPEN_COURSE_EXPLORER -> {
                notificationChannelId = NotificationChannelData.UPDATES.id
                notificationChannelName = NotificationChannelData.UPDATES.type
                return Intent(context, CourseExploreActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
            }
            NotificationAction.ACTION_OPEN_URL -> {
                notificationChannelId = NotificationChannelData.UPDATES.id
                notificationChannelName = NotificationChannelData.UPDATES.type
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
                notificationChannelId = NotificationChannelData.UPDATES.id
                notificationChannelName = NotificationChannelData.UPDATES.type
                Intent(context, InboxActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            }
            NotificationAction.ACTION_UP_SELLING_POPUP -> {
                if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
                    return null
                }
                Intent(context, InboxActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
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
                notificationChannelId = NotificationChannelData.REMINDERS.id
                notificationChannelName = NotificationChannelData.REMINDERS.type
                return Intent(context, ReferralActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
            }
            NotificationAction.ACTION_DELETE_DATA -> {
                if (User.getInstance().isVerified) {
                    Mentor.deleteUserData()
                }
                return null
            }
            NotificationAction.ACTION_DELETE_CONVERSATION_DATA -> {
                actionData?.let { deleteConversationData(it) }
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
                //TODO: notificationChannelId = NotificationAction.ACTION_OPEN_REFERRAL.name
                return Intent(context, ReminderListActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
            }
            NotificationAction.AWARD_DECLARE -> {
                Intent(context, LeaderBoardViewPagerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                return null
            }
            NotificationAction.ACTION_OPEN_FREE_TRIAL_SCREEN -> {
                Intent(
                    AppObjectController.joshApplication,
                    FreeTrialOnBoardActivity::class.java
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            }
            NotificationAction.ACTION_OPEN_GROUPS -> {
                notificationChannelId = NotificationChannelData.MESSAGES_REQUESTS.id
                notificationChannelName = NotificationChannelData.MESSAGES_REQUESTS.type
                return Intent(context, JoshGroupActivity::class.java).apply {
                    putExtra(CONVERSATION_ID, actionData)
                }
            }
            NotificationAction.ACTION_OPEN_FPP_REQUESTS -> {
                notificationChannelId = NotificationChannelData.MESSAGES_REQUESTS.id
                notificationChannelName = NotificationChannelData.MESSAGES_REQUESTS.type
                return Intent(context, SeeAllRequestsActivity::class.java)
            }
            NotificationAction.ACTION_OPEN_FPP_LIST -> {
                notificationChannelId = NotificationChannelData.MESSAGES_REQUESTS.id
                notificationChannelName = NotificationChannelData.MESSAGES_REQUESTS.type
                return Intent(context, FavoriteListActivity::class.java).apply {
                    putExtra(CONVERSATION_ID, actionData)
                    putExtra(IS_COURSE_BOUGHT, true)
                }
            }
            NotificationAction.ACTION_OPEN_GROUP_CHAT_CLIENT -> {
                notificationChannelId = NotificationChannelData.MESSAGES_REQUESTS.id
                notificationChannelName = NotificationChannelData.MESSAGES_REQUESTS.type
                if (actionData.equals(Mentor.getInstance().getId()))
                    return null
                return Intent(context, JoshGroupActivity::class.java).apply {
                    putExtra(CONVERSATION_ID, actionData)
                }
            }
            NotificationAction.EMERGENCY_NOTIFICATION -> {
                lateinit var intent: Intent
                if (actionData?.isDigitsOnly() == true && isValidFullNumber("+91", actionData)) {
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
                try {
                    val jsonObj = JSONObject(actionData ?: EMPTY)
                    val callContext = context
                    val remoteServiceIntent = Intent(callContext, CallingRemoteService::class.java)
                    remoteServiceIntent.putExtra(INCOMING_CALL_ID, jsonObj.getString(INCOMING_CALL_ID))
                    remoteServiceIntent.putExtra(INCOMING_CALL_CATEGORY,jsonObj.getString(INCOMING_CALL_CATEGORY))
                    remoteServiceIntent.action = SERVICE_ACTION_INCOMING_CALL
                    com.joshtalks.joshskills.voip.Utils.context?.startService(remoteServiceIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return null
            }
            NotificationAction.ACTION_FPP_INCOMING_CALL -> {
                try {
                    val jsonObj = JSONObject(actionData ?: EMPTY)
                    val callContext = context
                    val remoteServiceIntent = Intent(callContext, CallingRemoteService::class.java)
                    remoteServiceIntent.putExtra(INCOMING_CALL_ID, jsonObj.getString(INCOMING_CALL_ID))
                    remoteServiceIntent.putExtra(INCOMING_CALL_CATEGORY, jsonObj.getString(INCOMING_CALL_CATEGORY))
                    remoteServiceIntent.putExtra(REMOTE_USER_NAME, jsonObj.getString(REMOTE_USER_NAME))
                    remoteServiceIntent.action = SERVICE_ACTION_INCOMING_CALL
                    callContext.startService(remoteServiceIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return null
            }
            NotificationAction.ACTION_EXPERT_INCOMING_CALL -> {
                try {
                    val jsonObj = JSONObject(actionData ?: EMPTY)
                    val callContext = context
                    val remoteServiceIntent = Intent(callContext, CallingRemoteService::class.java)
                    remoteServiceIntent.putExtra(INCOMING_CALL_ID, jsonObj.getString(INCOMING_CALL_ID))
                    remoteServiceIntent.putExtra(INCOMING_CALL_CATEGORY, jsonObj.getString(INCOMING_CALL_CATEGORY))
                    remoteServiceIntent.putExtra(REMOTE_USER_NAME, jsonObj.getString(REMOTE_USER_NAME))
                    remoteServiceIntent.putExtra(IS_PREMIUM_USER, jsonObj.optString(IS_PREMIUM_USER, "false"))
                    remoteServiceIntent.action = SERVICE_ACTION_INCOMING_CALL
                    callContext.startService(remoteServiceIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return null
            }
            NotificationAction.ACTION_GROUP_INCOMING_CALL -> {
                try {
                    val jsonObj = JSONObject(actionData ?: EMPTY)
                    val callContext = context
                    val remoteServiceIntent = Intent(callContext, CallingRemoteService::class.java)
                    remoteServiceIntent.putExtra(INCOMING_CALL_ID, jsonObj.getString(INCOMING_CALL_ID))
                    remoteServiceIntent.putExtra(INCOMING_CALL_CATEGORY, jsonObj.getString(INCOMING_CALL_CATEGORY))
                    remoteServiceIntent.putExtra(INCOMING_GROUP_NAME, jsonObj.getString(INCOMING_GROUP_NAME))
                    remoteServiceIntent.putExtra(INCOMING_GROUP_IMAGE, jsonObj.getString(INCOMING_GROUP_IMAGE))
                    remoteServiceIntent.action = SERVICE_ACTION_INCOMING_CALL
                    callContext.startService(remoteServiceIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return null
            }
            NotificationAction.CALL_RECORDING_NOTIFICATION -> {
                if (notificationObject.extraData.isNullOrBlank()) {
                    return null
                } else return CallRecordingShare.getActivityIntentForSharableCallRecording(
                    context = context,
                    videoUrl = notificationObject.extraData,
                )
            }
            NotificationAction.INITIATE_RANDOM_CALL -> {
                val intent = Intent(context, VoiceCallActivity::class.java).apply {
                    putExtra(INTENT_DATA_COURSE_ID, "151")
                    putExtra(INTENT_DATA_TOPIC_ID, "5")
                    putExtra(STARTING_POINT, FROM_ACTIVITY)
                    putExtra(INTENT_DATA_CALL_CATEGORY, Category.PEER_TO_PEER.ordinal)
                }
                return intent
            }
            NotificationAction.STICKY_COUPON -> {
                try {
                    if (PrefManager.getBoolValue(IS_COURSE_BOUGHT).not()) {
                        val jsonObj = JSONObject(actionData ?: EMPTY)
                        val serviceIntent = Intent(context, StickyNotificationService::class.java)
                        serviceIntent.putExtra("sticky_title", notificationObject.contentTitle)
                        serviceIntent.putExtra("sticky_body", notificationObject.contentText)
                        serviceIntent.putExtra("coupon_code", jsonObj.getString("coupon_code"))
                        serviceIntent.putExtra("expiry_time", jsonObj.getLong("expiry_time") * 1000L)
                        addValueToPref(jsonObj, notificationObject.contentTitle, notificationObject.contentText)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            WorkManagerAdmin.setStickyNotificationWorker(
                                title = notificationObject.contentTitle,
                                body = notificationObject.contentText,
                                coupon = jsonObj.getString("coupon_code"),
                                expiry = jsonObj.getLong("expiry_time") * 1000L
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            context.startForegroundService(serviceIntent)
                        else
                            context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return null
            }
            else -> {
                return null
            }
        }
    }

    private fun deleteConversationData(courseId: String) {
        CoroutineScope(Dispatchers.IO).launch {
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
    }

    private fun isNotificationCrash(): Class<*> {
        val isNotificationCrash = false
        return if (isNotificationCrash) {
            InboxActivity::class.java
        } else {
            ConversationActivity::class.java
        }
    }

    private fun isOpenPaymentNotificationCrash(): Class<*> {
        val isNotificationCrash = false
        return if (isNotificationCrash) {
            InboxActivity::class.java
        } else {
            BuyPageActivity::class.java
        }
    }

    private fun isOpenLessonNotificationCrash(): Class<*> {
        val isNotificationCrash = false
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
            val lessonId = obj.getInt(LessonActivity.LESSON_ID)
            putExtra(LessonActivity.LESSON_ID, lessonId)
            putExtra(LessonActivity.IS_DEMO, false)
            putExtra(
                LessonActivity.IS_LESSON_COMPLETED,
                obj.getBoolean(LessonActivity.IS_LESSON_COMPLETED)
            )
            putExtra(CONVERSATION_ID, obj.getString(CONVERSATION_ID))
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(ShareConstants.ACTION_TYPE, action)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return rIntent
    }

    private fun processOpenSpeakingSectionNotification(
        notificationObject: NotificationObject?,
        action: NotificationAction?,
        actionData: String?
    ): Intent {

        val rIntent = Intent(context.applicationContext, isOpenLessonNotificationCrash()).apply {
            try {
                putExtra(LessonActivity.LESSON_ID, actionData?.toInt() ?: 20)
            } catch (ex: Exception) {
                putExtra(LessonActivity.LESSON_ID, 20)
            }
            putExtra(LessonActivity.IS_DEMO, false)
            putExtra(LessonActivity.IS_LESSON_COMPLETED, false)
            putExtra(LessonActivity.LESSON_SECTION, SPEAKING_POSITION)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(ShareConstants.ACTION_TYPE, action)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
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
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return rIntent
    }

    private fun addValueToPref(json: JSONObject, title: String?, body: String?) {
        json.put("sticky_title", title)
        json.put("sticky_body", body)
        PrefManager.put(STICKY_COUPON_DATA, json.toString())
    }

    fun pushAnalytics(groupId: String?) {
        if (groupId != null) {
            GroupAnalytics.push(GroupAnalytics.Event.NOTIFICATION_RECEIVED, groupId)
        }
    }

    fun updateNotificationDb() {
        CoroutineScope(Dispatchers.IO).launch {
            val categoryMap = PrefManager.getPrefMap(NOTIFICATION_CATEGORY_SCHEDULED) ?: mutableMapOf()
            PrefManager.putPrefObject(NOTIFICATION_CATEGORY_SCHEDULED, categoryMap)
            when {
                categoryMap.containsKey(NotificationCategory.AFTER_FIVE_MIN_CALL.category) ->
                    updateNotificationDb(NotificationCategory.AFTER_FIVE_MIN_CALL)
                categoryMap.containsKey(NotificationCategory.AFTER_FIRST_CALL.category) ->
                    updateNotificationDb(NotificationCategory.AFTER_FIRST_CALL)
                categoryMap.containsKey(NotificationCategory.AFTER_LOGIN.category) ->
                    updateNotificationDb(NotificationCategory.AFTER_LOGIN)

                else -> updateNotificationDb(NotificationCategory.AFTER_LOGIN)
            }
            updateNotificationDb(NotificationCategory.EVENT_INDEPENDENT)
            if (categoryMap.containsKey(NotificationCategory.PAYMENT_INITIATED.category))
                updateNotificationDb(NotificationCategory.PAYMENT_INITIATED)
            else if (categoryMap.containsKey(NotificationCategory.AFTER_BUY_PAGE.category))
                updateNotificationDb(NotificationCategory.AFTER_BUY_PAGE)
        }
    }

    fun updateNotificationDb(category: NotificationCategory) {
        CoroutineScope(Dispatchers.IO).launch {
            val notificationList =
                AppObjectController.appDatabase.scheduleNotificationDao()
                    .getUnscheduledCatNotifications(category.category)
            val categoryMap = PrefManager.getPrefMap(NOTIFICATION_CATEGORY_SCHEDULED) ?: mutableMapOf()
            categoryMap[category.category] = 1
            PrefManager.putPrefObject(NOTIFICATION_CATEGORY_SCHEDULED, categoryMap)
            notificationList.forEach {
                val intent = Intent(context.applicationContext, ScheduledNotificationReceiver::class.java)
                intent.putExtra("id", it.id)
                val pendingIntent = PendingIntent.getBroadcast(
                    context.applicationContext,
                    it.id.hashCode(),
                    intent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    else
                        PendingIntent.FLAG_UPDATE_CURRENT
                )
                AlarmUtil(context).createAlarm(pendingIntent, it.frequency!!, it.execute_after)
                AppObjectController.appDatabase.scheduleNotificationDao().updateScheduled(it.id)
            }
        }
    }

    fun removeScheduledNotification(category: NotificationCategory) {
        CoroutineScope(Dispatchers.IO).launch {
            val notificationIds =
                AppObjectController.appDatabase.scheduleNotificationDao().removeCategory(category.category)
            notificationIds.forEach {
                val intent = Intent(context.applicationContext, ScheduledNotificationReceiver::class.java)
                intent.putExtra("id", it)
                val pendingIntent =
                    PendingIntent.getBroadcast(
                        context.applicationContext,
                        it.hashCode(),
                        intent,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        else
                            PendingIntent.FLAG_UPDATE_CURRENT
                    )
                AlarmUtil(context).deleteAlarm(pendingIntent)
            }
        }
    }

    fun removeAllScheduledNotification() {
        CoroutineScope(Dispatchers.IO).launch {
            removeAllNotificationAsync()
        }
    }

    suspend fun removeAllNotificationAsync() {
        val notificationIds = AppObjectController.appDatabase.scheduleNotificationDao().clearAllNotifications()
        notificationIds.forEach {
            try {
                val intent = Intent(context.applicationContext, ScheduledNotificationReceiver::class.java)
                intent.putExtra("id", it)
                val pendingIntent =
                    PendingIntent.getBroadcast(
                        context.applicationContext,
                        it.hashCode(),
                        intent,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        else
                            PendingIntent.FLAG_UPDATE_CURRENT
                    )
                AlarmUtil(context).deleteAlarm(pendingIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun removeAllNotifications() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }
}