package com.joshtalks.joshskills.ui.assessment.view

import android.content.Context
import android.support.v4.media.session.PlaybackStateCompat
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.showToast
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
import com.tonyodev.fetch2core.Func
import dm.audiostreamer.AudioStreamingManager
import dm.audiostreamer.CurrentSessionCallback
import dm.audiostreamer.MediaMetaData
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import kotlin.random.Random


class AudioPlayerView : FrameLayout, View.OnClickListener, CurrentSessionCallback {

    private lateinit var playButton: ImageView
    private lateinit var pauseButton: ImageView
    private lateinit var seekPlayerProgress: SeekBar
    private lateinit var timestamp: TextView
    private lateinit var progressWheel: ProgressBar
    private var streamingManager: AudioStreamingManager? = null
    private val context = AppObjectController.joshApplication

    private var id: String? = null
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
                    if ((streamingManager != null && streamingManager!!.isPlaying).not()) {
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
        streamingManager = AudioStreamingManager.getInstance(AppObjectController.joshApplication)
        streamingManager?.subscribesCallBack(this)
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
                    streamingManager?.onSeekTo(userSelectedPosition.toLong())
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
            if (file != null) {
                audioFile = file
                playPause(file)
            } else {
                url?.run {
                    downloadAndPlay(this)
                }
            }
        } else if (v.id == R.id.btnPause) {
            audioFile?.run {
                progressWheel.visibility = View.GONE
                playPause(this)
                mediaDuration?.let {
                    timestamp.text = Utils.formatDuration(it.toInt())
                }
            }
        }
    }

    private fun playPause(file: File) {
        streamingManager?.let {
            if (streamingManager?.currentAudio == null) {
                initAndPlay(file)
                return@let
            }
            if (streamingManager?.currentAudioId == id) {
                if (it.isPlaying) {
                    streamingManager?.handlePauseRequest()
                } else {
                    streamingManager?.handlePlayRequest()
                }
            } else {
                initAndPlay(file)
            }
        }
    }

    private fun initAndPlay(file: File) {
        logAudioPlayedEvent(true)
        val audioMediaMetaData = MediaMetaData()
        audioMediaMetaData.mediaId = id
        audioMediaMetaData.mediaUrl = file.absolutePath
        audioMediaMetaData.mediaDuration = 10_000.toString()
        streamingManager?.isPlayMultiple = false
        val duration = Utils.getDurationOfMedia(context, file.absolutePath) ?: 0
        mediaDuration = duration
        audioMediaMetaData.mediaDuration = this.toString()
        seekPlayerProgress.progress = 0
        seekPlayerProgress.max = duration.toInt()
        timestamp.text = Utils.formatDuration(duration.toInt())
        streamingManager?.onPlay(audioMediaMetaData)
        streamingManager?.setShowPlayerNotification(false)
    }

    override fun currentSeekBarPosition(progress: Int) {
        seekPlayerProgress.progress = progress
    }

    override fun playSongComplete() {
        seekPlayerProgress.progress = 0
        pausingAudio()
    }

    override fun playNext(indexP: Int, currentAudio: MediaMetaData?) {
        progressWheel.visibility = View.GONE
    }

    override fun updatePlaybackState(state: Int) {
        when (state) {
            PlaybackStateCompat.STATE_PLAYING -> {
                progressWheel.visibility = View.GONE
                playingAudio()
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                pausingAudio()
            }
            PlaybackStateCompat.STATE_STOPPED -> {
                seekPlayerProgress.progress = 0
                pausingAudio()
            }
            PlaybackStateCompat.STATE_BUFFERING -> {
                //  progressWheel.visibility = View.VISIBLE
            }
        }
    }

    override fun playCurrent(indexP: Int, currentAudio: MediaMetaData?) {

    }

    override fun playPrevious(indexP: Int, currentAudio: MediaMetaData?) {

    }

    private fun playingAudio() {
        playButton.visibility = View.GONE
        pauseButton.visibility = View.VISIBLE
    }

    private fun pausingAudio() {
        playButton.visibility = View.VISIBLE
        pauseButton.visibility = View.GONE
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
                AppObjectController.getFetchObject()
                    .addListener(downloadListener)
                    .enqueue(request, Func {
                    },
                        Func {
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
        streamingManager?.unSubscribeCallBack()
        streamingManager?.handlePauseRequest()
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
}