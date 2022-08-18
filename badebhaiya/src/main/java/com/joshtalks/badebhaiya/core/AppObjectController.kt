package com.joshtalks.badebhaiya.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.gson.*
import com.joshtalks.badebhaiya.BuildConfig
import com.joshtalks.badebhaiya.repository.CommonRepository
import com.joshtalks.badebhaiya.repository.service.initStethoLibrary
import com.joshtalks.badebhaiya.utils.datetimeutils.DateTimeUtils
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.branch.referral.Branch
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.text.DateFormat
import java.util.*

class AppObjectController {
    companion object {
        @JvmStatic
        lateinit var joshApplication: JoshApplication
            private set

        @JvmStatic
        lateinit var gsonMapper: Gson
            private set

        @JvmStatic
        var uiHandler: Handler = Handler(Looper.getMainLooper())
            private set

        @JvmStatic
        @Volatile
        var mRtcEngine: RtcEngine? = null

        @JvmStatic
        var screenWidth: Int = 0

        @JvmStatic
        var screenHeight: Int = 0

        fun init(context: Context) {
            joshApplication = context as JoshApplication
            initStethoLibrary(joshApplication)
            initGsonMapper()
            initFirebaseRemoteConfig()
            initNotificationChannels(context)
            initBranch(context)
            initRtcEngine(joshApplication)
            initUtils()
            initTimber()
            initCrashlytics()
            getCouponCode()
        }

        private fun getCouponCode() {
            CommonRepository().getCouponCode()
        }

        private fun initCrashlytics() {
            val mFirebaseAnalytics = FirebaseAnalytics.getInstance(joshApplication);
        }

        private fun initFirebaseRemoteConfig() {
            val remoteConfig = Firebase.remoteConfig
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 0
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
        }

        private fun initTimber(){
            if (BuildConfig.DEBUG) {
                Timber.plant(DebugTree())
            }
        }

        private fun initNotificationChannels(context: Context) {
            NotificationHelper.createNotificationChannel(
                context = context,
                importance = NotificationManagerCompat.IMPORTANCE_MAX,
                showBadge = false,
                name = NotificationType.REMINDER.value,
                description = "App notification channel.",
                enableLights = true,
                enableVibration = true,
            )
            NotificationHelper.createNotificationChannel(
                context = context,
                importance = NotificationManagerCompat.IMPORTANCE_MAX,
                showBadge = false,
                name = NotificationType.LIVE.value,
                description = "App notification channel.",
                enableLights = true,
                enableVibration = true,
            )
        }

        private fun initBranch(context: Context){
            Branch.getAutoInstance(context)
            if (BuildConfig.DEBUG) {
                Branch.enableLogging()
                Branch.enableTestMode()
            }
        }

        private fun initGsonMapper() {
            gsonMapper = GsonBuilder()
                .enableComplexMapKeySerialization()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .registerTypeAdapter(Date::class.java, object : JsonDeserializer<Date> {
                    @Throws(JsonParseException::class)
                    override fun deserialize(
                        json: JsonElement,
                        typeOfT: Type,
                        context: JsonDeserializationContext
                    ): Date {
                        return Date(json.asJsonPrimitive.asLong * 1000)
                    }
                })
                .excludeFieldsWithModifiers(
                    Modifier.TRANSIENT
                )
                .setDateFormat(DateFormat.LONG)
                .setPrettyPrinting()
                .serializeNulls()
                .create()
        }

        fun initRtcEngine(context: Context): RtcEngine? {
            try {
                mRtcEngine = RtcEngine.create(
                    context,
                    BuildConfig.AGORA_API_KEY,
                    object : IRtcEngineEventHandler() {})
            } catch (ex: Throwable) {
                Log.e("ABCService", "initRtcEngine: ",ex )
                ex.printStackTrace()
            }
            return mRtcEngine
        }

        private fun initUtils() {
            DateTimeUtils.setTimeZone("UTC")
        }
    }

    fun getFirebaseRemoteConfig(): FirebaseRemoteConfig {
        return FirebaseRemoteConfig.getInstance()
    }
}