package com.joshtalks.joshskills.core.notification


import android.annotation.SuppressLint
import android.app.*
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.liveRooms.ConversationLiveRoomActivity
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.ui.voip.NotificationId
import org.json.JSONObject
import java.util.*


class HeadsUpNotificationService : Service() {
    private val timer = Timer()
    protected var mNotificationManager: NotificationManager? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    @SuppressLint("InvalidWakeLockTag")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null && intent.extras != null
            && intent.getStringExtra(ConfigKey.CALL_RESPONSE_ACTION_KEY) != null &&
            (intent.getStringExtra(ConfigKey.CALL_RESPONSE_ACTION_KEY) == ConfigKey.CALL_CANCEL_ACTION
                    || intent.getStringExtra(ConfigKey.CALL_RESPONSE_ACTION_KEY) == ConfigKey.CALL_RECEIVE_ACTION)
        ) {
            if (intent.getStringExtra(ConfigKey.CALL_RESPONSE_ACTION_KEY) == ConfigKey.CALL_CANCEL_ACTION) {

                mNotificationManager?.cancelAll()
                stopForeground(true)
                stopSelf()
            } else {

                mNotificationManager?.cancelAll()
                intent.getStringExtra(ConfigKey.INTENT_ROOM_ID)?.let {
                    startActivity(
                        ConversationLiveRoomActivity.getIntentForNotification(
                            this,
                            it,
                            arrayOf(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    )
                }
                stopForeground(true)
                stopSelf()
            }

        } else {
            try {
                var data: String? = null
                if (intent != null && intent.extras != null) {
                    data = intent.getStringExtra(ConfigKey.ROOM_DATA)
                }
                data?.let {
                    val obj = JSONObject(it)
                    val moderatorName = obj.getString("moderator_name")
                    val topic = obj.getString("topic")
                    val roomId = obj.getString("room_id")

                    if ( topic.isNullOrBlank() || moderatorName.isNullOrBlank()) {
                        stopForeground(true)
                        stopSelf()
                    } else {
                        addNotification(
                            moderatorName,
                            topic,
                            roomId,
                            intent
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return START_STICKY
    }

    private fun addNotification(name: String, topic: String, roomId: String, intent: Intent?) {
        val notificationBuilder = incomingCallNotification(roomId, intent, name, topic)

        var incomingCallNotification: Notification? = null
        if (notificationBuilder != null) {
            incomingCallNotification = notificationBuilder.build()
        }
        startForeground(9999, incomingCallNotification)
        val task: TimerTask = object : TimerTask() {
            override fun run() {
                val notificationManager =
                    applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(9999)
                val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                sendBroadcast(it)
                stopSelf()
            }
        }
        timer.schedule(task, 120000)
    }

    private fun canHeadsUpNotification(): Boolean {
        if (Build.VERSION.SDK_INT >= 29) { //  if (Build.VERSION.SDK_INT >= 29 && JoshApplication.isAppVisible.not()) {
            return true
        }
        return false
    }


    private fun incomingCallNotification(
        roomId: String,
        intent: Intent?,
        name: String,
        topic: String
    ): NotificationCompat.Builder {

        val importance =
            if (canHeadsUpNotification()) NotificationCompat.PRIORITY_HIGH else NotificationManager.IMPORTANCE_LOW

        val builder = NotificationCompat.Builder(this, NotificationId.ROOM_NOTIFICATION_CHANNEL)
            .setContentTitle(getString(R.string.room_title))
            .setContentText(getString(R.string.room_title))
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setChannelId(NotificationId.ROOM_NOTIFICATION_CHANNEL)
            .setAutoCancel(false)
            .setColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.colorPrimary
                )
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationId.ROOM_NOTIFICATION_CHANNEL,
                NotificationId.ROOM_NOTIFICATION_CHANNEL,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = NotificationId.ROOM_NOTIFICATION_CHANNEL
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.setSound(
                Uri.parse("android.resource://" + application.packageName + "/" + R.raw.wrong_answer),
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(AudioManager.STREAM_RING)
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build()
            )
            Objects.requireNonNull(
                application.getSystemService(
                    NotificationManager::class.java
                )
            ).createNotificationChannel(channel)
        } else {
            builder.setSound(null, AudioManager.STREAM_RING)
        }

        val answerActionIntent =
            Intent(AppObjectController.joshApplication, HeadsUpNotificationService::class.java)

        answerActionIntent.apply {
            putExtra(
                ConfigKey.CALL_RESPONSE_ACTION_KEY,
                ConfigKey.CALL_RECEIVE_ACTION
            )
            putExtra(
                ConfigKey.INTENT_ROOM_ID,
                roomId
            )
            action = "RECEIVE_CALL"
        }


        val answerPendingIntent = PendingIntent.getService(
            this,
            112,
            answerActionIntent,
            FLAG_UPDATE_CURRENT
        )

        val declineActionIntent =
            Intent(AppObjectController.joshApplication, HeadsUpNotificationService::class.java)
        declineActionIntent.apply {
            putExtra(
                ConfigKey.CALL_RESPONSE_ACTION_KEY,
                ConfigKey.CALL_CANCEL_ACTION
            )
            putExtra(
                ConfigKey.INTENT_ROOM_ID,
                roomId
            )

            action = "CANCEL_CALL"
        }

        val declinePendingIntent: PendingIntent =
            PendingIntent.getService(
                this,
                112,
                declineActionIntent,
                FLAG_UPDATE_CURRENT
            )

        builder.addAction(
            NotificationCompat.Action(
                R.drawable.ic_call_end,
                getActionText(R.string.dismiss, R.color.error_color),
                declinePendingIntent
            )
        )

        builder.addAction(
            NotificationCompat.Action(
                R.drawable.ic_pick_call,
                getActionText(R.string.join, R.color.action_color),
                answerPendingIntent
            )
        )

        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val customView = getRemoteViews(isFavorite = true, name, topic)

        customView.setOnClickPendingIntent(R.id.answer_btn, answerPendingIntent)
        customView.setOnClickPendingIntent(R.id.decline_btn, declinePendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.priority = importance
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVibrate(LongArray(0))
            builder.setCategory(Notification.CATEGORY_CALL)
            builder.setOngoing(true)
        }
        if (canHeadsUpNotification()) {
            builder.setCustomHeadsUpContentView(customView)
            builder.setCustomBigContentView(customView)
            builder.setCustomContentView(customView)
        }
        builder.setShowWhen(false)
        return builder
    }

    private fun getRemoteViews(isFavorite: Boolean, name: String, topic: String): RemoteViews {
        val layout = if (isFavorite) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                R.layout.conversion_rooms_custom_notification
            } else {
                R.layout.conversion_rooms_custom_notification
            }
        } else {
            R.layout.call_notification
        }
        val customView = RemoteViews(packageName, layout)
        customView.setTextViewText(
            R.id.name,
            getString(R.string.convo_notification_title, name, topic)
        )
        return customView
    }

    private fun getActionText(@StringRes stringRes: Int, @ColorRes colorRes: Int): Spannable {
        val spannable: Spannable = SpannableString(getText(stringRes))
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, colorRes)),
            0,
            spannable.length,
            0
        )
        return spannable
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        timer.cancel()
    }

}