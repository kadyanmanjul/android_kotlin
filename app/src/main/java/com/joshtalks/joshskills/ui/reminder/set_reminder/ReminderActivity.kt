package com.joshtalks.joshskills.ui.reminder.set_reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_INCLUDE_STOPPED_PACKAGES
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.ActivityReminderBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.reminder.reminder_listing.ReminderListActivity
import java.util.*


class ReminderActivity : CoreJoshActivity() {
    private var previousTime: String = EMPTY
    private lateinit var titleView: AppCompatTextView
    private lateinit var binding: ActivityReminderBinding
    private val viewModel by lazy { ViewModelProvider(this).get(ReminderViewModel::class.java) }
    private var alarmHour: Int = 0
    private var alarmMins: Int = 0
    private var alarmAmPm: Int = Calendar.AM

    companion object {
        fun getIntent(context: Context, time: String, frequency: String): Intent {
            val intent = Intent(context, ReminderActivity::class.java)
            intent.putExtra("time", time)
            intent.putExtra("frequency", frequency)
            return intent
        }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_reminder)
        binding.lifecycleOwner = this
        binding.reminderData = this

        titleView = findViewById<AppCompatTextView>(R.id.text_message_title)
        titleView.text = getString(R.string.reminders)

        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            onBackPressed()
        }

        if (intent.extras != null) {
            if (intent.hasExtra("time")) {
                intent.getStringExtra("time")?.let {
                    previousTime = it
                    binding.createReminderBtn.text = getString(R.string.save_changes)
                    val parts = it.split(":")
                    alarmMins = parts[1].toInt()
                    alarmHour = parts[0].toInt()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        binding.timePicker.hour = alarmHour
                        binding.timePicker.minute = alarmMins
                    } else {
                        binding.timePicker.currentHour = alarmHour
                        binding.timePicker.currentMinute = alarmMins
                    }
                }
            }
            if (intent.hasExtra("frequency")) {
                val frequency = intent.getStringExtra("frequency")
                when {
                    ReminderFrequency.EVERYDAY.name == frequency -> binding.everydayChip.isChecked =
                        true
                    ReminderFrequency.WEEKENDS.name == frequency -> binding.weekendChip.isChecked =
                        true
                    ReminderFrequency.WEEKDAYS.name == frequency -> binding.weekdaysChip.isChecked =
                        true
                }
            }

        }
        binding.createReminderBtn.setOnClickListener {
            viewModel.submitReminder(
                "$alarmHour:$alarmMins",
                getReminderFrequency(),
                ReminderStatus.ACTIVE.name,
                Mentor.getInstance().getId(),
                previousTime
            )
        }
        binding.timePicker.setOnTimeChangedListener { view, hourOfDay, minute ->
            alarmHour = hourOfDay
            alarmMins = minute
            alarmAmPm = if (alarmHour > 11) Calendar.PM else Calendar.AM
        }

        viewModel.submitApiCallStatusLiveData.observe(this, androidx.lifecycle.Observer {
            if (it == ApiCallStatus.SUCCESS) {
                setAlarm(binding.repeatModeChips.checkedChipId)
                Toast.makeText(this, "Alarm added", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, ReminderListActivity::class.java))
            }
        })
    }

    private fun getReminderFrequency(): String {
        when (binding.repeatModeChips.checkedChipId) {
            R.id.weekdays_chip -> {
                return ReminderFrequency.WEEKDAYS.name
            }
            R.id.weekend_chip -> {
                return ReminderFrequency.WEEKENDS.name
            }
            R.id.everyday_chip -> {
                return ReminderFrequency.EVERYDAY.name
            }
            else ->
                return ""
        }

    }

    private fun setAlarm(repeatMode: Int) {
        val alarmCalendar: Calendar = Calendar.getInstance()
        alarmCalendar.set(Calendar.HOUR_OF_DAY, alarmHour)
        alarmCalendar.set(Calendar.MINUTE, alarmMins)
        alarmCalendar.set(Calendar.SECOND, 0)

        val currentTime = System.currentTimeMillis()

        when (repeatMode) {
            R.id.everyday_chip -> {
                createAlarm(
                    getAlarmPendingIntent(currentTime),
                    alarmCalendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY
                )
            }
            R.id.weekdays_chip -> {
                alarmCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                createAlarm(
                    getAlarmPendingIntent(currentTime),
                    alarmCalendar.timeInMillis,
                    7 * 24 * 60 * 60 * 1000
                )

                alarmCalendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
                createAlarm(
                    getAlarmPendingIntent(currentTime + 1),
                    alarmCalendar.timeInMillis,
                    7 * 24 * 60 * 60 * 1000
                )

                alarmCalendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
                createAlarm(
                    getAlarmPendingIntent(currentTime + 2),
                    alarmCalendar.timeInMillis,
                    7 * 24 * 60 * 60 * 1000
                )

                alarmCalendar.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY)
                createAlarm(
                    getAlarmPendingIntent(currentTime + 3),
                    alarmCalendar.timeInMillis,
                    7 * 24 * 60 * 60 * 1000
                )

                alarmCalendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
                createAlarm(
                    getAlarmPendingIntent(currentTime + 4),
                    alarmCalendar.timeInMillis,
                    7 * 24 * 60 * 60 * 1000
                )

            }
            R.id.weekend_chip -> {
                alarmCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                createAlarm(
                    getAlarmPendingIntent(currentTime),
                    alarmCalendar.timeInMillis,
                    7 * 24 * 60 * 60 * 1000
                )

                alarmCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
                createAlarm(
                    getAlarmPendingIntent(currentTime + 1),
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

    private fun getAlarmPendingIntent(requestCode: Long): PendingIntent {
        val intent = Intent(applicationContext, AlarmReceiver::class.java)
        intent.putExtra("id", "1234145")
        intent.addFlags(FLAG_INCLUDE_STOPPED_PACKAGES)
        return PendingIntent.getBroadcast(
            applicationContext,
            1256,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

}