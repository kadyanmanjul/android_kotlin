package com.joshtalks.joshskills.ui.chat.service


import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.media.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.DOWNLOAD_OBJECT
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonMaterialType
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion
import com.joshtalks.joshskills.repository.local.eventbus.DownloadMediaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.DownloadMediaEventBusForLessonQuestion
import com.joshtalks.joshskills.repository.local.model.NotificationChannelNames
import com.joshtalks.joshskills.ui.voip.*
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.Extras
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import timber.log.Timber


const val DOWNLOAD_CHAT_OBJECT = "chat_obj"
const val DOWNLOAD_LESSON_QUESTION_OBJECT = "lesson_question_obj"
const val DOWNLOAD_FILE_URL = "file_url"

const val DOWNLOAD_NOTIFICATION_CHANNEL = "Download Media "
private var notificationChannelId = "101133"

class DownloadMediaService : Service(), FetchListener {

    private var mNotificationManager: NotificationManager? = null
    private val mBinder: IBinder = MyBinder()
    private val downloadService: ExecutorService =
        JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Calling Service")

    private val jsonMapper = AppObjectController.gsonMapperForLocal
    private val fetch = AppObjectController.getFetchObject()
    private var downloadCount = 0
    private val chatDao = AppObjectController.appDatabase.chatDao()
    private val lessonQuestionDao = AppObjectController.appDatabase.lessonQuestionDao()

    companion object {
        private val TAG = DownloadMediaService::class.java.simpleName

        @Volatile
        private var callback: WeakReference<DownloadServiceCallback>? = null


        fun addDownload(chatModel: ChatModel?, url: String?) {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                DownloadMediaService::class.java
            ).apply {
                action = "Download Media"
                putExtra(DOWNLOAD_CHAT_OBJECT, chatModel)
                putExtra(DOWNLOAD_FILE_URL, url)
            }
            if (JoshApplication.isAppVisible) {
                AppObjectController.joshApplication.startService(serviceIntent)
            } else {
                ContextCompat.startForegroundService(
                    AppObjectController.joshApplication,
                    serviceIntent
                )
            }
        }

        fun addDownload(lessonQuestion: LessonQuestion?, url: String?) {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                DownloadMediaService::class.java
            ).apply {
                action = "Download Media"
                putExtra(DOWNLOAD_LESSON_QUESTION_OBJECT, lessonQuestion)
                putExtra(DOWNLOAD_FILE_URL, url)
            }
            if (JoshApplication.isAppVisible) {
                AppObjectController.joshApplication.startService(serviceIntent)
            } else {
                ContextCompat.startForegroundService(
                    AppObjectController.joshApplication,
                    serviceIntent
                )
            }
        }
    }


    inner class MyBinder : Binder() {
        fun getService(): DownloadMediaService {
            return this@DownloadMediaService
        }
    }

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).e("onCreate")
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        fetch.addListener(this)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).e("onStartCommand=  %s", intent?.action)
        downloadService.execute {
            intent?.let {
                if (it.hasExtra(DOWNLOAD_CHAT_OBJECT)) {
                    val data = it.getParcelableExtra(DOWNLOAD_CHAT_OBJECT) as ChatModel?
                    showNotification(
                        downloadNotification(),
                        NotificationId.INCOMING_CALL_NOTIFICATION_ID
                    )
                    val url = it.getStringExtra(DOWNLOAD_FILE_URL)!!
                    val localAudioFile = AppDirectory.getAudioReceivedFile(url).absolutePath
                    data?.run {
                        addDownload(this, url, localAudioFile)
                    }
                } else if (it.hasExtra(DOWNLOAD_LESSON_QUESTION_OBJECT)) {
                    val data =
                        it.getParcelableExtra(DOWNLOAD_LESSON_QUESTION_OBJECT) as LessonQuestion?
                    showNotification(
                        downloadNotification(),
                        NotificationId.INCOMING_CALL_NOTIFICATION_ID
                    )
                    val url = it.getStringExtra(DOWNLOAD_FILE_URL)!!
                    val localAudioFile = AppDirectory.getAudioReceivedFile(url).absolutePath
                    data?.run {
                        addDownload(this, url, localAudioFile)
                    }
                } else {
                    return@execute
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun addDownload(chatModel: ChatModel, source: String, destination: String) {
        downloadService.execute {
            downloadCount++
            val request = Request(source, destination)
            request.priority = Priority.HIGH
            request.networkType = NetworkType.ALL
            request.tag = chatModel.chatId
            request.extras = objToExtras(chatModel)
            fetch.remove(request.id)
            fetch.enqueue(request, {
                Timber.tag(TAG).e("Request   " + it.file + "  " + it.url)
            },
                {
                    downloadCount--
                    Timber.tag(TAG).e("error  ")
                    it.throwable?.printStackTrace()
                }).awaitFinishOrTimeout(20000)
        }
    }

    private fun addDownload(lessonQuestion: LessonQuestion, source: String, destination: String) {
        downloadService.execute {
            downloadCount++
            val request = Request(source, destination)
            request.priority = Priority.HIGH
            request.networkType = NetworkType.ALL
            request.tag = lessonQuestion.id
            request.extras = objToExtras(lessonQuestion)
            fetch.remove(request.id)
            fetch.enqueue(request, {
                Timber.tag(TAG).e("Request   " + it.file + "  " + it.url)
            },
                {
                    downloadCount--
                    Timber.tag(TAG).e("error  ")
                    it.throwable?.printStackTrace()
                }).awaitFinishOrTimeout(20000)
        }
    }

    private fun objToExtras(chatModel: ChatModel): Extras {
        return Extras(mapOf(DOWNLOAD_OBJECT to jsonMapper.toJson(chatModel)))
    }

    private fun objToExtras(lessonQuestion: LessonQuestion): Extras {
        return Extras(mapOf(DOWNLOAD_OBJECT to jsonMapper.toJson(lessonQuestion)))
    }


    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    fun addListener(down: DownloadServiceCallback?) {
        callback = WeakReference(down)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopForeground(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadService.shutdown()
        fetch.close()
    }

    private fun showNotification(notification: Notification, notificationId: Int) {
        startForeground(notificationId, notification)
    }

    private fun downloadNotification(title: String = "Downloading Media"): Notification {
        Timber.tag(TAG).e("actionNotification ")

        val lNotificationBuilder = NotificationCompat.Builder(this, DOWNLOAD_NOTIFICATION_CHANNEL)
            .setChannelId(DOWNLOAD_NOTIFICATION_CHANNEL)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.colorPrimary
                )
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(0, 0, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                notificationChannelId,
                NotificationChannelNames.DEFAULT.type,
                NotificationManager.IMPORTANCE_MIN
            )
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            lNotificationBuilder.setChannelId(notificationChannelId)
            mNotificationManager?.createNotificationChannel(notificationChannel)
        }

        lNotificationBuilder.priority = NotificationCompat.PRIORITY_LOW
        return lNotificationBuilder.build()
    }

    private fun removeNotifications() {
        try {
            mNotificationManager?.cancelAll()
            stopForeground(true)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onAdded(download: Download) {
        Timber.tag(TAG).e("onAdded     " + download.tag)

    }

    override fun onCancelled(download: Download) {
        Timber.tag(TAG).e("onCancelled     " + download.tag)
        downloadCount--

    }

    override fun onCompleted(download: Download) {
        downloadCount--
        Timber.tag(TAG).e("onCompleted     " + download.tag)
        updateDownloadStatus(download.file, download.extras, download.tag)

    }

    override fun onDeleted(download: Download) {
        Timber.tag(TAG).e("onDeleted     " + download.tag)

    }

    override fun onDownloadBlockUpdated(
        download: Download,
        downloadBlock: DownloadBlock,
        totalBlocks: Int
    ) {
        Timber.tag(TAG).e("onDownloadBlockUpdated     " + download.tag)

    }

    override fun onError(download: Download, error: Error, throwable: Throwable?) {
        Timber.tag(TAG).e("onError     " + download.tag)
        downloadCount--
    }

    override fun onPaused(download: Download) {
        Timber.tag(TAG).e("onPaused     " + download.tag)

    }

    override fun onProgress(
        download: Download,
        etaInMilliSeconds: Long,
        downloadedBytesPerSecond: Long
    ) {
        Timber.tag(TAG).e("onProgress     " + download.tag)

    }

    override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
        Timber.tag(TAG).e("onQueued     " + download.tag)

    }

    override fun onRemoved(download: Download) {
        Timber.tag(TAG).e("onRemoved     " + download.tag)

    }

    override fun onResumed(download: Download) {
        Timber.tag(TAG).e("onResumed     " + download.tag)
        downloadCount++
    }

    override fun onStarted(
        download: Download,
        downloadBlocks: List<DownloadBlock>,
        totalBlocks: Int
    ) {
        Timber.tag(TAG).e("onStarted     " + download.tag)

    }

    override fun onWaitingNetwork(download: Download) {
        Timber.tag(TAG).e("onWaitingNetwork     " + download.tag)

    }

    private fun updateDownloadStatus(filePath: String, extras: Extras, tag: String?) {
        downloadService.execute {
            var type = BASE_MESSAGE_TYPE.PD
            var lessonMaterialType = LessonMaterialType.PD
            var chatModel: ChatModel? = null
            try {
                chatModel = jsonMapper.fromJson<ChatModel>(
                    extras.map[DOWNLOAD_OBJECT],
                    DownloadUtils.CHAT_MODEL_TYPE_TOKEN
                )
                chatModel.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
                if (chatModel.type == BASE_MESSAGE_TYPE.Q) {
                    chatModel.question?.let { question ->
                        when (question.material_type) {
                            BASE_MESSAGE_TYPE.AU ->
                                question.audioList?.getOrNull(0)?.let { audioType ->
                                    chatDao.updateAudioPath(audioType.id, filePath)
                                }
                            BASE_MESSAGE_TYPE.PD ->
                                question.pdfList?.getOrNull(0)?.let { pdfType ->
                                    chatDao.updatePdfPath(pdfType.id, filePath)
                                }
                            else -> return@let
                        }
                    }
                } else {
                    chatModel.downloadedLocalPath = filePath
                }

                var duration = 0
                if (filePath.contains(".pdf").not()) {
                    type = BASE_MESSAGE_TYPE.AU
                    duration = Utils.getDurationOfMedia(this, filePath)?.toInt() ?: 0
                }

                chatDao.updateDownloadStatus(
                    chatModel.chatId,
                    DOWNLOAD_STATUS.DOWNLOADED,
                    path = filePath,
                    duration = duration
                )
            } catch (ex: Exception) {
                Timber.d(ex)
            }
            try {
                if (chatModel == null) {
                    val lessonQuestion = jsonMapper.fromJson<LessonQuestion>(
                        extras.map[DOWNLOAD_OBJECT],
                        DownloadUtils.LESSON_QUESTION_TYPE_TOKEN
                    )
                    lessonQuestion.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
                    when (lessonQuestion.materialType) {
                        LessonMaterialType.AU -> {
                            lessonMaterialType = LessonMaterialType.AU
                            lessonQuestion?.audioList?.getOrNull(0)?.let { audioType ->
                                chatDao.updateAudioPath(audioType.id, filePath)
                            }
                        }
                        LessonMaterialType.PD -> {
                            lessonMaterialType = LessonMaterialType.PD
                            lessonQuestion?.pdfList?.getOrNull(0)?.let { pdfType ->
                                chatDao.updatePdfPath(pdfType.id, filePath)
                            }
                        }
                        LessonMaterialType.VI -> {
                            lessonMaterialType = LessonMaterialType.VI
                            lessonQuestion?.videoList?.getOrNull(0)?.let { videoType ->
                                chatDao.updateVideoDownloadStatus(videoType.id, filePath)
                            }
                        }
                    }

                    var duration = 0
                    if (filePath.contains(".pdf").not()) {
                        duration = Utils.getDurationOfMedia(this, filePath)?.toInt() ?: 0
                    }

                    lessonQuestionDao.updateDownloadStatus(
                        lessonQuestion.id,
                        DOWNLOAD_STATUS.DOWNLOADED,
                        path = filePath,
                        // duration = duration
                    )
                }
            } catch (ex: Exception) {
                Timber.d(ex)
            }
            tag?.let {
                if (chatModel == null) {
                    RxBus2.publish(
                        DownloadMediaEventBusForLessonQuestion(
                            DOWNLOAD_STATUS.DOWNLOADED,
                            it,
                            lessonMaterialType
                        )
                    )
                } else {
                    RxBus2.publish(DownloadMediaEventBus(DOWNLOAD_STATUS.DOWNLOADED, it, type))
                }
            }
            if (downloadCount == 0) {
                removeNotifications()
            }
        }
    }
}

interface DownloadServiceCallback {
    fun onChannelJoin() {}
}