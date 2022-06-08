package com.joshtalks.joshskills.core.notification

import android.content.Intent
import com.clevertap.android.sdk.CleverTapAPI
import com.freshchat.consumer.sdk.Freshchat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.ApiRespStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication.Companion.isAppVisible
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.repository.local.model.FCMResponse
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.NotificationObject
import com.moengage.firebase.MoEFireBaseHelper
import com.moengage.pushbase.MoEPushHelper
import java.lang.reflect.Type
import kotlin.collections.set
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.lang.reflect.Modifier
import java.text.DateFormat
import java.util.*

const val FCM_TOKEN = "fcmToken"
const val HAS_NOTIFICATION = "has_notification"
const val HAS_LOCAL_NOTIFICATION = "has_local_notification"
const val NOTIFICATION_ID = "notification_id"
const val HAS_COURSE_REPORT = "has_course_report"
const val QUESTION_ID = "question_id"

const val FCM_ACTIVE = "FCM_ACTIVE"

class FirebaseNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
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
        MoEFireBaseHelper.getInstance().passPushToken(applicationContext, token)
        CleverTapAPI.getDefaultInstance(this)?.pushFcmRegistrationId(token, true)
        if (AppObjectController.freshChat != null) {
            AppObjectController.freshChat?.setPushRegistrationToken(token)
        }
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
        super.onMessageReceived(remoteMessage)
        Timber.tag(FirebaseNotificationService::class.java.name)
            .e("fcm onMessageReceived data: ${remoteMessage.data}  remote body: ${remoteMessage.notification?.body}  title : ${remoteMessage.notification?.title}")

        try {
            if (Freshchat.isFreshchatNotification(remoteMessage))
                Freshchat.handleFcmMessage(this, remoteMessage)
            else if (MoEPushHelper.getInstance()
                    .isFromMoEngagePlatform(remoteMessage.data) && JSONObject(remoteMessage.data["gcm_alert"]).has("isCustom")
            ) {
                val dataJson = JSONObject(remoteMessage.data["gcm_alert"])
                remoteMessage.data["title"] = dataJson["title"].toString()
                remoteMessage.data["body"] = dataJson["body"].toString()
                remoteMessage.data["action"] = dataJson["client_action"].toString()
                remoteMessage.data["action_data"] = dataJson["action_data"].toString()
                remoteMessage.data["notification_id"] = dataJson["notification_id"].toString()
                NotificationUtils(this)
                    .processRemoteMessage(remoteMessage, NotificationAnalytics.Channel.MOENGAGE)
                MoEPushHelper.getInstance().logNotificationReceived(this, remoteMessage.data)
                return
            } else if (MoEPushHelper.getInstance().isFromMoEngagePlatform(remoteMessage.data)) {
                MoEFireBaseHelper.getInstance().passPushPayload(applicationContext, remoteMessage.data)
                return
            } else if (remoteMessage.data.containsKey("is_group")) {
                NotificationUtils(this)
                    .processRemoteMessage(remoteMessage, NotificationAnalytics.Channel.GROUPS)
                return
            } else {
                NotificationUtils(this)
                    .processRemoteMessage(remoteMessage, NotificationAnalytics.Channel.FCM)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun handleIntent(intent: Intent) {
        if (!isAppVisible) {
            val data = mapOf(
                Pair("action", intent.extras?.getString("action")),
                Pair("action_data", intent.extras?.getString("action_data")),
                Pair("id", intent.extras?.getString("id")),
                Pair("content_title", intent.extras?.getString("gcm.notification.title")),
                Pair("content_text", intent.extras?.getString("gcm.notification.body"))
            )

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
                NotificationUtils(this@FirebaseNotificationService).sendNotification(nc)
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                val isFirstTimeNotification = NotificationAnalytics().addAnalytics(
                    notificationId = nc.id.toString(),
                    mEvent = NotificationAnalytics.Action.RECEIVED,
                    channel = NotificationAnalytics.Channel.FCM
                )
                if (isFirstTimeNotification)
                    NotificationUtils(this@FirebaseNotificationService).sendNotification(nc)
            }
            Timber.tag(FirebaseNotificationService::class.java.name).e("intent : ${intent.extras}")
        } else
            super.handleIntent(intent)
    }
}
