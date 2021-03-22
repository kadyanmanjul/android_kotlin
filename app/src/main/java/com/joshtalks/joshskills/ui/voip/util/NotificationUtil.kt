package com.joshtalks.joshskills.ui.voip.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CallType
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.core.textDrawableBitmap
import com.joshtalks.joshskills.core.urlToBitmap
import com.joshtalks.joshskills.repository.local.entity.practise.FavoriteCaller
import com.joshtalks.joshskills.repository.local.model.NotificationChannelNames
import com.joshtalks.joshskills.ui.voip.CALL_TYPE
import com.joshtalks.joshskills.ui.voip.CALL_USER_OBJ
import com.joshtalks.joshskills.ui.voip.RTC_IS_FAVORITE
import com.joshtalks.joshskills.ui.voip.RTC_PARTNER_ID
import com.joshtalks.joshskills.ui.voip.WebRtcActivity
import com.joshtalks.joshskills.ui.voip.extra.FullScreenActivity
import java.util.concurrent.TimeUnit


class NotificationUtil(val context: Context) {
    private var notificationChannelId = "101119"
    private var notificationChannelName = NotificationChannelNames.P2P.type
    private var channelIdPre = "miss_fcall"
    private val mNotificationManager: NotificationManager? =
        context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager?

    fun addMissCallPPNotification(id: Int) {
        JoshSkillExecutors.BOUNDED.submit {
            try {
                Thread.sleep(100)
            } catch (ex: Exception) {
            }
            val favoriteCaller =
                AppObjectController.appDatabase.favoriteCallerDao().getFavoriteCaller(id)
                    ?: return@submit
            removeNotification(id.hashCode())

            val data = HashMap<String, String?>().apply {
                put(RTC_IS_FAVORITE, "true")
            }

            val intent = Intent(context, WebRtcActivity::class.java).apply {
                putExtra(RTC_PARTNER_ID, id)
                putExtra(CALL_TYPE, CallType.FAVORITE_MISSED_CALL)
                putExtra(CALL_USER_OBJ, data)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(
                    "$channelIdPre$id",
                    notificationChannelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationChannel.enableLights(true)
                notificationChannel.enableVibration(true)
                notificationChannel.setBypassDnd(true)
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                try {
                    mNotificationManager?.createNotificationChannel(notificationChannel)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    return@submit
                }
            }
            val pendingIntent =
                PendingIntent.getActivity(context, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            val notificationBuilder = NotificationCompat.Builder(context, "$channelIdPre$id")
                .setDefaults(Notification.DEFAULT_ALL)
                .setTicker("Missed call")
                .setSmallIcon(R.drawable.ic_status_bar_notification)
                .setContentTitle(favoriteCaller.name)
                .setAutoCancel(true)
                .setContentText("Missed Practice Partner call")
                .setContentIntent(pendingIntent)
                .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                .setWhen(System.currentTimeMillis())
                .setTimeoutAfter(TimeUnit.DAYS.toMillis(2))
                .setSilent(true)

            notificationBuilder.setChannelId("$channelIdPre$id")

            notificationBuilder.addAction(
                R.drawable.ic_pick_call,
                context.getText(R.string.call_back),
                pendingIntent
            )

            notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            notificationBuilder.setFullScreenIntent(
                FullScreenActivity.getPendingIntent(context, 22),
                true
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationBuilder.priority = NotificationManager.IMPORTANCE_DEFAULT
            }
            notificationBuilder.setShowWhen(true)
            notificationBuilder.setAutoCancel(true)
            notificationBuilder.setLargeIcon(getAvatar(favoriteCaller))
            mNotificationManager?.notify(id.hashCode(), notificationBuilder.build())
        }
    }

    private fun getAvatar(favoriteCaller: FavoriteCaller): Bitmap? {
        return if (favoriteCaller.image == null) {
            favoriteCaller.name.substring(0, 2).textDrawableBitmap(width = 36, height = 36)
        } else {
            favoriteCaller.image.urlToBitmap(width = 36, height = 36)
        }
    }

    private fun removeNotification(id: Int) {
        mNotificationManager?.cancel(id.hashCode())
    }
}
