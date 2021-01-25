package com.joshtalks.joshskills.ui.reminder.set_reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.joshtalks.joshskills.repository.local.model.NotificationChannelNames
import com.joshtalks.joshskills.util.ReminderUtil
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if ("android.intent.action.BOOT_COMPLETED" == intent.action) {
            CoroutineScope(Dispatchers.IO).launch {
                val reminders = AppDatabase.getDatabase(context)?.reminderDao()?.getRemindersList()
                val reminderUtil = ReminderUtil(context)
                reminders?.let {
                    it.forEach { reminderItem ->
                        val timeParts = reminderItem.reminderTime.split(":")
                        val hours = timeParts[0]
                        val mins = timeParts[1]
                        reminderUtil.setAlarm(
                            when (reminderItem.reminderFrequency) {
                                ReminderUtil.Companion.ReminderFrequency.EVERYDAY.name -> ReminderUtil.Companion.ReminderFrequency.EVERYDAY
                                ReminderUtil.Companion.ReminderFrequency.WEEKDAYS.name -> ReminderUtil.Companion.ReminderFrequency.WEEKDAYS
                                else -> ReminderUtil.Companion.ReminderFrequency.WEEKENDS
                            },
                            reminderUtil.getAlarmPendingIntent(reminderItem.id),
                            hours.toIntOrNull(),
                            mins.toIntOrNull()
                        )
                    }
                }
            }
        } else

            CoroutineScope(Dispatchers.IO).launch {
                val reminder = AppDatabase.getDatabase(context)?.reminderDao()
                    ?.getReminder(intent.getIntExtra("id", 0))
                reminder?.let {
                    val alarmCalendar = Calendar.getInstance()

                    if (reminder.status != ReminderUtil.Companion.ReminderStatus.ACTIVE.name)
                        return@launch
                    when (reminder.reminderFrequency) {
                        ReminderUtil.Companion.ReminderFrequency.WEEKDAYS.name -> {
                            if (alarmCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY ||
                                alarmCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY ||
                                alarmCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY ||
                                alarmCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY ||
                                alarmCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
                            )
                                context.showNotificationWithFullScreenIntent(context)
                        }
                        ReminderUtil.Companion.ReminderFrequency.WEEKENDS.name -> {
                            if (alarmCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                                alarmCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                            )
                                context.showNotificationWithFullScreenIntent(context)
                        }
                        else ->
                            context
                                .showNotificationWithFullScreenIntent(context)
                    }

                }
            }
    }

    private fun Context.showNotificationWithFullScreenIntent(
        context: Context,
        channelId: String = CHANNEL_ID,
        title: String = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.REMINDER_NOTIFICATION_TITLE),
        description: String = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.REMINDER_NOTIFICATION_DESCRIPTION)


    ) {
        val builder = NotificationCompat.Builder(this, channelId)
            .setFullScreenIntent(getFullScreenIntent(), true)
            .setSmallIcon(android.R.drawable.arrow_up_float)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setContentText(description)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.ic_status_bar_notification).setColor(
                ContextCompat.getColor(
                    context,
                    R.color.colorAccent
                )
            )
            .setSound(
                RingtoneManager.getActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_ALARM
                )
            )
        val dismissIntent =
            Intent(applicationContext, AlarmNotifDismissReceiver::class.java)
        val dismissPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(applicationContext, 1, dismissIntent, 0)

        builder.setDeleteIntent(dismissPendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        with(notificationManager) {
            buildChannel()

            val notification = builder.build()

            notify(NOTIFICATION_ID, notification)
            playRingtone(context)
        }
    }

    private fun NotificationManager.buildChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = NotificationChannelNames.OTHERS.type
            val descriptionText = "This is for josh alarm notification"
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
        private const val CHANNEL_ID = "josh_app_alarm_channel"
        const val NOTIFICATION_ID = 0
    }

    private fun playRingtone(context: Context) {
        val mAudioPlayer = com.joshtalks.joshskills.util.RingtoneManager.getInstance(context)
        mAudioPlayer?.playRingtone()

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(1000, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    pattern, 0
                )
            )
        } else {
            //deprecated in API 26
            vibrator.vibrate(pattern, 0)
        }
    }
}

