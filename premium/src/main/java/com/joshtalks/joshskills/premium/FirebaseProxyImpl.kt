package com.joshtalks.joshskills.premium

import android.content.Intent
import com.freshchat.consumer.sdk.Freshchat
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.FirebaseProxy
import com.joshtalks.joshskills.PremiumApplication
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.premium.core.analytics.AppAnalytics
import com.joshtalks.joshskills.premium.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.premium.core.notification.FCM_TOKEN
import com.joshtalks.joshskills.premium.core.notification.FirebaseNotificationService
import com.joshtalks.joshskills.premium.core.notification.NotificationUtils
import com.joshtalks.joshskills.premium.repository.local.model.FCMResponse
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.repository.local.model.NotificationObject
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.text.DateFormat
import java.util.*

class FirebaseProxyImpl : FirebaseProxy  {
    override fun onNewToken(token: String) {
        Timber.tag(FirebaseNotificationService::class.java.name).e(token)
        try {
            if (PrefManager.hasKey(FCM_TOKEN)) {
                val fcmResponse = FCMResponse.getInstance()
                fcmResponse?.apiStatus = ApiRespStatus.POST
                fcmResponse?.update()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        PrefManager.put(FCM_TOKEN, token)
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO +
                    CoroutineExceptionHandler { _, _ -> /* Do Nothing */ }
        ).launch {
            val userId = Mentor.getInstance().getId()
            if (userId.isNotBlank()) {
                try {
                    val data = mutableMapOf(
                        "user_id" to userId,
                        "registration_id" to token,
                        "name" to Utils.getDeviceName(),
                        "device_id" to Utils.getDeviceId(),
                        "active" to "true",
                        "type" to "android",
                        "gaid" to PrefManager.getStringValue(USER_UNIQUE_ID),
                        "newToken" to "true"
                    )
                    val resp = AppObjectController.signUpNetworkService.postFCMToken(data.toMap())
                    if (resp.isSuccessful) {
                        resp.body()?.update()
                    }
                } catch (ex: Exception) {
                    try {
                        AppAnalytics.create(AnalyticsEvent.FCM_TOKEN_CRASH_EVENT.NAME)
                            .addBasicParam()
                            .addUserDetails()
                            .push()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                    ex.printStackTrace()
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.tag(FirebaseNotificationService::class.java.name)
            .e("fcm onMessageReceived data: ${remoteMessage.data}  remote body: ${remoteMessage.notification?.body}  title : ${remoteMessage.notification?.title}")

        try {
            if (Freshchat.isFreshchatNotification(remoteMessage))
                Freshchat.handleFcmMessage(PremiumApplication.premiumApplication, remoteMessage)
            else if (remoteMessage.data.containsKey("is_group")) {
                NotificationUtils(PremiumApplication.premiumApplication)
                    .processRemoteMessage(remoteMessage, NotificationAnalytics.Channel.GROUPS)
                NotificationUtils(PremiumApplication.premiumApplication).pushAnalytics(remoteMessage.data["group_id"])
                return
            } else {
                NotificationUtils(PremiumApplication.premiumApplication)
                    .processRemoteMessage(remoteMessage, NotificationAnalytics.Channel.FCM)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun handleIntent(intent: Intent) {
        if (!JoshApplication.isAppVisible) {
            Timber.tag(FirebaseNotificationService::class.java.name).e("intent : ${intent.extras}")

            var channel = NotificationAnalytics.Channel.FCM
            val data =  if (intent.extras?.containsKey("action") == true && intent.extras?.containsKey("id") == true)
                mapOf(
                    Pair("action", intent.extras?.getString("action")),
                    Pair("action_data", intent.extras?.getString("action_data")),
                    Pair("id", intent.extras?.getString("id")),
                    Pair("content_title", intent.extras?.getString("gcm.notification.title")),
                    Pair("content_text", intent.extras?.getString("gcm.notification.body"))
                )
            else {
                // TODO:
                //super.handleIntent(intent)
                return
            }

            if (data.isNullOrEmpty())
                return

            val notificationTypeToken: Type = object : TypeToken<NotificationObject>() {}.type
            val gsonMapper = GsonBuilder()
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
                .excludeFieldsWithModifiers(Modifier.TRANSIENT)
                .setDateFormat(DateFormat.LONG)
                .setPrettyPrinting()
                .serializeNulls()
                .create()

            val nc: NotificationObject = gsonMapper.fromJson(
                gsonMapper.toJson(data),
                notificationTypeToken
            )

            if (intent.extras?.containsKey("is_group") == true) {
                NotificationUtils(PremiumApplication.premiumApplication).sendNotification(nc)
                NotificationUtils(PremiumApplication.premiumApplication).pushAnalytics(intent.extras?.getString("group_id"))
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                val isFirstTimeNotification = NotificationAnalytics().addAnalytics(
                    notificationId = nc.id.toString(),
                    mEvent = NotificationAnalytics.Action.RECEIVED,
                    channel = channel
                )
                if (isFirstTimeNotification)
                    NotificationUtils(PremiumApplication.premiumApplication).sendNotification(nc)
            }
        }
        // TODO:
        //else
            //super.handleIntent(intent)
    }

}