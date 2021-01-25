package com.joshtalks.joshskills.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.IS_SUBSCRIPTION_STARTED
import com.joshtalks.joshskills.core.IS_TRIAL_STARTED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.DismissNotifEventReceiver
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import com.joshtalks.joshskills.repository.local.model.NotificationAction
import com.joshtalks.joshskills.repository.local.model.NotificationChannelNames
import com.joshtalks.joshskills.repository.local.model.NotificationObject
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import java.lang.reflect.Type
import java.util.Calendar
import java.util.Date

class EngageToUseAppNotificationWorker(var context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private var notificationChannelId = "101213"
    private var notificationChannelName = "Engagement"

    override suspend fun doWork(): Result {
        if (PrefManager.getBoolValue(IS_SUBSCRIPTION_STARTED)) {
            return Result.success()
        }
        if (PrefManager.getBoolValue(IS_TRIAL_STARTED).not()) {
            return Result.success()
        }
        if (isCurrentTimeNotification().not()) {
            return Result.success()
        }
        // today notification limit reached
        val installed: Long = InstallReferrerModel.getPrefObject()?.installOn?.times(1000)
            ?: context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        var string = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.FREE_TRIAL_COURSE_IDS)


        when (Utils.diffFromToday(Date(installed))) {
            1 -> {
                createNotificationMediator(
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.FREE_TRIAL_COURSE_IDS)
                )
            }
            2 -> {

            }
            3 -> {

            }
            4 -> {

            }
            5 -> {

            }
            6 -> {

            }
            7 -> {

            }
        }






        return Result.success()
    }

    private fun isCurrentTimeNotification(): Boolean {
        val cal: Calendar = Calendar.getInstance()
        cal.time = Date()
        val hour: Int = cal.get(Calendar.HOUR_OF_DAY)
        if (hour in 8..20) {
            return true
        }
        return false
    }

    private fun createNotificationMediator(data: String) {
        if (data.isEmpty()) {
            return
        }
        val notificationTypeToken: Type = object : TypeToken<NotificationObject>() {}.type
        val nc: NotificationObject = AppObjectController.gsonMapper.fromJson(
            AppObjectController.gsonMapper.toJson(data),
            notificationTypeToken
        )
        createEngageNotification(nc)
    }

    private fun createEngageNotification(notificationObject: NotificationObject) {
        val intent = getIntentAccordingAction(
            notificationObject,
            notificationObject.action,
            notificationObject.actionData
        )

        intent?.run {
            this.putExtra(HAS_NOTIFICATION, true)
            val activityList = arrayOf(this)
            intent.putExtra(NOTIFICATION_ID, notificationObject.id)
            val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val pendingIntent = PendingIntent.getActivities(
                applicationContext,
                uniqueInt, activityList,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notificationBuilder =
                NotificationCompat.Builder(
                    context, notificationChannelId
                )
                    .setTicker(notificationObject.ticker)
                    .setSmallIcon(R.drawable.ic_status_bar_notification)
                    .setContentTitle(notificationObject.contentTitle)
                    .setAutoCancel(true)
                    .setSound(defaultSound)
                    .setContentText(notificationObject.contentText)
                    .setContentIntent(pendingIntent)
                    .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                    .setWhen(System.currentTimeMillis())
                    .setStyle(getNotificationStyle(notificationObject))
            notificationBuilder.setDefaults(Notification.DEFAULT_ALL)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationBuilder.priority = NotificationManager.IMPORTANCE_HIGH
            }

            val dismissIntent =
                Intent(applicationContext, DismissNotifEventReceiver::class.java).apply {
                    putExtra(NOTIFICATION_ID, notificationObject.id)
                    putExtra(HAS_NOTIFICATION, true)

                }
            val dismissPendingIntent: PendingIntent =
                PendingIntent.getBroadcast(applicationContext, uniqueInt, dismissIntent, 0)

            notificationBuilder.setDeleteIntent(dismissPendingIntent)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
            notificationManager.notify(uniqueInt, notificationBuilder.build())
        }
    }

    private fun getNotificationStyle(notificationObject: NotificationObject): NotificationCompat.Style? {
        return if (notificationObject.bigPicture.isNullOrEmpty()) {
            NotificationCompat.BigTextStyle()
                .setBigContentTitle(notificationObject.contentTitle)
                .setSummaryText(notificationObject.contentText)
                .bigText(notificationObject.contentText)
        } else {
            val bitmap = Glide.with(context)
                .asBitmap()
                .load(notificationObject.bigPicture).submit().get()
            NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap)
                .bigLargeIcon(bitmap)
                .setBigContentTitle(notificationObject.contentTitle)
                .setSummaryText(notificationObject.contentText)
        }
    }


    private fun getIntentAccordingAction(
        notificationObject: NotificationObject,
        action: NotificationAction?,
        actionData: String?
    ): Intent? {

        return when (action) {
            NotificationAction.ACTION_OPEN_TEST -> {
                CourseDetailsActivity.getIntent(
                    applicationContext,
                    actionData!!.toInt(),
                    "Notification",
                    arrayOf(Intent.FLAG_ACTIVITY_CLEAR_TOP, Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
            }
            else -> {
                return null
            }
        }
    }
}