package com.joshtalks.badebhaiya.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.*
import com.joshtalks.badebhaiya.BuildConfig
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.repository.service.initStethoLibrary
import io.branch.referral.Branch
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

        fun init(context: Context) {
            joshApplication = context as JoshApplication
            initStethoLibrary(context)
            initGsonMapper()
            initNotificationChannels(context)
            initBranch(context)
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
    }

    fun getFirebaseRemoteConfig(): FirebaseRemoteConfig {
        return FirebaseRemoteConfig.getInstance()
    }
}