package com.joshtalks.joshskills.ui.reminder.set_reminder

import android.app.Activity
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.ui.launch.LauncherActivity
import com.joshtalks.joshskills.util.RingtoneManager
import java.text.SimpleDateFormat
import java.util.Date


class AlarmNotifierActivity : AppCompatActivity(),
    View.OnClickListener {

    private var mAudioPlayer: RingtoneManager? = null
    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_notifier)

        val descriptionTv = findViewById<MaterialTextView>(R.id.start_exp)
        val timeTv = findViewById<MaterialTextView>(R.id.alarm_time_tv)
        val amPmTv = findViewById<MaterialTextView>(R.id.alarm_am_pm)

        findViewById<MaterialTextView>(R.id.dismiss_bt).setOnClickListener {
            finish()
        }

        findViewById<MaterialTextView>(R.id.start_course_bt).setOnClickListener {
            startActivity(Intent(this, LauncherActivity::class.java))
            finish()
        }

        descriptionTv.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.REMINDER_NOTIFIER_SCREEN_DESCRIPTION)

        val dt = Date(System.currentTimeMillis())
        val sdf = SimpleDateFormat("hh:mm aa")
        val time1: String = sdf.format(dt)
        val timeparts = time1.split(" ")
        timeTv.text = timeparts[0]
        amPmTv.text = timeparts[1]

        turnScreenOnAndKeyguardOff()

        mAudioPlayer = RingtoneManager.getInstance(this)
        mAudioPlayer?.playRingtone()

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(1000, 1000, 1000, 1000, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    pattern, 5
                )
            )
        } else {
            //deprecated in API 26
            vibrator.vibrate(pattern, 5)
        }

    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.dismiss_bt -> {
                startActivity(Intent(this, ReminderActivity::class.java))
                finish()
            }
            R.id.start_course_bt -> {
                v.setOnLongClickListener { false }
                finish()
            }
        }
    }


    private fun Activity.turnScreenOnAndKeyguardOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        with(getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestDismissKeyguard(this@turnScreenOnAndKeyguardOff, null)
            }
        }
    }

    private fun Activity.turnScreenOffAndKeyguardOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false)
            setTurnScreenOn(false)
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        } else {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(AlarmReceiver.NOTIFICATION_ID)
        mAudioPlayer?.stopRingtone()
        vibrator.cancel()
        turnScreenOffAndKeyguardOn()
    }
}