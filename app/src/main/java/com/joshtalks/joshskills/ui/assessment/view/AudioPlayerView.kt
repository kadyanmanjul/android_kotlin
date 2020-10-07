package com.joshtalks.joshskills.ui.assessment.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.util.ExoAudioPlayer
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.HttpUrlConnectionDownloader
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.Downloader
import io.reactivex.disposables.CompositeDisposable
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber


class AudioPlayerView : FrameLayout, View.OnClickListener,
    LifecycleObserver,
    ExoAudioPlayer.ProgressUpdateListener, AudioPlayerEventListener {

    private lateinit var playButton: ImageView
    private lateinit var pauseButton: ImageView
    private lateinit var seekPlayerProgress: SeekBar
    private lateinit var timestamp: TextView
    private lateinit var progressWheel: ProgressBar
    private var audioManger: ExoAudioPlayer? = null
    private val context = AppObjectController.joshApplication

    private var id: String = EMPTY
    private var url: String? = null
    private var audioFile: File? = null
    private var mediaDuration: Long? = null
    private var compositeDisposable = CompositeDisposable()
    private val jobs = arrayListOf<Job>()

    private var downloadListener = object : FetchListener {
        override fun onAdded(download: Download) {
        }

        override fun onCancelled(download: Download) {
        }

        override fun onCompleted(download: Download) {
            val fileName = Utils.getFileNameFromURL(url)
            val cacheFile = File(AppObjectController.createDefaultCacheDir(), fileName)
            cacheFile.createNewFile()
            if (AppDirectory.copy(download.file, cacheFile.absolutePath)) {
                audioFile = cacheFile
                audioFile?.run {
                    if (audioManger != null && audioManger!!.isPlaying().not()) {
                        playPause(this)
                    }
                    AppDirectory.deleteFile(download.file)
                }
            }
        }

        override fun onDeleted(download: Download) {
        }

        override fun onDownloadBlockUpdated(
            download: Download,
            downloadBlock: DownloadBlock,
            totalBlocks: Int
        ) {
        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            throwable?.printStackTrace()
            onDownloadIssue()
            progressWheel.visibility = View.GONE
            showToast(context.getString(R.string.something_went_wrong))
        }

        override fun onPaused(download: Download) {
        }

        override fun onProgress(
            download: Download,
            etaInMilliSeconds: Long,
            downloadedBytesPerSecond: Long
        ) {
        }

        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
        }

        override fun onRemoved(download: Download) {
            onDownloadIssue()
        }

        override fun onResumed(download: Download) {
        }

        override fun onStarted(
            download: Download,
            downloadBlocks: List<DownloadBlock>,
            totalBlocks: Int
        ) {
            playButton.visibility = View.GONE
        }


        override fun onWaitingNetwork(download: Download) {
        }


    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    private fun init() {
        AppObjectController.createDefaultCacheDir()
        audioManger = ExoAudioPlayer.getInstance()
        ExoAudioPlayer.LAST_ID = EMPTY
        audioManger?.playerListener = this
        audioManger?.setProgressUpdateListener(this)

        View.inflate(context, R.layout.audio_player_layout, this)
        playButton = findViewById(R.id.btnPlay)
        pauseButton = findViewById(R.id.btnPause)
        seekPlayerProgress = findViewById(R.id.seek_bar)
        progressWheel = findViewById(R.id.progress_bar)
        timestamp = findViewById(R.id.message_time)
        seekPlayerProgress.progress = 0
        playButton.setOnClickListener(this)
        pauseButton.setOnClickListener(this)

        seekPlayerProgress.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                var userSelectedPosition = 0
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        userSelectedPosition = progress
                    }
                    timestamp.text = Utils.formatDuration(progress)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    audioManger?.seekTo(userSelectedPosition.toLong())
                }
            })
    }

    fun setupAudio(id: String, url: String) {
        this.id = id
        this.url = url
        setDefaultValue()
    }

    private fun setDefaultValue() {
        progressWheel.visibility = View.GONE
        pausingAudio()
        seekPlayerProgress.progress = 0
        File(AppObjectController.getAppCachePath() + "/" + Utils.getFileNameFromURL(url)).run {
            configureAudioProperty(this)
        }

    }

    private fun configureAudioProperty(file: File) {
        Utils.getDurationOfMedia(context, file.absolutePath)?.let {
            mediaDuration = it
            seekPlayerProgress.max = it.toInt()
            timestamp.text = Utils.formatDuration(it.toInt())
        }
    }


    override fun onClick(v: View) {
        if (v.id == R.id.btnPlay) {
            val file = if (audioFile != null) {
                audioFile
            } else {
                checkFileInCache()
            }
            progressWheel.visibility = View.VISIBLE
            playButton.visibility = View.GONE
            pauseButton.visibility = View.GONE
            if (file != null) {
                audioFile = file
                playPause(file)
            } else {
                url?.run {
                    downloadAndPlay(this)
                }
            }
        } else if (v.id == R.id.btnPause) {
            pauseAudio()
        }
    }

    fun pauseAudio() {
        audioFile?.run {
            progressWheel.visibility = View.GONE
            playPause(this)
            mediaDuration?.let {
                timestamp.text = Utils.formatDuration(it.toInt())
            }
        }
    }

    private fun playPause(file: File) {
        audioManger?.let {
            if (ExoAudioPlayer.LAST_ID.isEmpty()) {
                initAndPlay(file)
                return@let
            }
            if (ExoAudioPlayer.LAST_ID == id) {
                audioManger?.resumeOrPause()
                if (audioManger?.isPlaying() == true) {
                    playingAudio()
                } else
                    pausingAudio()
            } else {
                initAndPlay(file)
            }
        }
    }

    private fun initAndPlay(file: File) {
        logAudioPlayedEvent(true)
        audioManger?.play(file.absolutePath, id)
        seekPlayerProgress.progress = 0
        playingAudio()
        val duration = Utils.getDurationOfMedia(context, file.absolutePath) ?: 0
        seekPlayerProgress.max = duration.toInt()
    }

    private fun playingAudio() {
        playButton.visibility = View.GONE
        pauseButton.visibility = View.VISIBLE
        progressWheel.visibility = View.GONE
    }

    private fun pausingAudio() {
        playButton.visibility = View.VISIBLE
        pauseButton.visibility = View.GONE
        progressWheel.visibility = View.GONE
    }

    private fun downloadAndPlay(url: String) {
        try {
            logAudioPlayedEvent(false)

            val fileName = Random(1000).nextInt().toString().plus(Utils.getFileNameFromURL(url))
            val cacheFile = File(AppObjectController.createDefaultCacheDir(), fileName)
            cacheFile.createNewFile()
            val request = Request(url, cacheFile.absolutePath)
            request.identifier = Random(1000L).nextLong()
            request.priority = Priority.HIGH
            request.tag = id
            jobs += CoroutineScope(Dispatchers.IO).launch {
                AppObjectController.getFetchObject().remove(request.id)
                AppObjectController.getFetchObject()
                    .addListener(downloadListener)
                    .enqueue(request, {
                    },
                        {
                            it.throwable?.printStackTrace()
                            onDownloadIssue()
                        }).awaitFinishOrTimeout(20000)
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun logAudioPlayedEvent(isDowloaded: Boolean) {
        AppAnalytics.create(AnalyticsEvent.ASSESSMENT_AUDIO_PLAYED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.IS_DOWNLOADED.name, isDowloaded)
            .push()
    }

    private fun checkFileInCache(): File? {
        val fileName =
            File(
                AppObjectController.createDefaultCacheDir() + File.separator + Utils.getFileNameFromURL(
                    url
                )
            )
        if (fileName.canRead()) {
            return fileName
        }
        return null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setDefaultValue()
        Timber.tag("onAttachedToWindow").e("AudioPlayer")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        compositeDisposable.clear()
        setDefaultValue()
        audioManger?.release()
        ExoAudioPlayer.LAST_ID = ""
        AppObjectController.getFetchObject().removeListener(downloadListener)
        Timber.tag("onDetachedFromWindow").e("AudioPlayer")
    }

    fun onDownloadIssue() {
        progressWheel.visibility = View.GONE
        pausingAudio()
    }

    fun getLocalFetch(): Fetch {
        val fetchConfiguration = FetchConfiguration.Builder(AppObjectController.joshApplication)
            .enableRetryOnNetworkGain(true)
            .enableLogging(true)
            .setAutoRetryMaxAttempts(1)
            .enableFileExistChecks(true)
            .enableHashCheck(true)
            .createDownloadFileOnEnqueue(false)
            .setGlobalNetworkType(NetworkType.ALL)
            .setHttpDownloader(HttpUrlConnectionDownloader(Downloader.FileDownloaderType.PARALLEL))
            .build()
        return Fetch.getInstance(fetchConfiguration)

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPausePlayer() {
        audioManger?.onPause()
        pausingAudio()
    }

    override fun onProgressUpdate(progress: Long) {
        if (!JoshApplication.isAppVisible)
            onPausePlayer()
        seekPlayerProgress.progress = progress.toInt()
    }

    override fun onDurationUpdate(duration: Long?) {
    }

    override fun onPlayerPause() {
        pausingAudio()
    }

    override fun onPlayerResume() {
        playingAudio()
    }

    override fun onCurrentTimeUpdated(lastPosition: Long) {
    }

    override fun onTrackChange(tag: String?) {
    }

    override fun onPositionDiscontinuity(lastPos: Long, reason: Int) {
    }

    override fun onPositionDiscontinuity(reason: Int) {
    }

    override fun onPlayerReleased() {
    }

    override fun onPlayerEmptyTrack() {
    }

    override fun complete() {
        seekPlayerProgress.progress = 0
        audioManger?.seekTo(0)
        audioManger?.onPause()
    }
}