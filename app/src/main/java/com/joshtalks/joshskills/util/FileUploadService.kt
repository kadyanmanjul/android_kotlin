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
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.PendingTask
import com.joshtalks.joshskills.repository.local.entity.PendingTaskModel
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.EmptyEventBus
import com.joshtalks.joshskills.repository.local.eventbus.SnackBarEvent
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
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
    private val MAX_NUMBER_OF_RETRIES = 5
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
        try {
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
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
        return START_NOT_STICKY
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
                if (filePath.type == PendingTask.READING_PRACTICE) {
                    callUploadFileApiForReadingPractice(filePath)
                } else {
                    callUploadFileApi(filePath)
                }
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
                    val obj = mapOf("media_path" to File(requestEngage.localPath!!).name,
                        "question_id" to pendingTaskModel.requestObject.questionId)
                    val responseObj =
                        AppObjectController.chatNetworkService.requestUploadMediaAsync(obj).await()

                    if(responseObj.pointsList.isNullOrEmpty().not()){
                        RxBus2.publish(SnackBarEvent(responseObj.pointsList?.get(0),pendingTaskModel.requestObject.questionId))
                    }
                    val statusCode: Int = uploadOnS3Server(responseObj, requestEngage.localPath!!)
                    if (statusCode in 200..210) {
                        val url =
                            responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
                        requestEngage.answerUrl = url
                    } else {
                        handleRetry(pendingTaskModel)
                        return@launch
                    }
                }

                val resp = AppObjectController.chatNetworkService.submitPracticeAsync(requestEngage)
                if (resp.isSuccessful && resp.body() != null) {
                    // update question status and engagement data here from response
                    val engangementList = List(1) {
                        resp.body()!!
                    }
                    val question = AppObjectController.appDatabase.chatDao()
                        .getQuestionOnId(pendingTaskModel.requestObject.questionId)
                    question?.let {
                        question.practiceEngagement = engangementList
                        question.status = QUESTION_STATUS.AT
                        question.practiceEngagement!!.get(0).duration = requestEngage.duration
                        question.practiceEngagement!!.get(0).localPath = requestEngage.localPath
                        AppObjectController.appDatabase.chatDao()
                            .updateQuestionObject(question)
                            .updateQuestionObject(question)

                        if(resp.body()!!.pointsList.isNullOrEmpty().not()){
                            RxBus2.publish(SnackBarEvent(
                                resp.body()!!.pointsList?.get(0),
                                pendingTaskModel.requestObject.questionId
                            ))
                        }
                    }
                    AppObjectController.appDatabase.pendingTaskDao().deleteTask(pendingTaskModel.id)
                } else {
                    handleRetry(pendingTaskModel)
                }
            } catch (ex: Exception) {

            }
        }
    }
    private fun callUploadFileApiForReadingPractice(pendingTaskModel: PendingTaskModel) {
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
                        handleRetry(pendingTaskModel)
                        return@launch
                    }
                }

                val resp =
                    AppObjectController.chatNetworkService.submitNewReadingPractice(requestEngage)
                Log.e(TAG, "callUploadFileApiForReadingPractice: $resp")
                if (resp.isSuccessful && resp.body() != null) {
                    resp.body()?.let {
                        it.questionForId = requestEngage.questionId
                        AppObjectController.appDatabase.practiceEngagementDao()
                            .insertPractiseAfterUploaded(it)
                        RxBus2.publish(EmptyEventBus())
                    }
                    AppObjectController.appDatabase.pendingTaskDao().deleteTask(pendingTaskModel.id)
                } else {
                    handleRetry(pendingTaskModel)
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    private suspend fun handleRetry(pendingTaskModel: PendingTaskModel) {
        if (pendingTaskModel.numberOfRetries < MAX_NUMBER_OF_RETRIES) {
            pendingTaskModel.numberOfRetries++
            AppObjectController.appDatabase.pendingTaskDao()
                .updateRetryCount(
                    pendingTaskModel.id,
                    pendingTaskModel.numberOfRetries
                )
            fileQueue.add(pendingTaskModel)
        } else {
            deleteRequestFromDbAndMarkQuestionIncomplete(pendingTaskModel)

        }
    }

    private suspend fun deleteRequestFromDbAndMarkQuestionIncomplete(pendingTaskModel: PendingTaskModel) {
        val lessonId = AppObjectController.appDatabase.chatDao()
            .getLessonIdOfQuestion(pendingTaskModel.requestObject.questionId)

        AppObjectController.appDatabase.chatDao().updateQuestionAndLessonStatus(
            pendingTaskModel.requestObject.questionId,
            QUESTION_STATUS.NA, LESSON_STATUS.AT
        )
        AppObjectController.appDatabase.lessonDao().updateLessonStatus(
            lessonId,
            LESSON_STATUS.AT
        )

        if (pendingTaskModel.type == PendingTask.READING_PRACTICE) {
            AppObjectController.appDatabase.lessonDao()
                    .updateReadingSectionStatus(lessonId, LESSON_STATUS.NO)
        } else {
            AppObjectController.appDatabase.lessonDao()
                    .updateVocabularySectionStatus(lessonId, LESSON_STATUS.NO)
        }
        AppObjectController.appDatabase.pendingTaskDao()
                .deleteTask(pendingTaskModel.id)
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
                    NotificationChannel(CHANNEL_ID, name, importance)
            mNotificationManager?.createNotificationChannel(mChannel)
        }

        val lNotificationBuilder = NotificationCompat.Builder(
                this,
                CHANNEL_ID
        )
                .setChannelId(CHANNEL_ID)
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

        fun uploadSinglePendingTasks(
                context: Context = AppObjectController.joshApplication,
                insertedTaskLocalId: Long = 0
        ) {
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