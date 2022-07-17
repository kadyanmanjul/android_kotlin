package com.joshtalks.joshskills.ui.inbox.extra;

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.NotificationChannelNames


const val CHANNEL_ID = "VIDEO_AUDIO_PROCESSING"
const val NOTIFICATION_ID = 1681

class OnDemandFeatureDownloadService : Service() {
    private var mNotificationManager: NotificationManager? = null
    private var manager: SplitInstallManager? = null
    private val listener = SplitInstallStateUpdatedListener { state ->
        val multiInstall = state.moduleNames().size > 1
        val names = state.moduleNames().joinToString(" - ")
        when (state.status()) {
            SplitInstallSessionStatus.DOWNLOADING -> {
                showToast("SplitInstallStateUpdatedListener state: DOWNLOADING ")
            }
            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                showToast("SplitInstallStateUpdatedListener state: REQUIRES_USER_CONFIRMATION ")
                startIntentSender(state.resolutionIntent()?.intentSender, null, 0, 0, 0)
            }
            SplitInstallSessionStatus.INSTALLED -> {
                showToast("SplitInstallStateUpdatedListener state: INSTALLED ")
            }

            SplitInstallSessionStatus.INSTALLING -> {
                showToast("SplitInstallStateUpdatedListener state: INSTALLING ")
            }
            SplitInstallSessionStatus.FAILED -> {
                showToast("SplitInstallStateUpdatedListener state: FAILED ")
            }
        }
    }
    override fun onCreate() {
        super.onCreate()
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        manager = SplitInstallManagerFactory.create(this)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (intent.getBooleanExtra(IS_INSTANT_DOWNLOAD,false)){
                startDownloadLibraryInForeground()
            }else {
                startDownloadLibraryInBackground()
            }
        }
        showNotification()
        return START_STICKY
    }

    private fun startDownloadLibraryInBackground() {
        manager?.registerListener(listener)
        val module = getString(R.string.dynamic_feature_title)
        showToast("Loading module $module")
        if (manager?.installedModules?.contains(module) == true){
            showToast("Already $module installed")
            hideNotification()
        } else{
            showToast("Starting install for$module")
            manager?.deferredInstall(listOf(getString(R.string.dynamic_feature_title)))
                ?.addOnSuccessListener { showToast("Loading ${module}") }
                ?.addOnFailureListener { showToast("Error Loading ${module}") }
        }
    }

    private fun startDownloadLibraryInForeground() {
        manager?.registerListener(listener)
        val module = getString(R.string.dynamic_feature_title)
        showToast("Loading module $module")
        if (manager?.installedModules?.contains(module) == true){
            showToast("Already $module installed")
            hideNotification()
        } else{
             showToast("Starting install for$module")

            val request = SplitInstallRequest.newBuilder()
                .addModule(module)
                .build()

            manager?.startInstall(request)
                ?.addOnCompleteListener { showToast("Module ${module} installed") }
                ?.addOnSuccessListener { showToast("Loading ${module}") }
                ?.addOnFailureListener { showToast("Error Loading ${module}") }

        }
    }

    private fun showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = NotificationChannelNames.OTHERS.type
            val importance: Int = NotificationManager.IMPORTANCE_LOW
            val mChannel =
                NotificationChannel(CHANNEL_ID, name, importance)
            mNotificationManager?.createNotificationChannel(mChannel)
        }

        val lNotificationBuilder = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setChannelId(CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Extracting Josh Skills resources...")
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)


        startForeground(NOTIFICATION_ID, lNotificationBuilder.build())
    }

    private fun hideNotification() {
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        stopForeground(true)
        manager?.unregisterListener(listener)
    }

    companion object {

        const val IS_INSTANT_DOWNLOAD = "IS_INSTANT_DOWNLOAD"

        fun startOnDemandFeatureDownloadService(
            context: Context,
            instantDownload:Boolean = false
        ) {
            val intent = Intent(context, OnDemandFeatureDownloadService::class.java).apply {
                putExtra(IS_INSTANT_DOWNLOAD,instantDownload)
            }
            context.startService(intent)
        }
    }

}