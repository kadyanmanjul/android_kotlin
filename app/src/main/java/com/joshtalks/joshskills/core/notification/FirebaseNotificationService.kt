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
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.facebook.share.internal.ShareConstants.ACTION_TYPE
import com.freshchat.consumer.sdk.Freshchat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.API_TOKEN
import com.joshtalks.joshskills.core.ARG_PLACEHOLDER_URL
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.COURSE_ID
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.DismissNotifEventReceiver
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.NotificationAction
import com.joshtalks.joshskills.repository.local.model.NotificationObject
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.chat.UPDATED_CHAT_ROOM_OBJECT
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.launch.LauncherActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import timber.log.Timber
import java.util.concurrent.ExecutorService

const val FCM_TOKEN = "fcmToken"
const val HAS_NOTIFICATION = "has_notification"
const val NOTIFICATION_ID = "notification_id"
const val HAS_COURSE_REPORT = "has_course_report"
const val QUESTION_ID = "question_id"


class FirebaseNotificationService : FirebaseMessagingService() {

    private var notificationChannelId = "101111"
    private var notificationChannelName = "JoshTalksDefault"

    @RequiresApi(Build.VERSION_CODES.N)
    private var importance = NotificationManager.IMPORTANCE_DEFAULT
    private val executor: ExecutorService =
        JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Notification-Process")


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PrefManager.put(FCM_TOKEN, token)
        if (AppObjectController.freshChat != null) {
            AppObjectController.freshChat?.setPushRegistrationToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        if (Freshchat.isFreshchatNotification(remoteMessage)) {
            Freshchat.handleFcmMessage(this, remoteMessage)
        } else {
            val nc: NotificationObject = Gson().fromJson(
                Gson().toJson(remoteMessage.data),
                NotificationObject::class.java
            )
            if (BuildConfig.DEBUG) {
                Timber.tag(FirebaseNotificationService::class.java.simpleName).e(
                    Gson().toJson(remoteMessage.data)
                )
            }
            sendNotification(nc)
        }
    }

    private fun sendNotification(notificationObject: NotificationObject) {
        executor.execute {
            EngagementNetworkHelper.receivedNotification(notificationObject)
            val style = NotificationCompat.BigTextStyle()
            style.setBigContentTitle(notificationObject.contentTitle)
            style.bigText(notificationObject.contentText)
            style.setSummaryText(notificationObject.contentText)

            val intent = getIntentAccordingAction(
                notificationObject,
                notificationObject.action,
                notificationObject.actionData
            )

            intent?.run {
                intent.putExtra(NOTIFICATION_ID, notificationObject.id)
                val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()
                val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val pendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    uniqueInt,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

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
                        .setColor(
                            ContextCompat.getColor(
                                this@FirebaseNotificationService,
                                R.color.colorAccent
                            )
                        )
                        .setWhen(System.currentTimeMillis())
                notificationBuilder.setDefaults(Notification.DEFAULT_ALL)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    notificationBuilder.priority = NotificationManager.IMPORTANCE_HIGH
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
                notificationManager.notify(uniqueInt, notificationBuilder.build())
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
            NotificationAction.ACTION_OPEN_CONVERSATION, NotificationAction.ACTION_OPEN_COURSE_REPORT -> {
                if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
                    return null
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    importance = NotificationManager.IMPORTANCE_HIGH
                }
                notificationChannelId = action.name
                val obj: InboxEntity? = AppObjectController.appDatabase.courseDao()
                    .chooseRegisterCourseMinimal(actionData!!)
                obj?.run {
                    WorkMangerAdmin.updatedCourseForConversation(this.conversation_id)
                }

                if (null != obj) {
                    notificationChannelId = obj.conversation_id
                    notificationChannelName = obj.course_name
                    val rIntnet = Intent(applicationContext, isNotificationCrash()).apply {
                        putExtra(UPDATED_CHAT_ROOM_OBJECT, obj)
                        putExtra(ACTION_TYPE, action)
                        putExtra(HAS_NOTIFICATION, true)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    if (NotificationAction.ACTION_OPEN_COURSE_REPORT == action) {
                        rIntnet.putExtra(HAS_COURSE_REPORT, true)
                    }
                    notificationObject.extraData?.let {
                        rIntnet.putExtra(QUESTION_ID, it["question_id"])
                    }
                    rIntnet
                } else {
                    returnDefaultIntent()
                }
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
            NotificationAction.ACTION_OPEN_QUESTION -> {
                return null
            }
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
            else -> {
                return null
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
}