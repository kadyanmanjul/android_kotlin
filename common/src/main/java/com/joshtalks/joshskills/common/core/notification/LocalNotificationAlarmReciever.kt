package com.joshtalks.joshskills.common.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.Navigator
import com.joshtalks.joshskills.common.core.SplashContract
import com.joshtalks.joshskills.common.core.analytics.LocalNotificationDismissEventReceiver
import com.joshtalks.joshskills.common.core.service.NOTIFICATION_DELAY
import com.joshtalks.joshskills.common.core.service.NOTIFICATION_TEXT_TEXT
import com.joshtalks.joshskills.common.core.service.NOTIFICATION_TITLE_TEXT
import com.joshtalks.joshskills.common.repository.local.model.NotificationChannelData
import com.joshtalks.joshskills.common.repository.local.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocalNotificationAlarmReciever : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val index = intent.getIntExtra("id", 0)
        if ("android.intent.action.BOOT_COMPLETED" == intent.action) {
            CoroutineScope(Dispatchers.IO).launch {
                context.showNotificationWithFullScreenIntent(context, notificationIndex = index)
            }
        } else
            CoroutineScope(Dispatchers.IO).launch {
                context.showNotificationWithFullScreenIntent(context, notificationIndex = index)
            }
    }

    private fun Context.showNotificationWithFullScreenIntent(
        context: Context,
        channelId: String = CHANNEL_ID,
        notificationIndex: Int
    ) {

        val delay = NOTIFICATION_DELAY.get(notificationIndex)
        val description = NOTIFICATION_TEXT_TEXT.get(notificationIndex)
        var title: String? = null
        if (notificationIndex == 0) {
            title =
                NOTIFICATION_TITLE_TEXT.get(notificationIndex)
                    .replace("%num", (24..78).random().toString())
                    .replace("%name", User.getInstance().firstName.toString())
        } else {
            title = NOTIFICATION_TITLE_TEXT.get(notificationIndex)
        }

        AppObjectController.navigator.with(applicationContext).navigate(object : SplashContract {
            override val navigator = AppObjectController.navigator
        })

        val intent: Intent? = null /*Intent(applicationContext, LauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(HAS_NOTIFICATION, true)
            putExtra(HAS_LOCAL_NOTIFICATION, true)
        }*/

        intent.run {
            val activityList = arrayOf(this)
            val uniqueInt = (System.currentTimeMillis() and 0xfffffff).plus(delay).toInt()

            val pendingIntent = PendingIntent.getActivities(
                context,
                uniqueInt, activityList,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                else
                    PendingIntent.FLAG_UPDATE_CURRENT
            )
            val style = NotificationCompat.BigTextStyle()
            style.setBigContentTitle(title)
            style.bigText(description)
            style.setSummaryText("")

            val builder = NotificationCompat.Builder(context, channelId)
                .setStyle(style)
                .setDefaults(Notification.DEFAULT_ALL)
                .setColor(
                    ContextCompat.getColor(context, R.color.primary_500)
                )
                .setSmallIcon(R.drawable.ic_status_bar_notification)
                .setContentTitle(title)
                .setContentIntent(pendingIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .setContentText(description)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_status_bar_notification).setColor(
                    ContextCompat.getColor(context, R.color.primary_500)
                )
            val dismissIntent =
                Intent(applicationContext, LocalNotificationDismissEventReceiver::class.java)
            val dismissPendingIntent: PendingIntent =
                PendingIntent.getBroadcast(
                    applicationContext,
                    1,
                    dismissIntent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE
                    else
                        0
                )
            builder.setDeleteIntent(dismissPendingIntent)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            with(notificationManager) {
                buildChannel()
                val notification = builder.build()
                notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun NotificationManager.buildChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = NotificationChannelData.LOCAL_NOTIFICATIONS.type
            val descriptionText = "This is for josh local notification"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            createNotificationChannel(channel)
        }
    }

    companion object {
        private val CHANNEL_ID = NotificationChannelData.LOCAL_NOTIFICATIONS.id
        const val NOTIFICATION_ID = 0
    }
}
