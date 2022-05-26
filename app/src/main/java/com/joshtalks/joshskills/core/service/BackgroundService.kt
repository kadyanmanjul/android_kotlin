package com.joshtalks.joshskills.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.HeaderInterceptor
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.SERVER_TIME_OFFSET
import com.joshtalks.joshskills.core.StatusCodeInterceptor
import com.joshtalks.joshskills.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.core.firestore.NotificationAnalyticsRequest
import com.joshtalks.joshskills.core.getStethoInterceptor
import com.joshtalks.joshskills.core.notification.NotificationUtils
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.joshtalks.joshskills.repository.local.model.NotificationObject
import com.joshtalks.joshskills.repository.service.UtilsAPIService
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val CONNECTION_TIMEOUT = 30L
private const val CALL_TIMEOUT = 60L
private const val WRITE_TIMEOUT = 30L
private const val READ_TIMEOUT = 30L

class BackgroundService : Service() {

    private val NOTIF_ID = 12301
    private val NOTIF_CHANNEL_ID = "12301"
    private val NOTIF_CHANNEL_NAME = "NOTIFICATION SERVICE"

    lateinit var apiService: UtilsAPIService

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        initRetrofit()
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
            val notifications = apiService.getMissedNotifications().body()
            if (notifications?.isNotEmpty() == true) {
                for (item in notifications) {
                    val notificationTypeToken: Type = object : TypeToken<NotificationObject>() {}.type
                    val nc: NotificationObject = AppObjectController.gsonMapper.fromJson(
                        AppObjectController.gsonMapper.toJson(item),
                        notificationTypeToken
                    )
                    nc.contentTitle = item.title
                    nc.contentText = item.body
                    val isFirstTimeNotification = NotificationAnalytics().addAnalytics(
                        notificationId = nc.id.toString(),
                        mEvent = NotificationAnalytics.Action.RECEIVED,
                        channel = NotificationAnalytics.Channel.API
                    )
                    if (isFirstTimeNotification)
                        NotificationUtils(this@BackgroundService).sendNotification(nc)
                }
            }
            stopForeground(true)
            stopSelf()
        }
    }

    private fun startForeground() {
        val notificationIntent = Intent(this, InboxActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        startForeground(BackgroundService().NOTIF_ID, buildNotification(pendingIntent))
    }

    private fun buildNotification(pendingIntent: PendingIntent): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, BackgroundService().NOTIF_CHANNEL_ID)
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

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
            val notificationDao = AppDatabase.getDatabase(this@BackgroundService)?.notificationEventDao()
            val listOfReceived = notificationDao?.getUnsyncEvent()
            if (listOfReceived?.isEmpty() == true)
                return@launch

            val serverOffsetTime = PrefManager.getLongValue(SERVER_TIME_OFFSET, true)
            val request = ArrayList<NotificationAnalyticsRequest>()
            listOfReceived?.forEach {
                request.add(
                    NotificationAnalyticsRequest(
                        it.id,
                        it.time_stamp.plus(serverOffsetTime),
                        it.action,
                        it.platform
                    )
                )
            }
            if (request.isEmpty())
                return@launch

            val resp = AppObjectController.utilsAPIService.engageNewNotificationAsync(request)
            if (resp.isSuccessful) {
                listOfReceived?.forEach {
                    notificationDao.updateSyncStatus(it.notificationId)
                }
            }
        }
    }
}