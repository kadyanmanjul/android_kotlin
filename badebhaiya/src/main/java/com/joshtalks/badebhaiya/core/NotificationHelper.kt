package com.joshtalks.badebhaiya.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import com.google.firebase.messaging.FirebaseMessaging
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.feed.FeedActivity
import com.joshtalks.badebhaiya.notifications.NotificationLauncher
import com.joshtalks.badebhaiya.notifications.reminderNotification.ReminderNotificationManager
import com.joshtalks.badebhaiya.profile.ProfileFragment
import com.joshtalks.badebhaiya.repository.model.User
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.parcel.Parcelize
import timber.log.Timber
import javax.inject.Inject

@Parcelize
data class Notification(
    val title: String,
    val body: String,
    val id: Int,
    val userId: String,
    val type: NotificationType,
    val roomId: String,
    val speakerPicture: Bitmap?,
    val speakerName: String,
    val remainingTime: String? = null,
) : Parcelable {
    fun isSpeaker(): Boolean = userId == User.getInstance().userId
}

enum class NotificationType(val value: String) {
    REMINDER("Reminders"),
    LIVE("Live")
}

@AndroidEntryPoint
class NotificationHelper : BroadcastReceiver() {

    @Inject
    lateinit var notificationLauncher: NotificationLauncher

    companion object {
        const val NOTIFICATION_ID = "notification-id"
        const val NOTIFICATION = "notification"
        const val NOTIFICATION_BUNDLE = "notification_bundle"

        fun getNotificationIntent(context: Context, notification: Notification): Intent =
            Intent(context, NotificationHelper::class.java).apply {
                putExtra(
                    NOTIFICATION_BUNDLE,
                    bundleOf(NOTIFICATION to notification)
                )
            }.also {
                Log.d("NotificationHelper.kt", "YASH => getNotificationIntent: ${it.extras}")
            }

        fun createNotificationChannel(
            context: Context,
            importance: Int,
            showBadge: Boolean,
            name: String,
            description: String,
            enableLights: Boolean = true,
            enableVibration: Boolean = true
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(name, name, importance)
                channel.description = description
                channel.setShowBadge(showBadge)
                channel.enableLights(enableLights)
                channel.enableVibration(enableVibration)
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Notification agaya => ${intent.extras}")
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if(it.isComplete && it.isSuccessful) {
                FirebaseMessaging.getInstance().deleteToken()
            }
        }
//        Call this to check what happens when there's token change
//        FirebaseMessaging.getInstance().deleteToken()

        notificationLauncher.launchNotification(context, intent)

//        val id: Int = intent.getIntExtra(NOTIFICATION_ID, 0)
//        notificationManager.notify(id, notification)
    }
}