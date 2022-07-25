package com.joshtalks.joshskills.voip.notification

import android.annotation.SuppressLint
import android.app.*
import android.app.Notification.DEFAULT_SOUND
import android.app.Notification.DEFAULT_VIBRATE
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.joshtalks.joshskills.voip.R
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.getServiceNotificationIntent
import com.joshtalks.joshskills.base.model.NotificationData as Data

internal class NotificationGenerator {
    private lateinit var notificationBuilder : NotificationCompat.Builder
    private val context: Application?= Utils.context
    private val notificationManager by lazy {
        context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createChannelForCalls()
        createChannelForService()
    }

    private fun createChannelForCalls() {
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
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun createChannelForService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //           creating channel for normal
            val name1 = context?.getString(R.string.channel_name_normal)
            val descriptionText1 = context?.getString(R.string.channel_others_description)
            val importance1 = NotificationManager.IMPORTANCE_MIN
            val channel1 = NotificationChannel(
                context?.getString(R.string.CHANNEL_ID_OTHERS),
                name1,
                importance1
            ).apply {
                description = descriptionText1
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
            notificationManager.createNotificationChannel(channel1)
        }
    }

    private fun getNotificationBuiltObj(
        notificationObj: NotificationData?,
        remoteView: RemoteViews?,
        notificationPriority: NotificationPriority
    ): NotificationCompat.Builder {

        val notificationActivity="com.joshtalks.joshskills.ui.voip.new_arch.ui.views.IncomingNotificationActivity"
        val callingActivity = Intent()
        callingActivity.apply {
            if (context != null) {
                setClassName(context,notificationActivity)
            }
        }
        val pendingIntent=PendingIntent.getActivity(context,(System.currentTimeMillis() and 0xfffffff).toInt(),callingActivity, PendingIntent.FLAG_UPDATE_CURRENT)

        when(notificationPriority) {
            NotificationPriority.High-> {
                notificationBuilder =
                    NotificationCompat.Builder(
                        context!!,
                        context.getString(R.string.CHANNEL_ID_CALLS)
                    )
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setFullScreenIntent(pendingIntent,true)
                        .setDefaults(NotificationCompat.FLAG_ONGOING_EVENT)
                        .setDefaults(DEFAULT_SOUND)
                        .setDefaults(DEFAULT_VIBRATE)
                        .setOngoing(true)
                        .setAutoCancel(false)
                        .setShowWhen(false)
            }
            NotificationPriority.Low-> {
                notificationBuilder =
                    NotificationCompat.Builder(context!!,
                        context.getString(R.string.CHANNEL_ID_OTHERS))
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .setSilent(true)
                        .setOnlyAlertOnce(true)
                        .setOngoing(false)
                        .setShowWhen(true)
            }
        }

        notificationBuilder
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setChannelId(context.getString(R.string.CHANNEL_ID_CALLS))
//            .color = ContextCompat.getColor(context, )

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
        return NotificationBuiltObj(
            notificationId,
            getNotificationBuiltObj(
                remoteView = remoteView,
                notificationObj = notificationData,
                notificationPriority=notificationPriority
            )
        )
    }

    fun removeNotification(notificationId: Int) {
        val notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    fun updateTitle(title:String,notificationBuiltObj: NotificationBuiltObj) {
        notificationBuiltObj.notificationBuilder.setContentTitle(title)
        NotificationManagerCompat.from(context!!).notify(notificationBuiltObj.id, notificationBuiltObj.notificationBuilder.build())
    }

    fun updateContent(content:String,notificationBuiltObj: NotificationBuiltObj) {
        notificationBuiltObj.notificationBuilder.setContentText(content)
        NotificationManagerCompat.from(context!!).notify(notificationBuiltObj.id, notificationBuiltObj.notificationBuilder.build())
    }

    fun connected(username : String, notificationBuiltObj: NotificationBuiltObj, onTap: PendingIntent, onNegativeAction : PendingIntent) {
        notificationBuiltObj.notificationBuilder.setContentTitle(username)
        notificationBuiltObj.notificationBuilder.setContentText("Ongoing p2p call")
        notificationBuiltObj.notificationBuilder.setSilent(true)
        notificationBuiltObj.notificationBuilder.setContentIntent(onTap)
        notificationBuiltObj.notificationBuilder.setUsesChronometer(true)
        notificationBuiltObj.notificationBuilder.setWhen(System.currentTimeMillis())
        notificationBuiltObj.notificationBuilder.setShowWhen(true)
        notificationBuiltObj.notificationBuilder.setOnlyAlertOnce(true)
        notificationBuiltObj.notificationBuilder.addAction(0, "Hang up", onNegativeAction)
        NotificationManagerCompat.from(context!!).notify(notificationBuiltObj.id, notificationBuiltObj.notificationBuilder.build())
    }

    fun searching(notificationBuiltObj: NotificationBuiltObj) {
        notificationBuiltObj.notificationBuilder.setContentTitle("Connecting to Practice Partner")
        notificationBuiltObj.notificationBuilder.setContentText("Connecting...")
        notificationBuiltObj.notificationBuilder.setSilent(true)
        notificationBuiltObj.notificationBuilder.setContentIntent(null)
        notificationBuiltObj.notificationBuilder.setOnlyAlertOnce(true)
        NotificationManagerCompat.from(context!!).notify(notificationBuiltObj.id, notificationBuiltObj.notificationBuilder.build())
    }

    @SuppressLint("RestrictedApi")
    fun idle(notificationBuiltObj: NotificationBuiltObj, notificationData: Data) {
        notificationBuiltObj.notificationBuilder.setContentTitle(notificationData.title)
        notificationBuiltObj.notificationBuilder.setContentText(notificationData.subTitle)
        notificationBuiltObj.notificationBuilder.setSilent(true)
        notificationBuiltObj.notificationBuilder.setContentIntent(Utils.context?.getServiceNotificationIntent(notificationData))
        notificationBuiltObj.notificationBuilder.setUsesChronometer(false)
        notificationBuiltObj.notificationBuilder.setShowWhen(false)
        notificationBuiltObj.notificationBuilder.setOnlyAlertOnce(true)
        notificationBuiltObj.notificationBuilder.mActions.clear()
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