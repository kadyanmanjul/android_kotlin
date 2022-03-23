package com.joshtalks.joshskills.voip.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.joshtalks.joshskills.voip.R

internal class NotificationGenerator(private val context: Context) {
    private lateinit var notificationBuilder: NotificationCompat.Builder

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//           creating channel for incoming calls
            val name = context.getString(R.string.channel_name_calls)
            val descriptionText = context.getString(R.string.channel_calls_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                context.getString(R.string.CHANNEL_ID_CALLS),
                name,
                importance
            ).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            //           creating channel for normal
            val name1 = context.getString(R.string.channel_name_normal)
            val descriptionText1 = context.getString(R.string.channel_others_description)
            val importance1 = NotificationManager.IMPORTANCE_DEFAULT
            val channel1 = NotificationChannel(
                context.getString(R.string.CHANNEL_ID_OTHERS),
                name1,
                importance1
            ).apply {
                description = descriptionText1
            }
            val notificationManager1: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager1.createNotificationChannel(channel1)
        }
    }

    private fun getNotificationBuiltObj(
        notificationObj: NotificationData?,
        remoteView: RemoteViews?
    ): NotificationCompat.Builder {

        notificationBuilder =
            NotificationCompat.Builder(
                context,
                context.getString(R.string.CHANNEL_ID_CALLS)
            )
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_light_focused)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setDefaults(NotificationCompat.DEFAULT_SOUND)
                .setOngoing(true)
                .setAutoCancel(true)

        if (notificationObj!=null) {
            notificationBuilder.setContentTitle(notificationObj.setTitle())
                .setContentText(notificationObj.setContent())
            notificationObj.setTapAction()?.let { notificationBuilder.setContentIntent(it) }
            notificationObj.setAction1()?.let { notificationBuilder.addAction(0,it.title,it.actionPendingIntent) }
            notificationObj.setAction2()?.let { notificationBuilder.addAction(0,it.title,it.actionPendingIntent) }
            notificationObj.setTapAction()?.let { notificationBuilder.setFullScreenIntent(it,true) }
        }
        if(remoteView!=null){
            notificationBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            notificationBuilder.setCustomContentView(remoteView)
            notificationBuilder.setCustomBigContentView(remoteView)
        }
        return notificationBuilder
    }

    private fun showNotification(): Int {
        with(NotificationManagerCompat.from(context)) {
            val notificationId = System.currentTimeMillis().toInt()
            notify(notificationId, notificationBuilder.build())
            return notificationId
        }
    }

    fun initiateNotification(
        remoteView: RemoteViews?,
        notificationData:NotificationData?
    ): Int {
        getNotificationBuiltObj(remoteView = remoteView,notificationObj = notificationData)
        return showNotification()
    }

    fun updateNotification(
        notificationId: Int,
        notificationData: NotificationData?,
        remoteView: RemoteViews?
    ) {
        val notificationBuiltObj = getNotificationBuiltObj(notificationData,remoteView)
        NotificationManagerCompat.from(context).notify(notificationId, notificationBuiltObj.build())
    }

    fun getNotificationObject(
        remoteView: RemoteViews?,
        notificationData:NotificationData?
    ): NotificationDetails {
        val notificationBuiltObj = getNotificationBuiltObj(notificationObj = notificationData,remoteView = remoteView)
        return NotificationDetails(notificationBuiltObj, System.currentTimeMillis().toInt())
    }

    fun removeNotification(notificationId: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}