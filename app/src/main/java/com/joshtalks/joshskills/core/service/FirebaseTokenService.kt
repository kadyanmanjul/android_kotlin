package com.joshtalks.joshskills.core.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.joshtalks.joshskills.core.PrefManager
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.NotificationObject
import com.joshtalks.joshskills.ui.chat.ConversationActivity


const val FCM_TOKEN = "fcmToken"
const val FCM_ID = "fcmId"

class FirebaseTokenService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PrefManager.put(FCM_TOKEN, token)
        FCMTokenManager.pushToken()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val nc:NotificationObject = Gson().fromJson<NotificationObject>(
            Gson().toJson(remoteMessage.data),
            NotificationObject::class.java
        )
        sendNotification(nc)
    }

    private fun sendNotification(notificationObject: NotificationObject) {


        val style = NotificationCompat.BigTextStyle()
        style.bigText(notificationObject.contentTitle)
        style.setBigContentTitle(notificationObject.contentText)
        style.setSummaryText(notificationObject.contentText)

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val intent = Intent(applicationContext, ConversationActivity::class.java)
        intent.flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val NOTIFICATION_CHANNEL_ID = "101"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @SuppressLint("WrongConstant") val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Notification",
                NotificationManager.IMPORTANCE_MAX
            )

            //Configure Notification Channel
            notificationChannel.description = "Josh Skill Notifications"
            notificationChannel.enableLights(true)
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setTicker(notificationObject.ticker)
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setContentTitle(notificationObject.contentTitle)
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setContentText(notificationObject.contentText)
            .setContentIntent(pendingIntent)
            .setStyle(style)
            .setColor(Color.parseColor("#f36273"))
            .setWhen(System.currentTimeMillis())
            .setPriority(NotificationManager.IMPORTANCE_HIGH)

        notificationManager.notify(1, notificationBuilder.build())


    }

}
