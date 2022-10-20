package com.joshtalks.joshskills.core.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationManagerCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.repository.local.model.NotificationChannelData
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.BuyPageActivity
import com.joshtalks.joshskills.ui.special_practice.utils.COUPON_CODE
import com.joshtalks.joshskills.ui.special_practice.utils.FLOW_FROM
import kotlinx.coroutines.*
import org.json.JSONObject
import timber.log.Timber

class StickyNotificationService : Service() {

    private val notificationId = 10206
    private var endTime: Long = 0L
    private var shouldUpdate = true
    private var serviceRunning = false
    private var job: Job? = null
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override fun onCreate() {
        super.onCreate()
        startForeground(notificationId, buildNotification(getPendingIntent()))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!serviceRunning) {
            serviceRunning = true
            val couponCode = intent?.extras?.getString("coupon_code") ?: "ENG10"
            endTime = intent?.extras?.getLong("expiry_time") ?: 3600000L
            val title = intent?.extras?.getString("sticky_title") ?: "Do you know?"
            val body = intent?.extras?.getString("sticky_body") ?: "You'll miss an offer if you don't click on this notification"

            updateJob(title, body, couponCode, endTime)

            if (intent?.extras?.getBoolean("start_from_inbox") == false) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = AppObjectController.utilsAPIService.updateNotificationStatus(
                            mapOf(Pair("coupon_code", couponCode))
                        )
                        shouldUpdate = true
                        endTime = (response["expiry_time"] as Double).toLong() * 1000L
                        updatePrefValue(endTime)
                        updateJob(title, body, couponCode, endTime)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        job?.cancel()
        stopForeground(true)
        stopSelf()
        super.onDestroy()
    }

    private fun getPendingIntent(code: String = EMPTY): PendingIntent {
        val notificationIntent = Intent(this, BuyPageActivity::class.java).apply {
            putExtra(FLOW_FROM, "Sticky Notification")
            putExtra(COUPON_CODE, code)
            putExtra(HAS_NOTIFICATION, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun buildNotification(pendingIntent: PendingIntent): Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannelData.UPDATES
        notificationBuilder = NotificationCompat.Builder(this, channel.id)
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Claim your coupon by clicking this notification")
            .setContentIntent(pendingIntent)
            .setDefaults(Notification.FLAG_ONGOING_EVENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.priority = PRIORITY_HIGH
        }

        val remoteView = getRemoteView()

        notificationBuilder.setCustomContentView(remoteView)
        if (Build.VERSION.SDK_INT >= 29) {
            notificationBuilder.apply {
                setCustomHeadsUpContentView(remoteView)
                setCustomBigContentView(remoteView)
                setCustomContentView(remoteView)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channel.id,
                channel.type,
                NotificationManager.IMPORTANCE_LOW
            )

            notificationBuilder.setChannelId(channel.id)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notification = notificationBuilder.build()
        notification.flags = Notification.FLAG_NO_CLEAR

        return notification
    }

    private fun updateNotification(title: String, body: String, coupon: String, time: Float, timeDiff: Long) {
        if (shouldUpdate) {
            notificationBuilder.setContentIntent(getPendingIntent(coupon))
            notificationBuilder.contentView.setTextViewText(R.id.notification_title, title)
            notificationBuilder.contentView.setTextViewText(R.id.notification_body, body)
            notificationBuilder.contentView.setChronometer(
                R.id.notification_timer,
                SystemClock.elapsedRealtime() + timeDiff,
                null,
                true
            )
            shouldUpdate = false
        }
        notificationBuilder.contentView.setProgressBar(R.id.notification_progress, 100, (100 - (time * 100)).toInt(), false)
        NotificationManagerCompat.from(this).notify(notificationId, notificationBuilder.build())
    }

    private fun updateJob(title: String, body: String, couponCode: String, endTime: Long) {
        job?.cancel()
        val offsetTime = PrefManager.getLongValue(SERVER_TIME_OFFSET, true)
        val timeDiff = endTime - System.currentTimeMillis().plus(offsetTime)
        job = CoroutineScope(Dispatchers.Main).launch {
            while (System.currentTimeMillis().plus(offsetTime) < endTime) {
                updateNotification(
                    title = title,
                    body = body,
                    coupon = couponCode,
                    ((endTime - System.currentTimeMillis().plus(offsetTime)).toFloat() / timeDiff),
                    timeDiff
                )
                if (System.currentTimeMillis().plus(offsetTime) > endTime)
                    onDestroy()
                delay(10000)
            }
            onDestroy()
        }
    }

    private fun updatePrefValue(endTime: Long) {
        try {
            val jsonObject = JSONObject(PrefManager.getStringValue(STICKY_COUPON_DATA))
            jsonObject.put("expiry_time", endTime.div(1000))
            PrefManager.put(STICKY_COUPON_DATA, jsonObject.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getRemoteView(): RemoteViews {
        val remoteView = RemoteViews(packageName, R.layout.coupon_code_notification)
        remoteView.setTextViewText(R.id.notification_title, "Sticky Notification")
        remoteView.setTextViewText(R.id.notification_body, "This is the notification body")
        remoteView.setChronometerCountDown(R.id.notification_timer, true)
        remoteView.setProgressBar(R.id.notification_progress, 100, 0, false)
        return remoteView
    }
}