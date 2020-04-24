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
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ARG_PLACEHOLDER_URL
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.*
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.chat.CHAT_ROOM_OBJECT
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.launch.LauncherActivity
import com.joshtalks.joshskills.ui.payment.COURSE_ID
import com.joshtalks.joshskills.ui.payment.PaymentActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.random.Random

const val FCM_TOKEN = "fcmToken"
const val FCM_ID = "fcmId"
const val HAS_NOTIFICATION = "has_notification"
const val NOTIFICATION_ID = "notification_id"
const val HAS_COURSE_REPORT = "has_course_report"


class FirebaseNotificationService : FirebaseMessagingService() {

    private var notificationChannelId = "101111"
    private var notificationId = Random(1000).nextInt()
    private var notificationChannelName = "JoshTalksDefault"

    @RequiresApi(Build.VERSION_CODES.N)
    private var importance = NotificationManager.IMPORTANCE_DEFAULT

    //restart_last_conversation_time
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PrefManager.put(FCM_TOKEN, token)
        FCMTokenManager.pushToken()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val nc: NotificationObject = Gson().fromJson(
            Gson().toJson(remoteMessage.data),
            NotificationObject::class.java
        )
        Timber.i(
            FirebaseNotificationService::class.java.simpleName,
            Gson().toJson(remoteMessage.data)
        )
        sendNotification(nc)
    }

    private fun sendNotification(notificationObject: NotificationObject) {
        CoroutineScope(Dispatchers.IO).launch {
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
                    .setTimeoutAfter(60 * 60 * 24)
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

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && !notificationManager.isNotificationPolicyAccessGranted
                ) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                }
            } catch (ex: Exception) {
            }
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

    private suspend fun getIntentAccordingAction(
        notificationObject: NotificationObject,
        action: String?,
        actionData: String?
    ): Intent {
        if (action.isNullOrEmpty().not()) {
            if (ACTION_OPEN_TEST.equals(action, ignoreCase = true)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    importance = NotificationManager.IMPORTANCE_HIGH
                }
                notificationChannelId = ACTION_OPEN_TEST
                return Intent(applicationContext, PaymentActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(COURSE_ID, actionData)
                }
            } else if (ACTION_OPEN_CONVERSATION.equals(
                    action,
                    ignoreCase = true
                ) || ACTION_OPEN_COURSE_REPORT.equals(action, ignoreCase = true)
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    importance = NotificationManager.IMPORTANCE_HIGH
                }
                notificationChannelId = action ?: ""
                val obj: InboxEntity? = AppObjectController.appDatabase.courseDao()
                    .chooseRegisterCourseMinimal(actionData!!)
                obj?.run { WorkMangerAdmin.updatedCourseForConversation(this.conversation_id) }

                if (obj != null) {
                    notificationChannelId = obj.conversation_id
                    notificationChannelName = obj.course_name
                    val rIntnet =
                        Intent(applicationContext, ConversationActivity::class.java).apply {
                            putExtra(CHAT_ROOM_OBJECT, obj)
                            putExtra(HAS_NOTIFICATION, true)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    if (ACTION_OPEN_COURSE_REPORT.equals(action, ignoreCase = true)) {
                        rIntnet.putExtra(HAS_COURSE_REPORT, true)
                    }
                    return rIntnet

                }
            } else if (ACTION_OPEN_COURSE_EXPLORER.equals(action, ignoreCase = true)) {
                notificationChannelId = ACTION_OPEN_COURSE_EXPLORER
                return Intent(applicationContext, CourseExploreActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }

            } else if (ACTION_OPEN_URL.equals(action, ignoreCase = true)) {
                notificationChannelId = ACTION_OPEN_URL
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (actionData!!.trim().startsWith("http://").not()) {
                    intent.data = Uri.parse("http://" + actionData.replace("https://", "").trim())
                } else {
                    intent.data = Uri.parse(actionData.trim())
                }
                return intent


            } else if (ACTION_OPEN_CONVERSATION_LIST.equals(action, ignoreCase = true)) {
                notificationChannelId = ACTION_OPEN_CONVERSATION_LIST
                return Intent(applicationContext, InboxActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(HAS_NOTIFICATION, true)
                    putExtra(NOTIFICATION_ID, notificationObject.id)
                }
            } else if (ACTION_UPSELLING_POPUP.equals(action, ignoreCase = true)) {
                notificationChannelId = ACTION_UPSELLING_POPUP
                return Intent(applicationContext, InboxActivity::class.java).apply {
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
            } else if (ACTION_OPEN_REFERRAL.equals(action, ignoreCase = true)) {
                notificationChannelId = ACTION_OPEN_REFERRAL
                return Intent(applicationContext, ReferralActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
            }
        }
        return Intent(applicationContext, LauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }


}
