package com.joshtalks.joshskills.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.RequestEngage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.ArrayList
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue


class FileUploadService : Service() {
    private val fileQueue: BlockingQueue<RequestEngage> = ArrayBlockingQueue(100)
    private val mFileUploadHandler = Handler()
    private var mFileUploadTask: FileUploadTask? = null
    private var isFileUploadRunning = false

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        if (!TextUtils.isEmpty(action)) {
            when (action) {
                START_UPLOAD -> {
                    // get all files here to be uploaded
                    val fileList = intent.getParcelableArrayListExtra<RequestEngage?>(FILE_LIST)
                    fileList?.let { startFileUpload(it) }
                }
                CANCEL_UPLOAD -> cancelFileUpload()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun cancelFileUpload() {
        fileQueue.clear()
        if (mFileUploadTask != null) {
            mFileUploadHandler.removeCallbacks(mFileUploadTask)
            isFileUploadRunning = false
            mFileUploadTask = null
            hideNotification()
        }
    }

    private fun startFileUpload(fileList: ArrayList<RequestEngage?>) {
        for (filePath in fileList) {
            if (!fileQueue.contains(filePath) && filePath != null) {
                fileQueue.add(filePath)
            }
        }
        startUploadingFile()
    }

    private fun startUploadingFile() {
        if (mFileUploadTask == null) {
            mFileUploadTask = FileUploadTask()
        }
        if (!isFileUploadRunning) {
            isFileUploadRunning = true
            mFileUploadHandler.post(mFileUploadTask)
        }
    }

    internal inner class FileUploadTask : Runnable {
        override fun run() {
            if (fileQueue.isEmpty()) {
                isFileUploadRunning = false
                Log.e(TAG, "File Upload Complete.")
                hideNotification()
                return
            }
            try {
                val filePath = fileQueue.take()
                Log.e(TAG, "Upload File: $filePath")
                callUploadFileApi(filePath)
                showUploadNotification(filePath)
                Thread.sleep(2000)
                mFileUploadHandler.post(this)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun callUploadFileApi(requestEngage: RequestEngage) {
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            try {
                val localPath = requestEngage.localPath
                if (requestEngage.localPath.isNullOrEmpty().not()) {
                    val obj = mapOf("media_path" to File(requestEngage.localPath!!).name)
                    val responseObj =
                        AppObjectController.chatNetworkService.requestUploadMediaAsync(obj).await()
                    val statusCode: Int = uploadOnS3Server(responseObj, requestEngage.localPath!!)
                    if (statusCode in 200..210) {
                        val url =
                            responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
                        requestEngage.answerUrl = url
                    } else {
                        // want to retry here retry ??
                        return@launch
                    }
                }

                val resp = AppObjectController.chatNetworkService.submitPracticeAsync(requestEngage)
                if (resp.isSuccessful && resp.body() != null) {
                    val engangementList = List(1) { resp.body()!! }
                    // update question status and engagement data here from response

                } else {
                    // want to retry here retry ??
                }
            } catch (ex: Exception) {

            }
        }

    }

    private suspend fun uploadOnS3Server(
        responseObj: AmazonPolicyResponse,
        mediaPath: String
    ): Int {
        return CoroutineScope(Dispatchers.IO).async {
            val parameters = emptyMap<String, RequestBody>().toMutableMap()
            for (entry in responseObj.fields) {
                parameters[entry.key] = Utils.createPartFromString(entry.value)
            }

            val requestFile = File(mediaPath).asRequestBody("*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData(
                "file",
                responseObj.fields["key"],
                requestFile
            )
            val responseUpload = AppObjectController.mediaDUNetworkService.uploadMediaAsync(
                responseObj.url,
                parameters,
                body
            ).execute()
            return@async responseUpload.code()
        }.await()
    }

    private fun hideNotification() {
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        stopForeground(true)
    }

    private fun showUploadNotification(fileName: RequestEngage) {
        var messageText = ""
        if (fileQueue.size > 0) {
            messageText = """$messageText${fileQueue.size} is remaining."""
        }

        val lNotificationBuilder = NotificationCompat.Builder(
            this, CHANNEL_ID.toString()
        )
            .setChannelId(CHANNEL_ID.toString())
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Practise submitting ...")
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setOngoing(false)
            .setColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.colorPrimary
                )
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
        startForeground(0, lNotificationBuilder.build())
    }

    companion object {
        const val START_UPLOAD = "START_UPLOAD"
        const val CANCEL_UPLOAD = "CANCEL_UPLOAD"
        private const val FILE_LIST = "FILE_LIST"
        private val TAG = "FileUploadService"
        private const val CHANNEL_ID = "FILE_UPLOAD"
        private const val NOTIFICATION_ID = 111
        fun startUpload(context: Context, fileList: ArrayList<RequestEngage?>) {
            val intent = Intent(context, FileUploadService::class.java)
            intent.action = START_UPLOAD
            intent.putParcelableArrayListExtra(FILE_LIST, fileList)
            context.startService(intent)
        }

        fun cancelUpload(context: Context) {
            val intent = Intent(context, FileUploadService::class.java)
            intent.action = CANCEL_UPLOAD
            context.startService(intent)
        }
    }
}