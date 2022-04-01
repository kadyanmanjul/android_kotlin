package com.joshtalks.joshskills.voip.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.voip.R
import com.joshtalks.joshskills.voip.Utils

internal class NotificationGenerator {
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private val context: Application?= Utils.context

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//           creating channel for incoming calls
            val name = context?.getString(R.string.channel_name_calls)
            val descriptionText = context?.getString(R.string.channel_calls_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                context?.getString(R.string.CHANNEL_ID_CALLS),
                name,
                importance
            ).apply {
                description = descriptionText
                enableLights(false)
                enableVibration(false)
            }
            val notificationManager: NotificationManager =
                context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager1: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager1.createNotificationChannel(channel1)
        }
    }

    private fun getNotificationBuiltObj(
        notificationObj: NotificationData?,
        remoteView: RemoteViews?,
        notificationPriority: NotificationPriority
    ): NotificationCompat.Builder {

        val destination="com.joshtalks.joshskills.ui.voip.CallActivity"
        val callingActivity = Intent()
        callingActivity.apply {
            if (context != null) {
                setClassName(context,destination)
            }
        }
        val pendingIntent=PendingIntent.getActivity(context,(System.currentTimeMillis() and 0xfffffff).toInt(),callingActivity, PendingIntent.FLAG_UPDATE_CURRENT)

        when(notificationPriority){
            NotificationPriority.High->{
                notificationBuilder =
                    NotificationCompat.Builder(
                        context!!,
                        context.getString(R.string.CHANNEL_ID_CALLS)
                    )
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setFullScreenIntent(pendingIntent,true)
                        .setDefaults(NotificationCompat.FLAG_ONGOING_EVENT)
                        .setOngoing(true)
                        .setAutoCancel(false)
                        .setShowWhen(false)
            }
            NotificationPriority.Low->{
                notificationBuilder =
                    NotificationCompat.Builder(
                        context!!,
                        context.getString(R.string.CHANNEL_ID_OTHERS)
                    )
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
                        .setDefaults(NotificationCompat.FLAG_SHOW_LIGHTS)
                        .setOngoing(false)
                        .setAutoCancel(true)
                        .setShowWhen(true)
            }
        }

        notificationBuilder
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setChannelId(context.getString(R.string.CHANNEL_ID_CALLS))
            .color = ContextCompat.getColor(context, R.color.colorPrimary)

        if (notificationObj!=null) {
            notificationBuilder.setContentTitle(notificationObj.setTitle())
                .setContentText(notificationObj.setContent())
            notificationObj.setTapAction()?.let { notificationBuilder.setContentIntent(it) }
            notificationObj.setAction1()?.let { notificationBuilder.addAction(0,it.title,it.actionPendingIntent) }
            notificationObj.setAction2()?.let { notificationBuilder.addAction(0,it.title,it.actionPendingIntent) }
        }
        if(remoteView!=null){
            notificationBuilder.setCustomContentView(remoteView)
            if (Build.VERSION.SDK_INT >= 29) {
                notificationBuilder.apply {
                    setCustomHeadsUpContentView(remoteView)
                    setCustomBigContentView(remoteView)
                    setCustomContentView(remoteView)
                }
            }
        }
        return notificationBuilder
    }

    fun initiateNotification(
        remoteView: RemoteViews?,
        notificationData:NotificationData?,
        notificationPriority:NotificationPriority
    ): NotificationBuiltObj {
        val notificationId = System.currentTimeMillis().toInt()
        return NotificationBuiltObj(notificationId,getNotificationBuiltObj(remoteView = remoteView,notificationObj = notificationData,notificationPriority=notificationPriority))
    }

    fun removeNotification(notificationId: Int) {
        val notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    fun updateTitle(title:String,notificationBuiltObj: NotificationBuiltObj){
        notificationBuiltObj.notificationBuilder.setContentTitle(title)
        NotificationManagerCompat.from(context!!).notify(notificationBuiltObj.id, notificationBuiltObj.notificationBuilder.build())
    }

    fun updateContent(content:String,notificationBuiltObj: NotificationBuiltObj){
        notificationBuiltObj.notificationBuilder.setContentText(content)
        NotificationManagerCompat.from(context!!).notify(notificationBuiltObj.id, notificationBuiltObj.notificationBuilder.build())
    }

    fun updateUI(remoteView: RemoteViews,notificationBuiltObj: NotificationBuiltObj){
        notificationBuilder.setCustomContentView(remoteView)
        if (Build.VERSION.SDK_INT >= 29) {
            notificationBuilder.apply {
                setCustomHeadsUpContentView(remoteView)
                setCustomBigContentView(remoteView)
                setCustomContentView(remoteView)
            }
        }
        NotificationManagerCompat.from(context!!).notify(notificationBuiltObj.id, notificationBuiltObj.notificationBuilder.build())
    }

    fun show(notificationBuiltObj: NotificationBuiltObj){
        NotificationManagerCompat.from(context!!).notify(notificationBuiltObj.id, notificationBuiltObj.notificationBuilder.build())
    }
}