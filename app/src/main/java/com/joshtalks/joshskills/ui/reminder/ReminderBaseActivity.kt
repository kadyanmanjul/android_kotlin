package com.joshtalks.joshskills.ui.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.ui.reminder.set_reminder.AlarmReceiver
import java.util.*

open class ReminderBaseActivity : CoreJoshActivity() {

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
            getSystemService(Context.ALARM_SERVICE) as AlarmManager
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

        when (frequency) {
            ReminderFrequency.EVERYDAY -> {
                createAlarm(
                    pendingIntent,
                    alarmCalendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY
                )
            }
            ReminderFrequency.WEEKDAYS -> {
                alarmCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                createAlarm(
                    pendingIntent,
                    alarmCalendar.timeInMillis,
                    7 * 24 * 60 * 60 * 1000
                )

                alarmCalendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
                createAlarm(
                    pendingIntent,
                    alarmCalendar.timeInMillis,
                    7 * 24 * 60 * 60 * 1000
                )

                alarmCalendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
                createAlarm(
                    pendingIntent,
                    alarmCalendar.timeInMillis,
                    7 * 24 * 60 * 60 * 1000
                )

                alarmCalendar.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY)
                createAlarm(
                    pendingIntent,
                    alarmCalendar.timeInMillis,
                    7 * 24 * 60 * 60 * 1000
                )

                alarmCalendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
                createAlarm(
                    pendingIntent,
                    alarmCalendar.timeInMillis,
                    7 * 24 * 60 * 60 * 1000
                )

            }
            ReminderFrequency.WEEKENDS -> {
                alarmCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                createAlarm(
                    pendingIntent,
                    alarmCalendar.timeInMillis,
                    7 * 24 * 60 * 60 * 1000
                )

                alarmCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
                createAlarm(
                    pendingIntent,
                    alarmCalendar.timeInMillis,
                    7 * 24 * 60 * 60 * 1000
                )

            }
        }
    }

    private fun createAlarm(pendingIntent: PendingIntent, triggerTime: Long, intervalMillis: Long) {
        println("triggertime $triggerTime")
        val alarmManager: AlarmManager =
            getSystemService(Context.ALARM_SERVICE) as AlarmManager
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

    fun getAlarmPendingIntent(
        reminderId: Int,
        flat: Int = PendingIntent.FLAG_UPDATE_CURRENT
    ): PendingIntent {
        val intent = Intent(applicationContext, AlarmReceiver::class.java)
        intent.putExtra("id", reminderId)
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        return PendingIntent.getBroadcast(
            applicationContext,
            reminderId,
            intent,
            flat
        )
    }

}
