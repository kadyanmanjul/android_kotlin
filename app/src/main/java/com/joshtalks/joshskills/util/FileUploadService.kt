package com.joshtalks.joshskills.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.entity.PendingTaskModel
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.ui.voip.NotificationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue


class FileUploadService : Service() {
    private val fileQueue: BlockingQueue<PendingTaskModel> = ArrayBlockingQueue(100)
    private val mFileUploadHandler = Handler()
    private var mFileUploadTask: FileUploadTask? = null
    private var isFileUploadRunning = false
    private var mNotificationManager: NotificationManager? = null


    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        if (!TextUtils.isEmpty(action)) {
            when (action) {
                START_UPLOAD -> {
                    if (intent.hasExtra(NEW_TASK_MODEL))
                        CoroutineScope(Dispatchers.IO).launch {
                            startFileUpload(
                                AppObjectController.appDatabase.pendingTaskDao()
                                    .getTask(intent.getLongExtra(NEW_TASK_MODEL, -1L))
                            )
                        }
                }
                UPLOAD_ALL_PENDING -> {
                    // get all files here to be uploaded
                    CoroutineScope(Dispatchers.IO).launch {
                        val fileList =
                            AppObjectController.appDatabase.pendingTaskDao().getPendingTasks()
                        if (fileList.isNullOrEmpty().not()) {
                            startFileUpload(ArrayList(fileList))
                        }
                    }
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

    private fun startFileUpload(fileList: ArrayList<PendingTaskModel?>) {
        for (filePath in fileList) {
            if (!fileQueue.contains(filePath) && filePath != null) {
                fileQueue.add(filePath)
            }
        }
        startUploadingFile()
    }

    private fun startFileUpload(pendingTask: PendingTaskModel?) {
        pendingTask?.let {
            fileQueue.add(it)
            startUploadingFile()
        }
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
                showUploadNotification()
                Thread.sleep(2000)
                mFileUploadHandler.post(this)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun callUploadFileApi(pendingTaskModel: PendingTaskModel) {
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            try {
                val requestEngage = pendingTaskModel.requestObject
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
                    // update question status and engagement data here from response
                    val engangementList = List(1) { resp.body()!! }
                    val question = AppObjectController.appDatabase.chatDao()
                        .getQuestionOnId(pendingTaskModel.requestObject.question)
                    question?.let {
                        question.practiceEngagement = engangementList
                        question.status = QUESTION_STATUS.AT

                        AppObjectController.appDatabase.chatDao()
                            .updateQuestionObject(question)
                    }
                    AppObjectController.appDatabase.pendingTaskDao().deleteTask(pendingTaskModel.id)
                } else {
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


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private fun showUploadNotification() {
        var messageText = ""
        if (fileQueue.size > 0) {
            messageText = """$messageText${fileQueue.size} is remaining."""
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Voip Login User"
            val importance: Int = NotificationManager.IMPORTANCE_LOW
            val mChannel =
                NotificationChannel(NotificationId.INCOMING_CALL_CHANNEL_ID, name, importance)
            mNotificationManager?.createNotificationChannel(mChannel)
        }

        val lNotificationBuilder = NotificationCompat.Builder(
            this,
            NotificationId.INCOMING_CALL_CHANNEL_ID
        )
            .setChannelId(NotificationId.INCOMING_CALL_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Submitting a practice...")
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


        startForeground(NOTIFICATION_ID, lNotificationBuilder.build())
    }

    companion object {
        const val START_UPLOAD = "START_UPLOAD"
        const val CANCEL_UPLOAD = "CANCEL_UPLOAD"
        const val UPLOAD_ALL_PENDING = "UPLOAD_ALL_PENDING"
        const val NEW_TASK_MODEL = "UPLOAD_ALL_PENDING"
        private val TAG = "FileUploadService"
        private const val CHANNEL_ID = "FILE_UPLOAD"
        private const val NOTIFICATION_ID = 111
        fun uploadAllPendingTasks(context: Context) {
            val intent = Intent(context, FileUploadService::class.java)
            intent.action = UPLOAD_ALL_PENDING
            context.startService(intent)
        }

        fun uploadSinglePendingTasks(context: Context, insertedTaskLocalId: Long) {
            val intent = Intent(context, FileUploadService::class.java)
            intent.action = START_UPLOAD
            intent.putExtra(NEW_TASK_MODEL, insertedTaskLocalId)
            context.startService(intent)
        }

        fun cancelUpload(context: Context) {
            val intent = Intent(context, FileUploadService::class.java)
            intent.action = CANCEL_UPLOAD
            context.startService(intent)
        }
    }
}