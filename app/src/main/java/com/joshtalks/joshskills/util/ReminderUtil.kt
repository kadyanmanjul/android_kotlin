package com.joshtalks.joshskills.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.joshtalks.joshskills.ui.reminder.set_reminder.AlarmReceiver
import java.util.*

class ReminderUtil(val context: Context) {
    companion object {
        enum class ReminderFrequency {
            EVERYDAY,
            WEEKDAYS,
            WEEKENDS
        }

        enum class ReminderStatus {
            ACTIVE,
            INACTIVE,
            DELETED
        }
    }

    fun deleteAlarm(
        pendingIntent: PendingIntent
    ) {
        val alarmManager =
            context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    fun setAlarm(
        frequency: ReminderFrequency,
        pendingIntent: PendingIntent,
        alarmHour: Int?,
        alarmMins: Int?
    ) {
        if (alarmHour == null || alarmMins == null)
            return
        val alarmCalendar: Calendar = Calendar.getInstance()
        alarmCalendar.set(Calendar.HOUR_OF_DAY, alarmHour)
        alarmCalendar.set(Calendar.MINUTE, alarmMins)
        alarmCalendar.set(Calendar.SECOND, 0)
        // Check if the Calendar time is in the past
        if (alarmCalendar.timeInMillis < System.currentTimeMillis()) {
            alarmCalendar.add(Calendar.DAY_OF_YEAR, 1) // it will tell to run to next day
        }
        createAlarm(
            pendingIntent,
            alarmCalendar.timeInMillis,
            AlarmManager.INTERVAL_DAY
        )
    }

    private fun createAlarm(pendingIntent: PendingIntent, triggerTime: Long, intervalMillis: Long) {
        val alarmManager: AlarmManager =
            context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT < 23) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                intervalMillis,
                pendingIntent
            )
        } else {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                intervalMillis,
                pendingIntent
            )
        }
    }

    private fun createAlarm(pendingIntent: PendingIntent, triggerTime: Long) {
        val alarmManager: AlarmManager =
            context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT < 23) {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun getAlarmPendingIntent(
        reminderId: Int,
        flag: Int = PendingIntent.FLAG_UPDATE_CURRENT
    ): PendingIntent {
        val intent = Intent(context.applicationContext, AlarmReceiver::class.java)
        intent.putExtra("id", reminderId)
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        return PendingIntent.getBroadcast(
            context.applicationContext,
            reminderId,
            intent,
            flag
        )
    }
}