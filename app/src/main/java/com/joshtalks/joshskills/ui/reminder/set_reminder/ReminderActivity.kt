package com.joshtalks.joshskills.ui.reminder.set_reminder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshcamerax.utils.SharedPrefsManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.ActivityReminderBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.reminder.ReminderBaseActivity
import com.joshtalks.joshskills.ui.reminder.reminder_listing.ReminderListActivity
import java.text.SimpleDateFormat
import java.util.*


class ReminderActivity : ReminderBaseActivity() {
    private var previousTime: String = EMPTY
    private lateinit var titleView: AppCompatTextView
    private var reminderId: Int = -1
    private lateinit var binding: ActivityReminderBinding
    private val viewModel by lazy { ViewModelProvider(this).get(ReminderViewModel::class.java) }
    private var alarmHour: Int = 0
    private var alarmMins: Int = 0
    private var alarmAmPm: Int = Calendar.AM

    companion object {
        fun getIntent(context: Context, time: String, frequency: String, reminderId: Int): Intent {
            val intent = Intent(context, ReminderActivity::class.java)
            intent.putExtra("time", time)
            intent.putExtra("frequency", frequency)
            intent.putExtra("reminder_id", reminderId)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_reminder)
        binding.lifecycleOwner = this
        binding.reminderData = this

        titleView = findViewById(R.id.text_message_title)
        titleView.text = getString(R.string.set_reminder)

        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            onBackPressed()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val pm: PowerManager =
                getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
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
                reminderId = intent.getIntExtra("reminder_id", -1)
            }
            if (intent.hasExtra("frequency")) {
                val frequency = intent.getStringExtra("frequency")
                when {
                    ReminderBaseActivity.Companion.ReminderFrequency.EVERYDAY.name == frequency -> binding.everydayChip.isChecked =
                        true
                    ReminderBaseActivity.Companion.ReminderFrequency.WEEKENDS.name == frequency -> binding.weekendChip.isChecked =
                        true
                    ReminderBaseActivity.Companion.ReminderFrequency.WEEKDAYS.name == frequency -> binding.weekdaysChip.isChecked =
                        true
                }
            }
        } else {
            val dt = Date(System.currentTimeMillis())
            val sdf = SimpleDateFormat("HH:mm")
            val time1: String = sdf.format(dt)

            val timeParts = time1.split(":")
            try {
                alarmHour = timeParts[0].toInt()
                alarmMins = timeParts[1].toInt()
            } catch (e: Exception) {

            }
        }

        println("default time is $alarmHour $alarmMins")

        binding.createReminderBtn.setOnClickListener {
            viewModel.submitReminder(
                reminderId,
                "$alarmHour:$alarmMins",
                getReminderFrequency().name,
                ReminderBaseActivity.Companion.ReminderStatus.ACTIVE.name,
                Mentor.getInstance().getId(),
                previousTime,
                this::onAlarmSetSuccess
            )
        }
        binding.timePicker.setOnTimeChangedListener { view, hourOfDay, minute ->
            alarmHour = hourOfDay
            alarmMins = minute
            alarmAmPm = if (alarmHour > 11) Calendar.PM else Calendar.AM
        }
    }

    private fun onAlarmSetSuccess(reminderId: Int) {
        setAlarm(
            getReminderFrequency(),
            getAlarmPendingIntent(reminderId),
            alarmHour,
            alarmMins
        )
        println("alarm set success")
        val firstTime = SharedPrefsManager.newInstance(this)
            .getBoolean(SharedPrefsManager.Companion.IS_FIRST_REMINDER, true)
        if (firstTime) {
            SharedPrefsManager.newInstance(this)
                .putBoolean(SharedPrefsManager.Companion.IS_FIRST_REMINDER, false)
            openNextScreen(firstTime)
        }

        openNextScreen(false)
    }

    private fun openNextScreen(firstTime: Boolean) {
//        showBottomSheet()
        if (!firstTime) {
            startActivity(Intent(this, ReminderListActivity::class.java))
            finish()
        } else {
            showBottomSheet()
        }
    }

    private fun showBottomSheet() {
        val bottomSheetFragment = ReminderBottomSheet.newInstance()
        bottomSheetFragment.show(
            supportFragmentManager,
            ReminderBottomSheet::class.java.name
        )
    }

    private fun getReminderFrequency(): ReminderBaseActivity.Companion.ReminderFrequency {
        return when (binding.repeatModeChips.checkedChipId) {
            R.id.weekdays_chip -> {
                ReminderBaseActivity.Companion.ReminderFrequency.WEEKDAYS
            }
            R.id.weekend_chip -> {
                ReminderBaseActivity.Companion.ReminderFrequency.WEEKENDS
            }
            else -> {
                ReminderBaseActivity.Companion.ReminderFrequency.EVERYDAY
            }
        }

    }

}
