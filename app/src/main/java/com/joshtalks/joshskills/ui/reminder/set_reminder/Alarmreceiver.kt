package com.joshtalks.joshskills.ui.reminder.set_reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey


class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        context.showNotificationWithFullScreenIntent(context)
    }

    fun Context.showNotificationWithFullScreenIntent(
        context: Context,
        channelId: String = CHANNEL_ID,
        title: String = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.REMINDER_NOTIFICATION_TITLE),
        description: String = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.REMINDER_NOTIFICATION_DESCRIPTION)


    ) {
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.arrow_up_float)
            .setContentTitle(title)
            .setContentText(description)
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.ic_status_bar_notification).setColor(
                ContextCompat.getColor(
                    context,
                    R.color.colorAccent
                )
            )
            .setFullScreenIntent(getFullScreenIntent(), true)


        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        with(notificationManager) {
            buildChannel()

            val notification = builder.build()

            notify(0, notification)
        }
    }

    private fun NotificationManager.buildChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Example Notification Channel" // todo move to string file
            val descriptionText = "This is used to demonstrate the Full Screen Intent"  // todo move to string file
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            createNotificationChannel(channel)
        }
    }

    private fun Context.getFullScreenIntent(): PendingIntent {

        val destination =
            AlarmNotifierActivity::class.java

        val intent = Intent(this, destination)

        // flags and request code are 0 for the purpose of demonstration
        return PendingIntent.getActivity(this, 0, intent, 0)
    }

    companion object {
        private const val CHANNEL_ID = "channelId"
    }

}

