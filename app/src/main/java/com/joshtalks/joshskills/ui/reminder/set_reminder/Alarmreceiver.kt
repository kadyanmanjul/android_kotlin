package com.joshtalks.joshskills.ui.reminder.set_reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Vibrator
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R


class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.hasExtra("id")) println("AlarmReceiver.onReceive " + intent.getStringExtra("id")) else println(
            "AlarmReceiver.onReceive no extra found"
        )
        Toast.makeText(
            context,
            "Broadcast received " + intent.action,
            Toast.LENGTH_SHORT
        ).show()
        println("Alarm Receiver.onReceive")
        MyWakeLock.acquire(context)
        val vibrator =
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(2 * 1000.toLong())
        getNotification(context)
        MyWakeLock.release()
    }

    private fun getNotification(context: Context) {
        val notificationManager =
            NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel: NotificationChannel? = null
            channel = NotificationChannel(
                context.packageName,
                "My Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
            val builder =
                Notification.Builder(context, context.packageName).setAutoCancel(true)
                    .setContentTitle("Joshskills alarm")
                    .setContentText("This is Joshskills alarm")
                    .setShowWhen(true)
                    .setSmallIcon(R.drawable.ic_status_bar_notification).setColor(
                        ContextCompat.getColor(
                            context,
                            R.color.colorAccent
                        )
                    )
            notificationManager.notify(101, builder.build())
        } else {
            val builder =
                NotificationCompat.Builder(context, "channel_01")
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentTitle("Joshskills alarm")
                    .setContentText("Joshskills alarm")
                    .setSmallIcon(R.drawable.ic_status_bar_notification).setColor(
                        ContextCompat.getColor(
                            context,
                            R.color.colorAccent
                        )
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            notificationManager.notify(101, builder.build())
        }
        val i = Intent()
        i.setClassName("com.joshtalks.joshskills", AlarmNotifierActivity::class.java.name)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(i)
    }
}

