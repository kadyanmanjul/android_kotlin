package com.joshtalks.joshskills.ui.voip.new_arch.ui.call_recording

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.text.TextUtils
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.audioVideoMuxer
import com.joshtalks.joshskills.base.copy
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.notification.NotificationUtils
import com.joshtalks.joshskills.repository.local.model.NotificationAction
import com.joshtalks.joshskills.repository.local.model.NotificationChannelNames
import com.joshtalks.joshskills.repository.local.model.NotificationObject
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.voip.data.api.CallRecordingRequest
import com.joshtalks.joshskills.voip.data.api.VoipNetwork
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber

class ProcessCallRecordingService : Service() {
    private val MAX_NUMBER_OF_RETRIES = 5
    private val fileQueue: BlockingQueue<InputFiles> = ArrayBlockingQueue(100)
    private val mFileUploadHandler = Handler()
    private var mFileUploadTask: VideoMuxingTask? = null
    private var isMuxingRunning = false
    private var mNotificationManager: NotificationManager? = null
    private val callApiService by lazy {
        VoipNetwork.getVoipApi()
    }

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
                    START_VIDEO_AUDIO_PROCESSING -> {
                        if (intent.hasExtra(VIDEO_PATH) && intent.hasExtra(AUDIO_PATH))
                            CoroutineScope(Dispatchers.IO).launch {
                                val videoPath = intent.getStringExtra(VIDEO_PATH)
                                val audioPath = intent.getStringExtra(AUDIO_PATH)
                                val callId = intent.getStringExtra(CALL_ID)
                                val agoraMentorId = intent.getStringExtra(AGORA_MENTOR_ID)
                                val duration = intent.getIntExtra(RECORD_DURATION,0)
                                startProcessingAudioVideoMixing(InputFiles(callId, agoraMentorId, videoPath, audioPath,duration = duration))
                            }
                    }
                    UPLOAD_ALL_CALL_RECORDING -> {

                    }
                }
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
        return START_NOT_STICKY
    }

    private fun cancelFileUpload() {
        CoroutineScope(Dispatchers.IO).launch {
            fileQueue.clear()
            if (mFileUploadTask != null) {
                mFileUploadHandler.removeCallbacks(mFileUploadTask!!)
                isMuxingRunning = false
                mFileUploadTask = null
                AppObjectController.uiHandler.post {
                    hideNotification()
                }
            }
        }
    }

    private fun startProcessingAudioVideoMixing(inputFiles: InputFiles) {
        if (inputFiles.callId.isNullOrBlank() || inputFiles.audioPath.isNullOrBlank() || inputFiles.videoPath.isNullOrBlank()) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            fileQueue.add(inputFiles)
            startMuxingVideo()
        }
    }

    private fun startMuxingVideo() {
        CoroutineScope(Dispatchers.IO).launch {
            if (mFileUploadTask == null) {
                mFileUploadTask = VideoMuxingTask()
            }
            if (!isMuxingRunning) {
                isMuxingRunning = true
                mFileUploadHandler.post(mFileUploadTask!!)
            }
        }
    }

    internal inner class VideoMuxingTask : Runnable {
        override fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                if (fileQueue.isEmpty()) {
                    isMuxingRunning = false
                    AppObjectController.uiHandler.post {
                        hideNotification()
                    }
                } else {
                    try {
                        val inputFiles = fileQueue.take()
                        // val audioFile = getAudioSentFile(context = applicationContext, null)
                        val audioFile = AppDirectory.getAudioSentFile(null)
                        copy(inputFiles.audioPath!!, audioFile.absolutePath)
                        AppObjectController.uiHandler.post {
                            showUploadNotification()
                        }
                        val outputFile = audioVideoMuxer(audioFile, File(inputFiles.videoPath!!), applicationContext)
                        if (outputFile.isNullOrBlank().not()) {
                            inputFiles.outputFile = File(outputFile!!)
                        }
                        uploadOutputVideoToS3Server(inputFiles)
                        delay(1000)
                        mFileUploadHandler.post(this@VideoMuxingTask)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun uploadOutputVideoToS3Server(inputFiles: InputFiles) {
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            try {
                val requestEngage = inputFiles
                if (inputFiles?.outputFile?.absolutePath?.isNullOrEmpty()?.not() == true) {
                    val obj = mapOf(
                        "media_path" to inputFiles.outputFile!!.name,
                    )
                    val responseObj =
                        AppObjectController.chatNetworkService.requestUploadMediaAsync(obj)
                            .await()
                    val statusCode: Int =
                        uploadOnS3Server(responseObj, inputFiles.outputFile!!.absolutePath)
                    if (statusCode in 200..210) {
                        val url =
                            responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
                        requestEngage.serverUrl = url
                    } else {
                        handleRetry(inputFiles)
                        return@launch
                    }
                }
                if (requestEngage.serverUrl.isNullOrBlank()) {
                    return@launch
                }

                val resp =
                    AppObjectController.chatNetworkService.postCallRecordingFile(
                        CallRecordingRequest(
                            agoraCallId = inputFiles.callId,
                            agoraMentorId = inputFiles.agoraMentorId,
                            recording_url = inputFiles.serverUrl!!,
                            duration = inputFiles.duration?:0
                        )
                    )
                if (resp.isSuccessful && resp.body() != null) {
                    val nc = NotificationObject().apply {
                        contentTitle = "Processed Call Recording"
                        contentText = "Well done!, Here is your call recording"
                        action = NotificationAction.CALL_RECORDING_NOTIFICATION
                        extraData = requestEngage.outputFile?.absolutePath?: EMPTY
                    }
                    NotificationUtils(applicationContext).sendNotification(nc)
                } else {
                    handleRetry(inputFiles)
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    private suspend fun handleRetry(pendingTaskModel: InputFiles) {

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

    private fun showUploadNotification() {
        var messageText = ""
        if (fileQueue.size > 0) {
            messageText = """$messageText${fileQueue.size} is remaining."""
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = NotificationChannelNames.OTHERS.type
            val importance: Int = NotificationManager.IMPORTANCE_LOW
            val mChannel =
                NotificationChannel(CHANNEL_ID, name, importance)
            mNotificationManager?.createNotificationChannel(mChannel)
        }

        val lNotificationBuilder = ContextCompat.getColor(AppObjectController.joshApplication, R.color.colorPrimary).let {
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setChannelId(CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Call Recording is generating...")
                .setSmallIcon(R.drawable.ic_status_bar_notification)
                .setOngoing(false)
                .setColor(
                    it
                )
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
        }


        startForeground(NOTIFICATION_ID, lNotificationBuilder.build())
    }

    companion object {
        const val START_VIDEO_AUDIO_PROCESSING = "START_VIDEO_AUDIO_PROCESSING"
        const val UPLOAD_ALL_CALL_RECORDING = "UPLOAD_ALL_CALL_RECORDING"
        const val VIDEO_PATH = "VIDEO_PATH"
        const val AUDIO_PATH = "AUDIO_PATH"
        const val RECORD_DURATION = "RECORD_DURATION"
        const val CALL_ID = "CALL_ID"
        const val AGORA_MENTOR_ID = "AGORA_MENTOR_ID"
        val TAG = "RecordingService"
        const val CHANNEL_ID = "VIDEO_PROCESSING"
        const val NOTIFICATION_ID = 1201
        const val LOCAL_NOTIFICATION_ID = 1202
        fun uploadAllPendingTasks(context: Context) {
            try {
                val intent = Intent(context, ProcessCallRecordingService::class.java)
                intent.action = UPLOAD_ALL_CALL_RECORDING
                context.startService(intent)
            } catch (ex: Exception) {
                Timber.d(ex)
            }
        }

        fun processSingleCallRecording(
            context: Context? = com.joshtalks.joshskills.voip.Utils.context,
            callId: String?,
            agoraMentorId: String?,
            videoPath: String,
            audioPath: String,
            recordDuration: Int
        ) {
            val intent = Intent(context, ProcessCallRecordingService::class.java)
            intent.action = START_VIDEO_AUDIO_PROCESSING
            intent.putExtra(CALL_ID, callId)
            intent.putExtra(AGORA_MENTOR_ID, agoraMentorId)
            intent.putExtra(VIDEO_PATH, videoPath)
            intent.putExtra(AUDIO_PATH, audioPath)
            intent.putExtra(RECORD_DURATION,recordDuration)
            context?.startService(intent)
        }
    }
}

data class InputFiles(
    val callId: String?,
    val agoraMentorId: String?,
    val videoPath: String?,
    val audioPath: String?,
    var outputFile: File? = null,
    var serverUrl: String? = null,
    var duration: Int? = null
)
