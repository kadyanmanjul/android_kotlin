package com.joshtalks.joshskills.ui.reminder.set_reminder

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshcamerax.utils.SharedPrefsManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.SET_REMINDER_DESCRIPTION
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivityReminderBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.reminder.reminder_listing.ReminderListActivity
import com.joshtalks.joshskills.util.ReminderUtil
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


class ReminderActivity : CoreJoshActivity() {
    private var actionUpdate = false
    private var previousTime: String = EMPTY
    private lateinit var titleView: AppCompatTextView
    private var reminderId: Int = -1
    private lateinit var binding: ActivityReminderBinding
    private val viewModel by lazy { ViewModelProvider(this).get(ReminderViewModel::class.java) }
    private var alarmHour: Int = 0
    private var alarmMins: Int = 0
    private var alarmAmPm: Int = Calendar.AM
    var timeFormatter: NumberFormat = DecimalFormat("00")

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
            AppAnalytics.create(AnalyticsEvent.REMINDER_CROSS.NAME)
                .addBasicParam()
                .addUserDetails()
                .push()
            onBackPressed()
        }

        binding.reminderMsgTv.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(SET_REMINDER_DESCRIPTION)

        if (intent.extras != null) {
            if (intent.hasExtra("time")) {
                actionUpdate = true
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
                    ReminderUtil.Companion.ReminderFrequency.EVERYDAY.name == frequency -> binding.everydayChip.isChecked =
                        true
                    ReminderUtil.Companion.ReminderFrequency.WEEKENDS.name == frequency -> binding.weekendChip.isChecked =
                        true
                    ReminderUtil.Companion.ReminderFrequency.WEEKDAYS.name == frequency -> binding.weekdaysChip.isChecked =
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

        binding.createReminderBtn.setOnClickListener {
            if (actionUpdate) {
                AppAnalytics.create(AnalyticsEvent.UPDATE_REMINDER_CLICKED.NAME)
                    .addBasicParam()
                    .addUserDetails()
                    .push()
            } else {
                AppAnalytics.create(AnalyticsEvent.SET_REMINDER_CLICKED.NAME)
                    .addBasicParam()
                    .addUserDetails()
                    .push()
            }
            viewModel.submitReminder(
                reminderId,
                "${timeFormatter.format(alarmHour)}:${timeFormatter.format(alarmMins)}:00",
                getReminderFrequency().name,
                ReminderUtil.Companion.ReminderStatus.ACTIVE.name,
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
        val reminderUtil = ReminderUtil(applicationContext)
        reminderUtil.setAlarm(
            getReminderFrequency(),
            reminderUtil.getAlarmPendingIntent(reminderId),
            alarmHour,
            alarmMins
        )
        openNextScreen()
    }

    private fun openNextScreen() {
        val firstTime = SharedPrefsManager.newInstance(this)
            .getBoolean(SharedPrefsManager.Companion.IS_FIRST_REMINDER, true)
        if (!firstTime) {
            startActivity(Intent(this, ReminderListActivity::class.java))
            finish()
        } else {
            SharedPrefsManager.newInstance(this)
                .putBoolean(SharedPrefsManager.Companion.IS_FIRST_REMINDER, false)
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

    private fun getReminderFrequency(): ReminderUtil.Companion.ReminderFrequency {
        return when (binding.repeatModeChips.checkedChipId) {
            R.id.weekdays_chip -> {
                ReminderUtil.Companion.ReminderFrequency.WEEKDAYS
            }
            R.id.weekend_chip -> {
                ReminderUtil.Companion.ReminderFrequency.WEEKENDS
            }
            else -> {
                ReminderUtil.Companion.ReminderFrequency.EVERYDAY
            }
        }
    }

}
