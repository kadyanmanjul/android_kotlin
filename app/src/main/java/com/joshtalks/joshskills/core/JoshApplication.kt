package com.joshtalks.joshskills.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.StrictMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.multidex.MultiDexApplication
import com.freshchat.consumer.sdk.Freshchat
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.notification.LocalNotificationAlarmReciever
import com.joshtalks.joshskills.core.service.BackgroundService
import com.joshtalks.joshskills.core.service.NOTIFICATION_DELAY
import com.joshtalks.joshskills.core.service.NetworkChangeReceiver
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.di.ApplicationComponent
import com.joshtalks.joshskills.di.DaggerApplicationComponent
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import com.vanniktech.emoji.ios.IosEmojiProvider
import com.vanniktech.emoji.EmojiManager

const val TAG = "JoshSkill"

class JoshApplication :
    MultiDexApplication(),
    LifecycleObserver,
    ComponentCallbacks2/*, Configuration.Provider*/ {
    val applicationGraph: ApplicationComponent by lazy {
        DaggerApplicationComponent.create()
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
        turnOnStrictMode()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        AppObjectController.init(this)
        registerBroadcastReceiver()
//        initServices()
        initGroups()
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
}
