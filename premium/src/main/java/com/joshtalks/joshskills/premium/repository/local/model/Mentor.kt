package com.joshtalks.joshskills.premium.repository.local.model

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.API_TOKEN
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.core.AppObjectController.Companion.joshApplication
import com.joshtalks.joshskills.premium.core.EMPTY
import com.joshtalks.joshskills.premium.core.PrefManager
import com.joshtalks.joshskills.premium.core.analytics.*
import com.joshtalks.joshskills.premium.core.analytics.AppAnalytics
import com.joshtalks.joshskills.premium.core.io.LastSyncPrefManager
import com.joshtalks.joshskills.premium.core.notification.HAS_NOTIFICATION
import com.joshtalks.joshskills.premium.core.notification.NOTIFICATION_ID
import com.joshtalks.joshskills.premium.core.notification.NotificationCategory
import com.joshtalks.joshskills.premium.core.notification.NotificationUtils
import com.joshtalks.joshskills.premium.repository.local.model.googlelocation.Locality
import com.joshtalks.joshskills.premium.repository.server.signup.LoginResponse
import com.joshtalks.joshskills.premium.ui.signup.SignUpActivity
import com.userexperior.UserExperior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val MENTOR_PERSISTANT_KEY = "mentor"

class Mentor {

    @SerializedName("id")
    private var id: String? = null

    @SerializedName("user")
    private var user: User? = null

    @SerializedName("locality")
    private var locality: Locality? = null

    @SerializedName("user_id")
    private var userId: String? = null

    @Expose
    var referralCode: String = EMPTY

    companion object {
        @JvmStatic
        private var instance: Mentor? = null

        @JvmStatic
        fun getInstance(): Mentor {
            return try {
                instance = AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(MENTOR_PERSISTANT_KEY), Mentor::class.java
                )
                instance!!
            } catch (ex: Exception) {
                Mentor()
            }
        }

        fun updateFromLoginResponse(loginResponse: LoginResponse) {
            CoroutineScope(Dispatchers.IO).launch {
                val user = User.getInstance()
                user.userId = loginResponse.userId
                user.isVerified = false
                user.token = loginResponse.token
                User.update(user)
                PrefManager.put(API_TOKEN, loginResponse.token)
                getInstance()
                    .setId(loginResponse.mentorId)
                    .setReferralCode(loginResponse.referralCode)
                    .setUserId(loginResponse.userId)
                    .update()
                AppAnalytics.updateUser()
                NotificationUtils(joshApplication).removeScheduledNotification(NotificationCategory.APP_OPEN)
                UserExperior.setUserIdentifier(getInstance().getId())
            }
        }

        @JvmStatic
        fun deleteUserData() {
            AppObjectController.appDatabase.run {
                courseDao().getAllConversationId().forEach {
                    PrefManager.removeKey(it)
                }
                LastSyncPrefManager.clear()
                clearAllTables()
            }
        }

        @JvmStatic
        fun deleteUserCredentials(showNotification: Boolean = false) {
            logoutAndShowLoginScreen(showNotification)
            PrefManager.logoutUser()
        }

        private fun logoutAndShowLoginScreen(showNotification: Boolean) {
            val mentorId = getInstance().getId()
            CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
                try {
                    NotificationManagerCompat.from(joshApplication).cancelAll()
                    AppObjectController.signUpNetworkService.signoutUser(mentorId)
                } catch (ex:Exception){
                    LogException.catchException(ex)
                }
                MixPanelTracker.publishEvent(MixPanelEvent.USER_LOGGED_OUT).push()
                AppAnalytics.create(AnalyticsEvent.LOGOUT_CLICKED.NAME)
                    .addUserDetails()
                    .addParam(AnalyticsEvent.USER_LOGGED_OUT.NAME, true)
                    .push()
                if (joshApplication.isAppVisible()) {
                    val intent = Intent(joshApplication, SignUpActivity::class.java)
                    intent.apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    joshApplication.startActivity(intent)
                }
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
            val notificationChannelId = NotificationChannelData.UPDATES.id
            val notificationChannelName = NotificationChannelData.UPDATES.type
            val contentTitle: String? = null
            val contentText = joshApplication.getString(R.string.auto_logout_message)
            val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val pendingIntent = PendingIntent.getActivities(
                joshApplication,
                uniqueInt,
                activityList,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                else
                    PendingIntent.FLAG_UPDATE_CURRENT
            )

            val style = NotificationCompat.BigTextStyle()
            style.setBigContentTitle(contentTitle)
            style.bigText(contentText)
            style.setSummaryText("")

            val notificationBuilder =
                NotificationCompat.Builder(joshApplication, notificationChannelId)
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
                        ContextCompat.getColor(joshApplication, R.color.primary_500)
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
                PendingIntent.getBroadcast(
                    joshApplication,
                    uniqueInt,
                    dismissIntent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE
                    else
                        0
                )

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

    fun getLocality(): Locality? {
        return locality
    }

    fun setLocality(locality: Locality?): Mentor {
        this.locality = locality
        return this
    }

    fun logout() {
        instance = null
        Mentor().update()
    }

    fun update() {
        val string: String = toString()
        PrefManager.put(MENTOR_PERSISTANT_KEY, string)
        AppObjectController.observeFirestore()
    }

    fun updateUser(user: User): Mentor {
        this.user = user
        return this
    }

    fun resetMentor() {
        instance = null
    }

    fun updateFromResponse(mentor: Mentor) {
        setLocality(mentor.getLocality())
        mentor.user?.let { updateUser(it) }
        update()
    }

    fun isCurrentUser(): Boolean {
        return getId() == getInstance().getId()
    }

    fun getId(): String {
        return id ?: EMPTY
    }

    fun getUserId(): String {
        return userId ?: EMPTY
    }

    fun setId(id: String): Mentor {
        this.id = id
        return this
    }

    fun setReferralCode(code: String): Mentor {
        this.referralCode = code
        return this
    }

    fun setUserId(userId: String): Mentor {
        this.userId = userId
        return this
    }

    fun hasId(): Boolean {
        return id != null && id?.isNotEmpty()!!
    }

    fun getUser(): User? {
        return user
    }

    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }

}
