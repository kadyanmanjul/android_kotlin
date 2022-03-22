package com.joshtalks.badebhaiya.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.joshtalks.badebhaiya.repository.service.initStethoLibrary
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.text.DateFormat
import java.util.Date

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