package com.joshtalks.joshskills.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.StrictMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.notification.LocalNotificationAlarmReciever
import com.joshtalks.joshskills.core.pstn_states.PstnObserver
import com.joshtalks.joshskills.core.service.NOTIFICATION_DELAY
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.ui.voip.new_arch.ui.utils.getVoipState
import com.joshtalks.joshskills.util.ReminderUtil
import com.joshtalks.joshskills.voip.ProximityHelper
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.constant.State
import io.branch.referral.Branch
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import timber.log.Timber
import java.util.*

const val TAG = "JoshSkill"

class JoshApplication :
    MultiDexApplication(),
    LifecycleEventObserver,
    ComponentCallbacks2/*, Configuration.Provider*/ {
    private var isAudioReset = false

    companion object {
        @Volatile
        public var isAppVisible = false
    }

    fun isAppVisible(): Boolean {
        return isAppVisible
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        base.let { ViewPumpContextWrapper.wrap(it) }
    }

    override fun onCreate() {
        super.onCreate()
        AppObjectController.joshApplication = this
        Utils.initUtils(this)
        if (BuildConfig.DEBUG) {
            Branch.enableTestMode()
            Branch.enableLogging()
        }
        // TODO: Need to be removed
        Branch.getAutoInstance(this)
        turnOnStrictMode()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this@JoshApplication)
    }


    private fun turnOnStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .penaltyLog()
                    //   .penaltyDialog()
                    .detectAll()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .detectAll()
                    .build()
            )
            Timber.plant(Timber.DebugTree())
        }
    }

    fun onAppForegrounded() {
        Timber.tag(TAG).e("************* foregrounded")
        Timber.tag(TAG).e("************* ${isActivityVisible()}")
        isAppVisible = true
        WorkManagerAdmin.userAppUsage(isAppVisible)
        ReminderUtil(this).deleteNotificationAlarms()
        val startIndex = PrefManager.getIntValue(LOCAL_NOTIFICATION_INDEX)
        for (i in startIndex..2) {
            removeAlarmReminder(i)
        }
    }

    private fun removeAlarmReminder(delay: Int) {
        val alarmManager = this.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (alarmManager != null) {
            val pIntent = Intent(
                this.applicationContext,
                LocalNotificationAlarmReciever::class.java
            ).let { intent ->
                intent.putExtra("id", delay)
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                PendingIntent.getBroadcast(
                    this.applicationContext,
                    delay,
                    intent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    else
                        PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            alarmManager.cancel(pIntent)
        }
    }

    fun onAppBackgrounded() {
        Timber.tag(TAG).e("************* backgrounded")
        Timber.tag(TAG).e("************* ${isActivityVisible()}")
        isAppVisible = false
        WorkManagerAdmin.userAppUsage(isAppVisible)
        if (getConditionForShowLocalNotifications()) {
            val startIndex = PrefManager.getIntValue(LOCAL_NOTIFICATION_INDEX)
            for (i in startIndex..2) {
                setAlarmReminder(i)
            }
        }
        ReminderUtil(this).setAlarmNotificationWorker()
    }

    private fun setAlarmReminder(delay: Int) {

        val pIntent = Intent(
            this.applicationContext,
            LocalNotificationAlarmReciever::class.java
        ).let { intent ->
            intent.putExtra("id", delay)
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            PendingIntent.getBroadcast(
                this.applicationContext,
                delay,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                else
                    PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val alarmManager: AlarmManager =
            this.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
        }
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis + 60 * 1000 * NOTIFICATION_DELAY.get(delay),
            pIntent
        )

    }

    private fun getConditionForShowLocalNotifications(): Boolean {
        return AppObjectController.getFirebaseRemoteConfig().getBoolean(FirebaseRemoteConfigKey.SHOW_LOCAL_NOTIFICATIONS) &&
                PrefManager.getIntValue(LOCAL_NOTIFICATION_INDEX, defValue = 0) < 3 &&
                PrefManager.getBoolValue(CHAT_OPENED_FOR_NOTIFICATION, defValue = false) &&
                PrefManager.getBoolValue(IS_COURSE_BOUGHT).not() &&
                PrefManager.getBoolValue(LESSON_COMPLETED_FOR_NOTIFICATION, defValue = false).not()
    }

    fun onAppDestroy() {
        Timber.tag(TAG).e("************* onAppDestroy")
        AppObjectController.releaseInstance()
        PstnObserver.unregisterPstnReceiver()
    }

    private fun isActivityVisible(): String {
        return ProcessLifecycleOwner.get().lifecycle.currentState.name
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                if(getVoipState() == State.CONNECTED || getVoipState() == State.RECONNECTING)
                    ProximityHelper.getInstance(this)?.start()
                if(isAudioReset.not()) {
                    (applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager?)?.mode =
                        AudioManager.MODE_NORMAL
                    isAudioReset = true
                }
                onAppForegrounded()
            }
            Lifecycle.Event.ON_STOP -> {
                ProximityHelper.getInstance(this)?.stop()
                onAppBackgrounded()
            }
            Lifecycle.Event.ON_DESTROY -> {
                onAppDestroy()
            }
            else -> {}
        }
    }

}
