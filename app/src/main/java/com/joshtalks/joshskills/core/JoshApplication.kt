package com.joshtalks.joshskills.core

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.*
import android.os.Build
import android.os.Process
import android.os.StrictMode
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.multidex.MultiDexApplication
import androidx.work.impl.background.greedy.GreedyScheduler
import com.facebook.stetho.Stetho
import com.freshchat.consumer.sdk.Freshchat
import com.google.firebase.FirebaseApp
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.AppObjectController.Companion.getLocalBroadcastManager
import com.joshtalks.joshskills.core.AppObjectController.Companion.restoreIdReceiver
import com.joshtalks.joshskills.core.AppObjectController.Companion.unreadCountChangeReceiver
import com.joshtalks.joshskills.core.notification.LocalNotificationAlarmReciever
import com.joshtalks.joshskills.core.pstn_states.PstnObserver
import com.joshtalks.joshskills.core.service.NOTIFICATION_DELAY
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.di.ApplicationComponent
import com.joshtalks.joshskills.di.DaggerApplicationComponent
import com.joshtalks.joshskills.util.ReminderUtil
import com.joshtalks.joshskills.voip.Utils
import io.branch.referral.Branch
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.reflect.Method
import java.util.*

/**
 * 1. Remove Process for P2P Call
 * 2. Remove WorkManager Init from StartupLibrary
 * 3. Check if Internet is Off then also RemoteConfig is working
 */

const val TAG = "JoshSkill"

class JoshApplication :
    MultiDexApplication(),
    LifecycleEventObserver,
    ComponentCallbacks2, Configuration.Provider {
    val applicationGraph: ApplicationComponent by lazy {
        DaggerApplicationComponent.create()
    }

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
        //enableLog(Feature.VOIP)
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

    override fun onTerminate() {
        super.onTerminate()
        getLocalBroadcastManager().unregisterReceiver(restoreIdReceiver)
        getLocalBroadcastManager().unregisterReceiver(unreadCountChangeReceiver)
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
        val alarmManager =
            this.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
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
        WorkManagerAdmin.setLocalNotificationWorker()
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
                onAppForegrounded()
            }
            Lifecycle.Event.ON_STOP -> {
                onAppBackgrounded()
            }
            Lifecycle.Event.ON_DESTROY -> {
                onAppDestroy()
            }
        }
    }

    @SuppressLint("PrivateApi")
    private fun getProcName(): String? {
        if (Build.VERSION.SDK_INT >= 28) {
            return Application.getProcessName()
        }

        // Try using ActivityThread to determine the current process name.
        Log.d(TAG, "getProcName: Trying ActivityThread ...")
        try {
            val activityThread = Class.forName(
                "android.app.ActivityThread",
                false,
                GreedyScheduler::class.java.classLoader
            )

            val packageName = if (Build.VERSION.SDK_INT >= 18) {
                val currentProcessName: Method =
                    activityThread.getDeclaredMethod("currentProcessName")
                currentProcessName.setAccessible(true)
                currentProcessName.invoke(null)
            } else {
                val getActivityThread: Method = activityThread.getDeclaredMethod(
                    "currentActivityThread"
                )
                getActivityThread.setAccessible(true)
                val getProcessName: Method = activityThread.getDeclaredMethod("getProcessName")
                getProcessName.setAccessible(true)
                getProcessName.invoke(getActivityThread.invoke(null))
            }
            if (packageName is String) {
                return packageName
            }
        } catch (exception: Throwable) {
            Log.d("TAG", "Unable to check ActivityThread for processName", exception)
        }

        // Fallback to the most expensive way
        Log.d(TAG, "getProcName: Trying expensive way ...")
        val pid: Int = Process.myPid()
        val am: ActivityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        if (am != null) {
            val processes: List<ActivityManager.RunningAppProcessInfo> = am.getRunningAppProcesses()
            if (processes != null && !processes.isEmpty()) {
                for (process in processes) {
                    if (process.pid === pid) {
                        return process.processName
                    }
                }
            }
        }
        return null
    }

    fun isMainProcess(): Boolean {
        Log.d(TAG, "onCreate: STARTING ...IS MAIN PROCESS")
        return TextUtils.equals(packageName, getProcName())
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder().setMinimumLoggingLevel(Log.VERBOSE).build()
    }
}
