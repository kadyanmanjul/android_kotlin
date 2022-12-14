package com.joshtalks.joshskills.common.core.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.common.BuildConfig
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.repository.local.AppDatabase
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.repository.local.model.NotificationChannelData
import com.joshtalks.joshskills.common.repository.local.model.NotificationObject
import com.joshtalks.joshskills.common.repository.service.UtilsAPIService
import com.joshtalks.joshskills.common.ui.inbox.InboxActivity
import com.joshtalks.joshskills.common.util.showAppropriateMsg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val CONNECTION_TIMEOUT = 30L
private const val CALL_TIMEOUT = 60L
private const val WRITE_TIMEOUT = 30L
private const val READ_TIMEOUT = 30L

const val NOTIF_ID = 12301
val NOTIF_CHANNEL_ID = NotificationChannelData.OTHERS.id
val NOTIF_CHANNEL_NAME = NotificationChannelData.OTHERS.type

//TODO: Uncomment manifest file declaration
class BackgroundService : Service() {

    lateinit var apiService: UtilsAPIService

    val gsonMapper: Gson by lazy {
        GsonBuilder()
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
    }

    override fun onCreate() {
        super.onCreate()
        showNotification()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        initRetrofit()
        com.joshtalks.joshskills.common.util.ReminderUtil(this).deleteNotificationAlarms()
        com.joshtalks.joshskills.common.util.ReminderUtil(this).setAlarmNotificationWorker()
        pushAnalyticsToServer()
        fetchMissedNotifications()
        return START_STICKY
    }

    private fun initRetrofit() {
        val builder = OkHttpClient().newBuilder()
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT, TimeUnit.SECONDS)
            .followSslRedirects(true)
            .addInterceptor(StatusCodeInterceptor())
            .addInterceptor(HeaderInterceptor())
            .hostnameVerifier { _, _ -> true }
            .addNetworkInterceptor(getStethoInterceptor())

        apiService = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(builder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UtilsAPIService::class.java)
    }

    private fun fetchMissedNotifications() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notifications = apiService.getMissedNotifications(Mentor.getInstance().getId()).body()
                if (notifications?.isNotEmpty() == true) {
                    for (item in notifications) {
                        val notificationTypeToken: Type = object : TypeToken<NotificationObject>() {}.type
                        val nc: NotificationObject = gsonMapper.fromJson(
                            gsonMapper.toJson(item),
                            notificationTypeToken
                        )
                        nc.contentTitle = item.title
                        nc.contentText = item.body

                        //TODO: Fix the code here -- Sukesh
//                        val isFirstTimeNotification = NotificationAnalytics().addAnalytics(
//                            notificationId = nc.id.toString(),
//                            mEvent = NotificationAnalytics.Action.RECEIVED,
//                            channel = NotificationAnalytics.Channel.API
//                        )
//                        if (isFirstTimeNotification) {
//                            NotificationUtils(this@BackgroundService).sendNotification(nc)
//                        }
                    }
                }
            } catch (e: Exception) {
                e.showAppropriateMsg()
                try {
                    FirebaseCrashlytics.getInstance().recordException(e)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            stopForeground(true)
            stopSelf()
        }
    }

    private fun showNotification() {
        val notificationIntent = Intent(this, InboxActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE
            else
                0
        )

        startForeground(NOTIF_ID, buildNotification(pendingIntent))
    }

    private fun buildNotification(pendingIntent: PendingIntent): Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_status_bar_notification)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Fetching all your notifications!")
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.FLAG_ONGOING_EVENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.priority = NotificationManager.IMPORTANCE_LOW
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                NOTIF_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )

            notificationBuilder.setChannelId(NOTIF_CHANNEL_ID)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notification = notificationBuilder.build()
        notification.flags = Notification.FLAG_NO_CLEAR

        return notification
    }

    fun pushAnalyticsToServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notificationDao = AppDatabase.getDatabase(this@BackgroundService)?.notificationEventDao()
                val listOfReceived = notificationDao?.getUnsyncEvent()
                if (listOfReceived?.isEmpty() == true)
                    return@launch

                val serverOffsetTime = PrefManager.getLongValue(SERVER_TIME_OFFSET, true)
                listOfReceived?.forEach {
                    try {
//                        apiService.engageNewNotificationAsync(
//                            NotificationAnalyticsRequest(
//                                it.id,
//                                it.time_stamp.plus(serverOffsetTime),
//                                it.action,
//                                it.platform
//                            )
//                        )
                        notificationDao.updateSyncStatus(it.notificationId)
                    } catch (e: Exception) {
                        if (e is HttpException) {
                            if (e.code() == 400)
                                notificationDao.updateSyncStatus(it.notificationId)
                        }
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.showAppropriateMsg()
                try {
                    FirebaseCrashlytics.getInstance().recordException(e)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }
}