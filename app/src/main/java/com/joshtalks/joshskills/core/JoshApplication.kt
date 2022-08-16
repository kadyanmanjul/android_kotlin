package com.joshtalks.joshskills.core

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import androidx.work.impl.background.greedy.GreedyScheduler
import com.facebook.stetho.Stetho
import com.freshchat.consumer.sdk.Freshchat
import com.google.android.play.core.splitcompat.SplitCompatApplication
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.notification.LocalNotificationAlarmReciever
import com.joshtalks.joshskills.core.pstn_states.PstnObserver
import com.joshtalks.joshskills.core.service.NOTIFICATION_DELAY
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.di.ApplicationComponent
import com.joshtalks.joshskills.di.DaggerApplicationComponent
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.util.ReminderUtil
import com.joshtalks.joshskills.voip.Utils
import com.moengage.core.DataCenter
import com.moengage.core.MoEngage
import com.moengage.core.config.MiPushConfig
import com.moengage.core.config.NotificationConfig
import com.moengage.core.enableAdIdTracking
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import io.branch.referral.Branch
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import java.lang.reflect.Method
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

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
        Log.d(TAG, "onCreate: STARTING MAIN PROCESS CHECK ${this.hashCode()}")
        if (BuildConfig.DEBUG) {
            Branch.enableTestMode()
            Branch.enableLogging()
        }
        Branch.getAutoInstance(this)
        if (isMainProcess()) {
            AppObjectController.initLibrary(this)
            AppObjectController.init(this@JoshApplication)
            Log.d(TAG, "onCreate: END ...IS MAIN PROCESS")
            turnOnStrictMode()
            ProcessLifecycleOwner.get().lifecycle.addObserver(this@JoshApplication)
            VoipPref.initVoipPref(this)
            PstnObserver
            registerBroadcastReceiver()
            initMoEngage()
            initGroups()
        } else {
            FirebaseApp.initializeApp(this)
            Timber.plant(Timber.DebugTree())
            Utils.initUtils(this)
            Stetho.initializeWithDefaults(this);
        }

        Log.d(TAG, "onCreate: STARTING MAIN PROCESS CHECK END")
    }

    private fun initMoEngage() {
        val moEngage = MoEngage.Builder(this, "DU9ICNBN2A9TTT38BS59KEU6")
            .setDataCenter(DataCenter.DATA_CENTER_3)
            .configureMiPush(MiPushConfig("2882303761518451933", "5761845183933", true))
            .configureNotificationMetaData(NotificationConfig(R.drawable.ic_status_bar_notification, R.mipmap.ic_launcher_round))
            .build()

        MoEngage.initialiseDefaultInstance(moEngage)
        enableAdIdTracking(this)
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

    private fun registerBroadcastReceiver() {
        val intentFilter = IntentFilter()
//        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        intentFilter.addAction(Intent.ACTION_USER_PRESENT)
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intentFilter.addAction(Intent.ACTION_USER_UNLOCKED)
        }
//        registerReceiver(ServiceStartReceiver(), intentFilter)

        JoshSkillExecutors.BOUNDED.submit {
            if (PrefManager.getStringValue(RESTORE_ID).isBlank()) {
                val intentFilterRestoreID =
                    IntentFilter(Freshchat.FRESHCHAT_USER_RESTORE_ID_GENERATED)
                getLocalBroadcastManager().registerReceiver(
                    restoreIdReceiver,
                    intentFilterRestoreID
                )
            }
        }
        JoshSkillExecutors.BOUNDED.submit {
            val intentFilterUnreadMessages =
                IntentFilter(Freshchat.FRESHCHAT_UNREAD_MESSAGE_COUNT_CHANGED)
            getLocalBroadcastManager().registerReceiver(
                unreadCountChangeReceiver,
                intentFilterUnreadMessages
            )
        }
    }

    private var restoreIdReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val restoreId = AppObjectController.freshChat?.user?.restoreId ?: EMPTY
                        if (restoreId.isBlank().not()) {
                            PrefManager.put(RESTORE_ID, restoreId)
                            val requestMap = mutableMapOf<String, String?>()
                            requestMap["restore_id"] = restoreId
                            AppObjectController.commonNetworkService.postFreshChatRestoreIDAsync(
                                PrefManager.getStringValue(USER_UNIQUE_ID),
                                requestMap
                            )
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }

    private var unreadCountChangeReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                AppObjectController.getUnreadFreshchatMessages()
            }
        }

    private fun getLocalBroadcastManager(): LocalBroadcastManager {
        return LocalBroadcastManager.getInstance(this@JoshApplication)
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

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                System.runFinalization()
                Runtime.getRuntime().gc()
                System.gc()
            }

            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                return
            }
            else -> {
                return
            }
        }
    }

    fun initGroups() {
        EmojiManager.install(IosEmojiProvider())
        //GroupRepository().subscribeNotifications()
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
