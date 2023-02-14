package com.joshtalks.joshskills.core.agora_dynamic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.io.AppDirectory.VIDEO_SENT_MIGRATED_PATH
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.repository.local.model.NotificationChannelData
import com.joshtalks.joshskills.voip.Utils.Companion.courseId
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.Func
import java.io.File
import java.util.HashMap
import kotlin.io.path.toPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class FileDownloadService : Service() {

    private var mNotificationManager: NotificationManager? = null
    var SDCardRoot: String = VIDEO_SENT_MIGRATED_PATH
    var mServerFileListApp = mutableListOf<String>()
    var mDeviceFileListApp = mutableListOf<String?>()
    val objectFetchListener = HashMap<String, FetchListener>()

    private var downloadListener = object : FetchListener {
        override fun onAdded(download: Download) {
            Log.d(TAG, "onAdded() called with: download = $download")
        }

        override fun onCancelled(download: Download) {
            Log.d(TAG, "onCancelled() called with: download = $download")
        }

        override fun onCompleted(download: Download) {
            Log.d(TAG, "onCompleted() called with: download = $download")
        }

        override fun onDeleted(download: Download) {
            Log.d(TAG, "onDeleted() called with: download = $download")
        }

        override fun onDownloadBlockUpdated(
            download: Download,
            downloadBlock: DownloadBlock,
            totalBlocks: Int
        ) {
            Log.d(
                TAG,
                "onDownloadBlockUpdated() called with: download = $download, downloadBlock = $downloadBlock, totalBlocks = $totalBlocks"
            )
        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            Log.d(
                TAG,
                "onError() called with: download = $download, error = $error, throwable = $throwable"
            )
        }

        override fun onPaused(download: Download) {
            Log.d(TAG, "onPaused() called with: download = $download")
        }

        override fun onProgress(
            download: Download,
            etaInMilliSeconds: Long,
            downloadedBytesPerSecond: Long
        ) {
            Log.d(
                TAG,
                "onProgress() called with: download = $download, etaInMilliSeconds = $etaInMilliSeconds, downloadedBytesPerSecond = $downloadedBytesPerSecond"
            )
        }

        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
            Log.d(
                TAG,
                "onQueued() called with: download = $download, waitingOnNetwork = $waitingOnNetwork"
            )
        }

        override fun onRemoved(download: Download) {
            Log.d(TAG, "onRemoved() called with: download = $download")
        }

        override fun onResumed(download: Download) {
            Log.d(TAG, "onResumed() called with: download = $download")
        }

        override fun onStarted(
            download: Download,
            downloadBlocks: List<DownloadBlock>,
            totalBlocks: Int
        ) {
            Log.d(
                TAG,
                "onStarted() called with: download = $download, downloadBlocks = $downloadBlocks, totalBlocks = $totalBlocks"
            )
        }

        override fun onWaitingNetwork(download: Download) {
            Log.d(TAG, "onWaitingNetwork() called with: download = $download")
        }

    }

    override fun onCreate() {
        super.onCreate()
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        showDownloadNotification()
        Toast.makeText(this, " device Services ", Toast.LENGTH_LONG).show()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun showDownloadNotification() {
        val messageText = """${mServerFileListApp.size.minus(mDeviceFileListApp.size)} is remaining."""
        val channelId = NotificationChannelData.UPLOADS.id
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = NotificationChannelData.UPLOADS.type
            val importance: Int = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(channelId, name, importance)
            mNotificationManager?.createNotificationChannel(mChannel)
        }

        val lNotificationBuilder = NotificationCompat.Builder(this, channelId)
            .setChannelId(channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(messageText)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(false)
            .setColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.primary_500
                )
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)

        startForeground(FileDownloadService.NOTIFICATION_ID, lNotificationBuilder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Received start id $startId: $intent")
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        val directory = File(SDCardRoot.toString() +'/'+ Build.SUPPORTED_ABIS[0])

        // create directory if not exists

        // create directory if not exists
        if (!directory.exists()) {
            if (directory.mkdirs()) //directory is created;
                Log.i(TAG, "App dir created")
            else Log.w(TAG, "Unable to create app dir!")
        }

        mDeviceFileListApp = getDeviceFiles()
        Toast.makeText(this, " device file  $mDeviceFileListApp", Toast.LENGTH_LONG).show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp =
                    AppObjectController.commonNetworkService.getAgoraLibPath("x86_64")
                Log.e(TAG, "DownloadLibUrl Called: $resp")
                if (resp.isSuccessful && resp.body() != null) {
                    resp.body()?.let {
                        val sLIst = mutableListOf<String>()
                        sLIst.addAll(it)
                        mServerFileListApp = sLIst
                        mServerFileListApp.forEach {url->
                            startDownloadFiles(url)
                        }
                    }
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
        return START_STICKY
    }

    suspend private fun startDownloadFiles(url: String) {
        val directory = File(SDCardRoot.toString() +File.separator+ Build.SUPPORTED_ABIS[0])
        val name = url.split('/').last()
        val path = File(directory.toString() + File.separator + name)
        if (!directory.exists()) {
            if (directory.mkdirs()) //directory is created;
                Log.i(TAG, "App dir created")
            else Log.w(TAG, "Unable to create app dir!")
        }
        if (!path.exists()) {
            if (path.createNewFile()) //directory is created;
                Log.i(TAG, "path dir created")
            else Log.w(TAG, "path not  to create app dir!")
        }
        Log.d(TAG, "startDownloadFiles() called with: url = $directory $path")
        val request = Request(url, path.toString())
        request.priority = Priority.HIGH
        request.networkType = NetworkType.ALL
        request.tag = url

        AppObjectController.getFetchObject().addListener(downloadListener)
        DownloadUtils.objectFetchListener[url] = downloadListener
        AppObjectController.getFetchObject().remove(request.id)
        AppObjectController.getFetchObject().enqueue(
            request, Func {
                Log.d(TAG, "downloaded called $it")
            },
            Func {
                Log.d(TAG, "startDownloadFiles() on error called $it")
                it.throwable?.printStackTrace()
                request.tag?.let { tag ->
                    DownloadUtils.objectFetchListener[tag]?.let { it1 ->
                        AppObjectController.getFetchObject().removeListener(it1)
                    }
                }
            })
    }

    private fun getDeviceFiles(): ArrayList<String?> {
        val deviceFileList = ArrayList<String?>()
        val directory = File(SDCardRoot.toString() +'/'+ Build.SUPPORTED_ABIS[0])
        if (directory.length() != 0L) // check no of files
        {
            for (file in directory.listFiles()!!) {
                if (file.isFile) deviceFileList.add(file.name)
            }
        }
        return deviceFileList
    }

    companion object {
        private val TAG = "FileDownloadService"
        private const val NOTIFICATION_ID = 241
        const val DOWNLOAD_ALL_FILES = "DOWNLOAD_ALL_FILES"
        const val CANCEL_DOWNLOAD = "CANCEL_DOWNLOAD"

        fun uploadAllPendingTasks(context: Context) {
            try {
                val intent = Intent(context, FileDownloadService::class.java)
                intent.action = DOWNLOAD_ALL_FILES
                context.startService(intent)
            } catch (ex: Exception) {
                Timber.d(ex)
            }
        }

        fun cancelUpload(context: Context) {
            val intent = Intent(context, FileDownloadService::class.java)
            intent.action = CANCEL_DOWNLOAD
            context.startService(intent)
        }
    }
}