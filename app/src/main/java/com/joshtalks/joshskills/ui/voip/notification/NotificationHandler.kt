package com.joshtalks.joshskills.ui.voip.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.ui.voip.WebRtcActivity


class NotificationHandler(private val applicationContext: Context) : NotificationInterface {
    override fun addNotification(
        notificationType: NotificationType,
        notificationData: NotificationData
    ): Int {
        return NotificationGenerator(applicationContext).initiateNotification(
            notificationData,
            notificationType
        )
    }

    override fun removeNotification(notificationId: Int) {
        NotificationGenerator(applicationContext).removeNotification(notificationId)
    }

    override fun getNotificationObject(
        notificationType: NotificationType,
        notificationData: NotificationData
    ): NotificationDetails {
        return NotificationGenerator(applicationContext).getNotificationObject(
            notificationType,
            notificationData
        )
    }

    override fun updateNotification(
        notificationId: Int,
        notificationType: NotificationType,
        notificationData: NotificationData
    ) {
        NotificationGenerator(applicationContext).updateNotification(
            notificationId,
            notificationData,
            notificationType
        )
    }
}



private class NotificationGenerator(private val applicationContext: Context) {
    private lateinit var notificationBuilder: NotificationCompat.Builder

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//           creating channel for incoming calls
            val name = applicationContext.getString(R.string.channel_name_calls)
            val descriptionText = applicationContext.getString(R.string.channel_calls_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                applicationContext.getString(R.string.CHANNEL_ID_CALLS),
                name,
                importance
            ).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)


            //           creating channel for normal
            val name1 = applicationContext.getString(R.string.channel_name_normal)
            val descriptionText1 = applicationContext.getString(R.string.channel_others_description)
            val importance1 = NotificationManager.IMPORTANCE_DEFAULT
            val channel1 = NotificationChannel(
                applicationContext.getString(R.string.CHANNEL_ID_OTHERS),
                name1,
                importance1
            ).apply {
                description = descriptionText1
            }
            val notificationManager1: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager1.createNotificationChannel(channel1)

        }
    }

    private fun getNotificationBuiltObj(
        notificationObj: NotificationData,
        notificationType: NotificationType
    ): NotificationCompat.Builder {
        when (notificationType) {

            NotificationType.Urgent -> {

                //        for Incoming Calls
                val pendingIntent: PendingIntent = getNotificationTapAction()
                val acceptPendingIntent: PendingIntent = getAcceptCallIntent()
                val rejectPendingIntent: PendingIntent = getRejectCallIntent()

                notificationBuilder =
                    NotificationCompat.Builder(
                        applicationContext,
                        applicationContext.getString(R.string.CHANNEL_ID_CALLS)
                    )
                        .setSmallIcon(R.drawable.common_google_signin_btn_icon_light_focused)
                        .setContentTitle(notificationObj.getCallDetails()[applicationContext.getString(R.string.notification_title)].toString())
                        .setContentText(notificationObj.getCallDetails()[applicationContext.getString(R.string.notification_content)].toString())
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setContentIntent(pendingIntent)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                        .setDefaults(NotificationCompat.DEFAULT_SOUND)
                        .addAction(0, applicationContext.getString(R.string.accept_call), acceptPendingIntent)
                        .addAction(0, applicationContext.getString(R.string.reject_call), rejectPendingIntent)
                        .setOngoing(true)
                        .setFullScreenIntent(pendingIntent, true)
                        .setAutoCancel(true)
            }

            NotificationType.Normal -> {

//            for normal notification
                val pendingIntent: PendingIntent = getNotificationTapAction()
                notificationBuilder =
                    NotificationCompat.Builder(
                        applicationContext,
                        applicationContext.getString(R.string.CHANNEL_ID_OTHERS)
                    )
                        .setSmallIcon(R.drawable.common_google_signin_btn_icon_light_focused)
                        .setContentTitle(notificationObj.getCallDetails()[applicationContext.getString(R.string.notification_title)].toString())
                        .setContentText(notificationObj.getCallDetails()[applicationContext.getString(R.string.notification_content)].toString())
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                        .setDefaults(NotificationCompat.DEFAULT_SOUND)
                        .setAutoCancel(false)
            }
        }
        return notificationBuilder
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getRejectCallIntent(): PendingIntent {
        val intent = Intent(applicationContext, WebRtcActivity::class.java).apply {
            action = ACTION_ACCEPT_CALL
        }
        return PendingIntent.getActivity(applicationContext, 10001, intent, 0)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getAcceptCallIntent(): PendingIntent {
        val intent = Intent(applicationContext, WebRtcActivity::class.java).apply {
            action = ACTION_REJECT_CALL
        }
        return PendingIntent.getActivity(applicationContext, 10001, intent, 0)
    }

    private fun getNotificationTapAction(): PendingIntent {
        val intent = Intent(applicationContext, WebRtcActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(applicationContext, 10001, intent, FLAG_UPDATE_CURRENT)
    }

    private fun showNotification(): Int {
        with(NotificationManagerCompat.from(applicationContext)) {
            val notificationId = System.currentTimeMillis().toInt()
            notify(notificationId, notificationBuilder.build())
            return notificationId
//            }

        }
    }

    fun initiateNotification(
        notificationObj: NotificationData,
        notificationType: NotificationType
    ): Int {
        getNotificationBuiltObj(notificationObj, notificationType)
        return showNotification()
    }

    fun updateNotification(
        notificationId: Int,
        notificationObj: NotificationData,
        notificationType: NotificationType
    ) {
        val notificationBuiltObj = getNotificationBuiltObj(notificationObj, notificationType)
        NotificationManagerCompat.from(applicationContext).notify(notificationId, notificationBuiltObj.build())
    }

    fun getNotificationObject(
        notificationType: NotificationType,
        notificationData: NotificationData
    ): NotificationDetails {
        val notificationBuiltObj = getNotificationBuiltObj(notificationData, notificationType)
        return NotificationDetails(notificationBuiltObj, System.currentTimeMillis().toInt())
    }

    fun removeNotification(notificationId: Int) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}