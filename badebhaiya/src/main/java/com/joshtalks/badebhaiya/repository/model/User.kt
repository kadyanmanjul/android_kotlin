package com.joshtalks.badebhaiya.repository.model

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.annotations.SerializedName
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.AppObjectController
import com.joshtalks.badebhaiya.core.AppObjectController.Companion.joshApplication
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.NotificationChannelNames
import com.joshtalks.badebhaiya.core.PrefManager
import com.joshtalks.badebhaiya.core.analytics.DismissNotifEventReceiver
import com.joshtalks.badebhaiya.notifications.HAS_NOTIFICATION
import com.joshtalks.badebhaiya.notifications.NOTIFICATION_ID
import com.joshtalks.badebhaiya.signup.SignUpActivity
import com.joshtalks.badebhaiya.utils.TAG
import kotlinx.coroutines.*

const val USER_PERSISTENT_KEY = "USER_PERSISTENT_KEY"

data class User(
    @SerializedName("first_name") var firstName: String? = null,
    @SerializedName("last_name") var lastName: String? = null,
    @SerializedName("user_id") var userId: String = EMPTY,
    @SerializedName("token") var token: String = EMPTY,
    @SerializedName("photo_url") var profilePicUrl: String? = null,
    @SerializedName("mobile") var mobile: String = EMPTY,
    @SerializedName("is_speaker") var isSpeaker: Boolean = false,
) {
    companion object {
        @JvmStatic
        private var instance: User? = null

        @JvmStatic
        fun getInstance(): User {
            return try {
                instance = AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(USER_PERSISTENT_KEY),
                    User::class.java
                )
                instance!!
            } catch (ex: Exception) {
                User()
            }
        }

        @JvmStatic
        fun deleteUserCredentials(showNotification: Boolean = false) {
            PrefManager.logoutUser()
            logoutAndShowLoginScreen(showNotification)
        }

        private fun logoutAndShowLoginScreen(showNotification: Boolean) {
            CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
                delay(1000)
//            AppObjectController.signUpNetworkService.signoutUser(getInstance().getId())

                val intent = Intent(joshApplication, SignUpActivity::class.java)
                intent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                joshApplication.startActivity(intent)
                if (showNotification) {
                    showLogoutNotification()
                }
            }
        }

        fun showLogoutNotification() {
            val activityList = arrayOf(
                Intent(joshApplication, SignUpActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )

            val notificationId = 101567
            val notificationChannelId = "109000"
            val notificationChannelName = NotificationChannelNames.DEFAULT.type
            val contentTitle: String? = null
            val contentText = joshApplication.getString(R.string.auto_logout_message)
            val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val pendingIntent = PendingIntent.getActivities(
                joshApplication,
                uniqueInt, activityList,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            val style = NotificationCompat.BigTextStyle()
            style.setBigContentTitle(contentTitle)
            style.bigText(contentText)
            style.setSummaryText("")

            val notificationBuilder =
                NotificationCompat.Builder(
                    joshApplication,
                    notificationChannelId
                )
                    .setTicker(null)
                    .setSmallIcon(R.drawable.ic_status_bar_notification)
                    .setContentTitle(contentTitle)
                    .setAutoCancel(true)
                    .setSound(defaultSound)
                    .setContentText(contentText)
                    .setContentIntent(pendingIntent)
                    .setStyle(style)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setColor(
                        ContextCompat.getColor(
                            joshApplication,
                            R.color.colorAccent
                        )
                    )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationBuilder.priority = NotificationManager.IMPORTANCE_HIGH
            }

            val dismissIntent =
                Intent(joshApplication, DismissNotifEventReceiver::class.java).apply {
                    putExtra(NOTIFICATION_ID, notificationId)
                    putExtra(HAS_NOTIFICATION, true)
                }
            val dismissPendingIntent: PendingIntent =
                PendingIntent.getBroadcast(joshApplication, uniqueInt, dismissIntent, 0)

            notificationBuilder.setDeleteIntent(dismissPendingIntent)

            val notificationManager =
                joshApplication.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(
                    notificationChannelId,
                    notificationChannelName,
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationChannel.enableLights(true)
                notificationChannel.enableVibration(true)
                notificationBuilder.setChannelId(notificationChannelId)
                notificationManager.createNotificationChannel(notificationChannel)
            }
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }

    fun update() {
        PrefManager.put(USER_PERSISTENT_KEY, this.toString())
        Log.i(TAG, "${this.userId}")
    }

    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }

    fun isLoggedIn(): Boolean = userId.isNotBlank()

    fun updateFromResponse(user: User) {
        this.firstName = user.firstName
        this.lastName = user.lastName
        this.mobile = user.mobile
        this.isSpeaker = user.isSpeaker
        if (this.profilePicUrl.isNullOrEmpty()) this.profilePicUrl = user.profilePicUrl
        if (this.userId.isEmpty()) this.userId = user.userId
        if (this.token.isEmpty()) this.token = user.token
        update()
    }
}
