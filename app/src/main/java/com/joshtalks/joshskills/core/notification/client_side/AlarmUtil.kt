package com.joshtalks.joshskills.core.notification.client_side

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import com.joshtalks.joshskills.core.dateStartOfDay
import java.util.*

class AlarmUtil(val context: Context) {

    fun createAlarm(pendingIntent: PendingIntent, frequency: AlarmFrequency, executeAfter: Long) {
        when (frequency) {
            AlarmFrequency.AT -> setExactAlarmAt(pendingIntent, executeAfter)

            AlarmFrequency.ONCE -> setExactAlarm(pendingIntent, System.currentTimeMillis() + executeAfter)

            AlarmFrequency.HOURLY, AlarmFrequency.TWO_HOUR, AlarmFrequency.THREE_HOUR,
            AlarmFrequency.FOUR_HOUR, AlarmFrequency.SIX_HOUR, AlarmFrequency.DAILY,
            AlarmFrequency.TWO_DAY, AlarmFrequency.THREE_DAY ->
                setRepeatingAlarm(
                    pendingIntent, System.currentTimeMillis() + executeAfter, frequency
                )

            AlarmFrequency.DAILY_AT, AlarmFrequency.TWO_DAY_AT, AlarmFrequency.THREE_DAY_AT,
            AlarmFrequency.FOUR_DAY_AT, AlarmFrequency.WEEKLY_AT ->
                setRepeatingAlarmAt(pendingIntent, frequency, executeAfter)
        }
    }

    private fun setExactAlarmAt(pendingIntent: PendingIntent, timeInMillis: Long) {
        val alarmCalendar: Calendar = Calendar.getInstance()
        alarmCalendar.timeInMillis = dateStartOfDay().time + timeInMillis
        if (alarmCalendar.timeInMillis < System.currentTimeMillis()) {
            alarmCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        setExactAlarm(pendingIntent, alarmCalendar.timeInMillis)
    }

    private fun setExactAlarm(pendingIntent: PendingIntent, triggerTime: Long) {
        val alarmManager: AlarmManager = context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
        )
    }

    private fun setRepeatingAlarmAt(pendingIntent: PendingIntent, frequency: AlarmFrequency, timeInMillis: Long) {
        val alarmCalendar: Calendar = Calendar.getInstance()
        alarmCalendar.timeInMillis = dateStartOfDay().time + timeInMillis
        if (alarmCalendar.timeInMillis < System.currentTimeMillis()) {
            alarmCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        setRepeatingAlarm(pendingIntent, alarmCalendar.timeInMillis, frequency)
    }

    private fun setRepeatingAlarm(pendingIntent: PendingIntent, triggerTime: Long, frequency: AlarmFrequency) {
        val alarmManager: AlarmManager = context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intervalMillis = when (frequency) {
            AlarmFrequency.HOURLY -> getMillisFromHours(1)
            AlarmFrequency.TWO_HOUR -> getMillisFromHours(2)
            AlarmFrequency.THREE_HOUR -> getMillisFromHours(3)
            AlarmFrequency.FOUR_HOUR -> getMillisFromHours(4)
            AlarmFrequency.SIX_HOUR -> getMillisFromHours(6)
            AlarmFrequency.DAILY -> getMillisFromHours(24)
            AlarmFrequency.TWO_DAY -> getMillisFromHours(24 * 2)
            AlarmFrequency.THREE_DAY -> getMillisFromHours(24 * 3)
            AlarmFrequency.DAILY_AT -> getMillisFromHours(24)
            AlarmFrequency.TWO_DAY_AT -> getMillisFromHours(24 * 2)
            AlarmFrequency.THREE_DAY_AT -> getMillisFromHours(24 * 3)
            AlarmFrequency.FOUR_DAY_AT -> getMillisFromHours(24 * 4)
            AlarmFrequency.WEEKLY_AT -> getMillisFromHours(24 * 7)
            else -> getMillisFromHours(6)
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP, triggerTime, intervalMillis, pendingIntent
        )
    }

    fun deleteAlarm(pendingIntent: PendingIntent) {
        val alarmManager = context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun getMillisFromHours(hours: Int) = hours * 3600 * 1000L
}