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
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.multidex.MultiDexApplication
import androidx.work.impl.background.greedy.GreedyScheduler
import com.facebook.stetho.Stetho
import com.freshchat.consumer.sdk.Freshchat
import com.google.firebase.FirebaseApp
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.notification.LocalNotificationAlarmReciever
import com.joshtalks.joshskills.core.service.BackgroundService
import com.joshtalks.joshskills.core.service.NOTIFICATION_DELAY
import com.joshtalks.joshskills.core.service.NetworkChangeReceiver
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.di.ApplicationComponent
import com.joshtalks.joshskills.di.DaggerApplicationComponent
import com.joshtalks.joshskills.ui.voip.presence.UserPresence
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.base.log.Feature
import com.joshtalks.joshskills.base.log.JoshLog.Companion.enableLog
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
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
    LifecycleObserver,
    ComponentCallbacks2/*, Configuration.Provider*/ {
    val applicationGraph: ApplicationComponent by lazy {
        DaggerApplicationComponent.create()
    }
    val userPresenceStatus by lazy {
        UserPresence
    }

    companion object {
        @Volatile
        public var isAppVisible = false
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        base.let { ViewPumpContextWrapper.wrap(it) }
    }

    override fun onCreate() {
        super.onCreate()
        enableLog(Feature.VOIP)
        Log.d(TAG, "onCreate: STARTING MAIN PROCESS CHECK ${this.hashCode()}")
//            if(isMainProcess()) {
                Log.d(TAG, "onCreate: END ...IS MAIN PROCESS")
                turnOnStrictMode()
                ProcessLifecycleOwner.get().lifecycle.addObserver(this@JoshApplication)
                AppObjectController.init(this@JoshApplication)
                Utils.initUtils(this)
                VoipPref.initVoipPref(this)
                registerBroadcastReceiver()
                initGroups()
//            } else {
//                FirebaseApp.initializeApp(this)
//                Timber.plant(Timber.DebugTree())
//                Utils.initUtils(this)
//                Stetho.initializeWithDefaults(this);
//            }

            Log.d(TAG, "onCreate: STARTING MAIN PROCESS CHECK END")
//        Log.d(TAG, "onCreate: $isMainProcess ... $packageName")
//        if(isMainProcess()) {
//            CoroutineScope(Dispatchers.IO).launch {
//
//            }
//        }
    }

    private fun initServices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(Intent(applicationContext, BackgroundService::class.java))
        } else {
            applicationContext.startService(Intent(applicationContext, BackgroundService::class.java))
        }
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
        registerReceiver(
            NetworkChangeReceiver(),
            IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        )
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

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Timber.tag(TAG).e("************* foregrounded")
        Timber.tag(TAG).e("************* ${isActivityVisible()}")
        isAppVisible = true
//        userPresenceStatus.setUserPresence(Mentor.getInstance().getId(),System.currentTimeMillis())
        WorkManagerAdmin.userAppUsage(isAppVisible)
//        WorkManagerAdmin.userActiveStatusWorker(isAppVisible)
//        WorkManagerAdmin.removeRepeatingNotificationWorker()
        val startIndex = PrefManager.getIntValue(LOCAL_NOTIFICATION_INDEX)
        for (i in startIndex..2) {
//            WorkManagerAdmin.setRepeatingNotificationWorker(i)
            removeAlarmReminder(i)
        }
//        UsageStatsService.activeUserService(this)
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

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Timber.tag(TAG).e("************* backgrounded")
        Timber.tag(TAG).e("************* ${isActivityVisible()}")
        isAppVisible = false
//        userPresenceStatus.setUserPresence(Mentor.getInstance().getId(),null)
        WorkManagerAdmin.userAppUsage(isAppVisible)
//        WorkManagerAdmin.userActiveStatusWorker(isAppVisible)
        if (getConditionForShowLocalNotifications()) {
            val startIndex = PrefManager.getIntValue(LOCAL_NOTIFICATION_INDEX)
            for (i in startIndex..2) {
                //WorkManagerAdmin.setRepeatingNotificationWorker(i)
                setAlarmReminder(i)
            }
        }
//        UsageStatsService.inactiveUserService(this)
        WorkManagerAdmin.setLocalNotificationWorker()
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
        return AppObjectController.getFirebaseRemoteConfig()
            .getBoolean(FirebaseRemoteConfigKey.SHOW_LOCAL_NOTIFICATIONS) &&
                PrefManager.getIntValue(LOCAL_NOTIFICATION_INDEX, defValue = 0) < 3 &&
                PrefManager.getBoolValue(CHAT_OPENED_FOR_NOTIFICATION, defValue = false)
                && PrefManager.getBoolValue(LESSON_COMPLETED_FOR_NOTIFICATION, defValue = false)
            .not()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onAppDestroy() {
        Timber.tag(TAG).e("************* onAppDestroy")
        AppObjectController.releaseInstance()
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
}
