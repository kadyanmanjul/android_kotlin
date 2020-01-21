package com.joshtalks.joshskills.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.joshtalks.joshskills.R
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


const val FCM_TOKEN = "fcmToken"
const val FCM_ID = "fcmId"
const val HAS_NOTIFICATION = "has_notification"
const val NOTIFICATION_ID = "notification_id"

class FirebaseNotificationService : FirebaseMessagingService() {

    private var notificationChannelId = "101111"
    private var notificationId = 1
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
        val nc: NotificationObject = Gson().fromJson<NotificationObject>(
            Gson().toJson(remoteMessage.data),
            NotificationObject::class.java
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

            /*val stackBuilder: TaskStackBuilder = TaskStackBuilder.create(this@FirebaseNotificationService)
            stackBuilder.addParentStack(LauncherActivity::class.java)
            stackBuilder.addNextIntent(intent)

            val pendingIntent = stackBuilder.getPendingIntent(
                uniqueInt,
                PendingIntent.FLAG_UPDATE_CURRENT
            )*/
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
                notificationChannel.lightColor = Color.RED
                notificationChannel.enableVibration(true)
                notificationChannel.vibrationPattern =
                    longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                notificationBuilder.setChannelId(notificationChannelId)
                notificationManager.createNotificationChannel(notificationChannel)
            }
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }

    private suspend fun getIntentAccordingAction(
        notificationObject: NotificationObject,
        action: String?,
        actionData: String?
    ): Intent {
        if (action.isNullOrEmpty().not()) {
            if (ACTION_OPEN_TEST == action) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    importance = NotificationManager.IMPORTANCE_HIGH
                }
                notificationChannelId = ACTION_OPEN_TEST
                return Intent(applicationContext, PaymentActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    putExtra(COURSE_ID, actionData)
                }
            } else if (ACTION_OPEN_CONVERSATION == action) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    importance = NotificationManager.IMPORTANCE_HIGH
                }
                notificationChannelId = ACTION_OPEN_CONVERSATION
                val obj: InboxEntity? = AppObjectController.appDatabase.courseDao()
                    .chooseRegisterCourseMinimal(actionData!!)
                obj?.run { WorkMangerAdmin.updatedCourseForConversation(this.conversation_id) }

                if (obj != null) {
                    notificationChannelId = obj.conversation_id
                    notificationId = obj.conversation_id.hashCode()
                    notificationChannelName = obj.course_name
                    return Intent(applicationContext, ConversationActivity::class.java).apply {
                        putExtra(CHAT_ROOM_OBJECT, obj)
                        putExtra(HAS_NOTIFICATION, true)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                }
            } else if (ACTION_OPEN_COURSE_EXPLORER == action) {
                notificationChannelId = ACTION_OPEN_COURSE_EXPLORER
                return Intent(applicationContext, CourseExploreActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }

            } else if (ACTION_OPEN_URL == action) {
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


            } else if (ACTION_OPEN_CONVERSATION_LIST == action) {
                notificationChannelId = ACTION_OPEN_CONVERSATION_LIST
                return Intent(applicationContext, InboxActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(HAS_NOTIFICATION, true)
                    putExtra(NOTIFICATION_ID, notificationObject.id)
                }
            }
        }
        /*else if (ACTION_OPEN_DIALOG == action) {
    }*/
        return Intent(applicationContext, LauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }


}
