package com.joshtalks.badebhaiya.recordedRoomPlayer.di

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.*
import com.joshtalks.badebhaiya.core.io.AppDirectory.copy
import com.joshtalks.badebhaiya.impressions.Records
import com.joshtalks.badebhaiya.repository.CommonRepository
import com.joshtalks.badebhaiya.repository.ConversationRoomRepository
import com.joshtalks.badebhaiya.repository.server.AmazonPolicyResponse
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance
import com.joshtalks.badebhaiya.utils.Utils
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
import java.util.concurrent.TimeoutException

class ProcessRoomRecordingService : Service() {
    private val MAX_NUMBER_OF_RETRIES = 5
    private val fileQueue: BlockingQueue<InputFiles> = ArrayBlockingQueue(100)
    private val mFileUploadHandler = Handler()
    private var mFileUploadTask: VideoMuxingTask? = null
    private var isMuxingRunning = false
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
                    START_AUDIO_PROCESSING -> {
                        if (intent.hasExtra(VIDEO_PATH) && intent.hasExtra(AUDIO_PATH))
                            CoroutineScope(Dispatchers.IO).launch {
                                val videoPath = intent.getStringExtra(VIDEO_PATH)
                                val audioPath = intent.getStringExtra(AUDIO_PATH)
                                val callId = intent.getStringExtra(CALL_ID)
                                val agoraMentorId = intent.getStringExtra(ROOM_ID)
                                val duration = intent.getIntExtra(RECORD_DURATION,0)
                                Log.i("RECORDUPLOAD", "onStartCommand: ")
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
            Log.i("RECORDUPLOAD", "startProcessingAudioVideoMixing: ")
            fileQueue.add(inputFiles)
            startMuxingVideo()
        }
    }

    private fun startMuxingVideo() {
        CoroutineScope(Dispatchers.IO).launch {
            if (mFileUploadTask == null) {
                Log.i("RECORDUPLOAD", "startMuxingVideo: ")
                mFileUploadTask = VideoMuxingTask()
            }
            if (!isMuxingRunning) {
                isMuxingRunning = true
                mFileUploadHandler.post(mFileUploadTask!!)
            }
        }
    }

    internal inner class VideoMuxingTask() : Runnable {

        override fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                Log.i("RECORDUPLOAD", "run: ")
                if (fileQueue.isEmpty()) {
                    isMuxingRunning = false
                    AppObjectController.uiHandler.post {
                        hideNotification()
                    }
                } else {
                    try {
                        val inputFiles = fileQueue.take()
//                        val audioFile = PrefManager.getLastRecordingPath()
//                        copy(inputFiles.audioPath!!, audioFile)
                        Log.i("RECORDUPLOAD", "run: ${inputFiles.audioPath}")

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
                Log.i("RECORDUPLOAD", "uploadOutputVideoToS3Server: ${inputFiles?.audioPath}")
                val requestEngage = inputFiles
                if (inputFiles.audioPath.isNullOrEmpty()==false) {
                    val obj = mapOf(
                        "media_path" to File(inputFiles.audioPath).name
                    )
                    Log.i("RECORDUPLOAD", "uploadOutputVideoToS3Server: ${inputFiles?.audioPath}")

                    val responseObj =
                        CommonRepository().requestUploadMediaAsync(obj).await()
                    val statusCode: Int =
                        uploadOnS3Server(responseObj, inputFiles.audioPath!!)
                    if(statusCode==-1) {
                        Log.i("RECORDUPLOAD", "uploadCompressedMedia: failed")
                        showToast("Error while uploading")
                    }
                    if(statusCode==-1) {
                        Log.i("RECORDUPLOAD", "uploadCompressedMedia: failed timeout")
                        showToast("TimeOut Error while uploading")
                    }
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

                val resp = ConversationRoomRepository().requestUploadRoomRecording(Records(inputFiles.callId?.toInt()!!,inputFiles.serverUrl!!))


            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
//        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
//            try {
//                val obj = mutableMapOf("media_path" to File(inputFiles.audioPath).name)
//                val responseObj =
//                    CommonRepository().requestUploadMediaAsync(obj).await()
//                val statusCode: Int = uploadOnS3Server(responseObj, inputFiles.audioPath!!)
//                if(statusCode==-1) {
//                    Log.i("RECORDUPLOAD", "uploadCompressedMedia: failed")
//                    showToast("Error while uploading")
//                }
//                if (statusCode in 200..210) {
//                    showToast("Uploaded")
//                    Log.i("RECORDUPLOAD", "uploadCompressedMedia: Succeed")
//                    val url = responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
//                    ConversationRoomRepository().requestUploadRoomRecording(Records(inputFiles.callId?.toInt()!!,url))
//                }
//            } catch (ex: Exception) {
//                ex.printStackTrace()
//            }
//        }
    }

    private suspend fun handleRetry(pendingTaskModel: InputFiles) {

    }

    private suspend fun uploadOnS3Server(
        responseObj: AmazonPolicyResponse,
        mediaPath: String
    ): Int {
        return CoroutineScope(Dispatchers.IO).async(Dispatchers.IO) {
            try{
                val parameters = emptyMap<String, RequestBody>().toMutableMap()
                for (entry in responseObj.fields) {
                    parameters[entry.key] = Utils.createPartFromString(entry.value)
                }
//                val requestFile = File(mediaPath).asRequestBody("*".toMediaTypeOrNull())
                val requestFile = File(mediaPath).asRequestBody(mediaPath.toMediaTypeOrNull())

                val body = MultipartBody.Part.createFormData(
                    "file",
                    responseObj.fields["key"],
                    requestFile
                )
                val responseUpload = RetrofitInstance.mediaDUNetworkService.uploadMediaAsync(
                    responseObj.url,
                    parameters,
                    body
                ).execute()
                Log.i(TAG, "uploadOnS3Server: succeed")
                return@async responseUpload.code()
            }
            catch (ex:OutOfMemoryError){
                Log.i("RECORDUPLOAD", "uploadOnS3Server: $ex")
                return@async -1
            }
            catch (ex:TimeoutException){
                Log.i("RECORDUPLOAD", "uploadOnS3Server: $ex")
                return@async -2
            }
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
        const val START_AUDIO_PROCESSING = "START_AUDIO_PROCESSING"
        const val UPLOAD_ALL_CALL_RECORDING = "UPLOAD_ALL_CALL_RECORDING"
        const val VIDEO_PATH = "VIDEO_PATH"
        const val AUDIO_PATH = "AUDIO_PATH"
        const val RECORD_DURATION = "RECORD_DURATION"
        const val CALL_ID = "CALL_ID"
        const val ROOM_ID = "ROOM_ID"
        val TAG = "RecordingService"
        const val CHANNEL_ID = "VIDEO_PROCESSING"
        const val NOTIFICATION_ID = 1201
        const val LOCAL_NOTIFICATION_ID = 1202
        fun uploadAllPendingTasks(context: Context) {
            try {
                val intent = Intent(context, ProcessRoomRecordingService::class.java)
                intent.action = UPLOAD_ALL_CALL_RECORDING
                context.startService(intent)
            } catch (ex: Exception) {
                Timber.d(ex)
            }
        }

        fun processSingleRoomRecording(
            context: Context? = Utils.context,
            callId: String?,
            agoraMentorId: String?,
            videoPath: String,
            audioPath: String,
            recordDuration: Int
        ) {
            Log.i("RECORDUPLOAD", "processSingleRoomRecording: ")
            val intent = Intent(context, ProcessRoomRecordingService::class.java)
            intent.action = START_AUDIO_PROCESSING
            intent.putExtra(CALL_ID, callId)
            intent.putExtra(ROOM_ID, agoraMentorId)
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
    var audioPath: String?,
    var outputFile: File? = null,
    var serverUrl: String? = null,
    var duration: Int? = null
)
