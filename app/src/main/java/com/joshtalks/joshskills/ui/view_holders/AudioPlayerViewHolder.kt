package com.joshtalks.joshskills.ui.view_holders


import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.audioplayer.general.PlayerUtil
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.playback.PlaybackInfoListener
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.AudioPlayEventBus
import com.joshtalks.joshskills.repository.local.eventbus.DownloadCompletedEventBus
import com.joshtalks.joshskills.repository.local.eventbus.DownloadMediaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.InternalSeekBarProgressEventBus
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.mindorks.placeholderview.annotations.*
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2core.DownloadBlock
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.*

@Layout(R.layout.audio_player_view)
class AudioPlayerViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel) :
    BaseChatViewHolder(activityRef, message) {

    @View(R.id.audio_view)
    lateinit var audioView: RelativeLayout

    @View(R.id.root_view)
    lateinit var rootView: RelativeLayout

    @View(R.id.audio_view_sent)
    lateinit var audioViewSent: android.view.View

    @View(R.id.audio_view_received)
    lateinit var audioViewReceived: android.view.View

    @View(R.id.profile_image)
    lateinit var profileImage: AppCompatImageView

    @View(R.id.download_container)
    lateinit var downloadContainer: FrameLayout

    @View(R.id.start_download_iv)
    lateinit var startDownloadImageView: AppCompatImageView


    @View(R.id.progress_bar)
    lateinit var progressBar: ProgressBar

    @View(R.id.cancel_download_iv)
    lateinit var cancelDownloadImageView: AppCompatImageView

    @View(R.id.btnPlay)
    lateinit var btnPlayImageView: AppCompatImageView

    @View(R.id.btnPause)
    lateinit var btnPauseImageView: AppCompatImageView

    @View(R.id.seekBar_ph)
    lateinit var seekBarPlaceHolder: android.view.View

    @View(R.id.seekBar)
    lateinit var seekBar: SeekBar


    @View(R.id.txtCurrentDuration)
    lateinit var txtCurrentDurationTV: AppCompatTextView

    @View(R.id.message_time)
    lateinit var messageTimeTV: AppCompatTextView

    @View(R.id.seekBar_thumb_virtual)
    lateinit var seekBarThumb: AppCompatImageView

    private var animBlink: Animation? = null
    lateinit var audioPlayerViewHolder: AudioPlayerViewHolder
    private var duration: Int = 0
    private val compositeDisposable = CompositeDisposable()


    private var downloadListener = object : FetchListener {
        override fun onAdded(download: Download) {

        }

        override fun onCancelled(download: Download) {

        }

        override fun onCompleted(download: Download) {
            AppAnalytics.create(AnalyticsEvent.AUDIO_DOWNLOAD.NAME)
                .addParam("ChatId", message.chatId).push()

            DownloadUtils.removeCallbackListener(download.tag)
            CoroutineScope(Dispatchers.IO).launch {
                DownloadUtils.updateDownloadStatus(download.file, download.extras).let {
                    RxBus2.publish(DownloadCompletedEventBus(audioPlayerViewHolder, message))
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

        }

        override fun onResumed(download: Download) {

        }

        override fun onStarted(
            download: Download,
            downloadBlocks: List<DownloadBlock>,
            totalBlocks: Int
        ) {

        }

        override fun onWaitingNetwork(download: Download) {
        }

    }


    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        animBlink = AnimationUtils.loadAnimation(activityRef.get()!!, R.anim.blink)
        profileImage.setImageResource(R.drawable.ic_user_rec_placeholder)
        this.audioPlayerViewHolder = this
        seekBar.progress = 0
        txtCurrentDurationTV.text = EMPTY
        audioViewSent.visibility = android.view.View.GONE
        audioViewReceived.visibility = android.view.View.GONE
        seekBar.visibility = android.view.View.INVISIBLE
        downloadContainer.visibility = android.view.View.VISIBLE
        progressBar.visibility = android.view.View.INVISIBLE
        cancelDownloadImageView.visibility = android.view.View.INVISIBLE
        startDownloadImageView.visibility = android.view.View.INVISIBLE
        btnPlayImageView.visibility = android.view.View.INVISIBLE
        btnPauseImageView.visibility = android.view.View.INVISIBLE
        seekBarPlaceHolder.visibility = android.view.View.INVISIBLE
        seekBarThumb.visibility = android.view.View.INVISIBLE


        message.sender?.let {
            if (it.id.equals(getUserId(), ignoreCase = true)) {

                val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(
                    com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 80f),
                    0,
                    com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 7f),
                    0
                )
                params.gravity = Gravity.END
                rootView.layoutParams = params
                rootView.setBackgroundResource(R.drawable.balloon_outgoing_normal)
                audioViewSent.visibility = android.view.View.VISIBLE
            } else {
                val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.gravity = Gravity.START
                params.setMargins(
                    com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 7f),
                    0,
                    com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 80f),
                    0
                )
                rootView.layoutParams = params
                rootView.setBackgroundResource(R.drawable.balloon_incoming_normal)
                audioViewReceived.visibility = android.view.View.VISIBLE
            }

        }


        updateTime(messageTimeTV)
        messageTimeTV.text =
            Utils.getMessageTimeInHours(message.created).toUpperCase(Locale.getDefault())


        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                var userSelectedPosition = 0
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        userSelectedPosition = progress
                    }
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    RxBus2.publish(InternalSeekBarProgressEventBus(userSelectedPosition))
                }
            })
        updateUI()
        audioPlayingStatus()

        // subscribeAudioPlayer()

    }

    private fun audioPlayingStatus() {
        seekBarThumb.clearAnimation()
        AppObjectController.currentPlayingAudioObject?.run {
            try {

                if (this.chatId == message.chatId) {
                    val ref = activityRef.get() as ConversationActivity
                    if (ref.isAudioPlaying()) {
                        seekBarThumb.startAnimation(animBlink)
                        btnPauseImageView.visibility = android.view.View.VISIBLE
                        btnPlayImageView.visibility = android.view.View.GONE
                    } else {
                        btnPauseImageView.visibility = android.view.View.GONE
                        btnPlayImageView.visibility = android.view.View.VISIBLE
                        seekBar.progress = 0
                    }

                }
            } catch (ex: Exception) {
                Crashlytics.logException(ex)
                ex.printStackTrace()
            }
        }
    }

    private fun updateUI() {
        try {
            duration = 0
            if (message.type == BASE_MESSAGE_TYPE.Q || message.type == BASE_MESSAGE_TYPE.AR) {
                val audioTypeObj: AudioType = message.question!!.audioList!![0]
                this.duration = audioTypeObj.duration
                when {
                    message.downloadStatus === DOWNLOAD_STATUS.DOWNLOADED -> {
                        mediaDownloaded()
                    }
                    message.downloadStatus === DOWNLOAD_STATUS.DOWNLOADING -> {
                        downloadStart(audioTypeObj.audio_url)
                        mediaDownloading()
                    }
                    else -> {
                        mediaNotDownloaded()
                    }
                }

            } else {
                if (message.downloadStatus === DOWNLOAD_STATUS.DOWNLOADED || message.downloadStatus === DOWNLOAD_STATUS.UPLOADED) {
                    mediaDownloaded()
                    duration = Utils.getDurationOfMedia(
                        activityRef.get()!!,
                        message.downloadedLocalPath!!
                    ).toInt()
                } else if (message.downloadStatus === DOWNLOAD_STATUS.DOWNLOADING) {
                    mediaDownloading()
                    downloadStart(message.url!!)
                } else if (message.downloadStatus === DOWNLOAD_STATUS.UPLOADING) {
                    mediaUploading()
                } else {
                    mediaNotDownloaded()
                }

            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        if (duration > 0) {
            txtCurrentDurationTV.text = PlayerUtil.toTimeSongString(duration)
            seekBar.max = duration
        }
    }

    private fun mediaNotDownloaded() {
        downloadContainer.visibility = android.view.View.VISIBLE
        seekBarPlaceHolder.visibility = android.view.View.VISIBLE
        startDownloadImageView.visibility = android.view.View.VISIBLE
        seekBar.visibility = android.view.View.INVISIBLE
        progressBar.visibility = android.view.View.INVISIBLE
        cancelDownloadImageView.visibility = android.view.View.INVISIBLE
        btnPlayImageView.visibility = android.view.View.INVISIBLE
        btnPauseImageView.visibility = android.view.View.INVISIBLE

    }

    private fun mediaDownloading() {
        downloadContainer.visibility = android.view.View.VISIBLE
        progressBar.visibility = android.view.View.VISIBLE
        cancelDownloadImageView.visibility = android.view.View.VISIBLE
        seekBarPlaceHolder.visibility = android.view.View.VISIBLE
        startDownloadImageView.visibility = android.view.View.INVISIBLE
    }

    private fun mediaDownloaded() {
        btnPlayImageView.visibility = android.view.View.VISIBLE
        // seekBar.visibility = android.view.View.VISIBLE
        btnPauseImageView.visibility = android.view.View.INVISIBLE
        seekBarPlaceHolder.visibility = android.view.View.VISIBLE
        downloadContainer.visibility = android.view.View.INVISIBLE
        seekBarThumb.visibility = android.view.View.VISIBLE
    }

    fun downloadStart(url: String) {
        DownloadUtils.downloadFile(
            url,
            AppDirectory.getAudioReceivedFile(url).absolutePath,
            message.chatId,
            message,
            downloadListener
        )
    }

    private fun mediaUploading() {
        seekBar.visibility = android.view.View.INVISIBLE
        seekBarPlaceHolder.visibility = android.view.View.VISIBLE
        downloadContainer.visibility = android.view.View.VISIBLE
        progressBar.visibility = android.view.View.VISIBLE
        cancelDownloadImageView.visibility = android.view.View.VISIBLE
        startDownloadImageView.visibility = android.view.View.INVISIBLE
        btnPlayImageView.visibility = android.view.View.INVISIBLE
        btnPauseImageView.visibility = android.view.View.INVISIBLE
    }


    @Click(R.id.btnPlay)
    fun play() {
        if (PermissionUtils.isStoragePermissionEnable(activityRef.get()!!).not()) {
            PermissionUtils.storageReadAndWritePermission(activityRef.get()!!,
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                playAudioInPlayer()
                                return

                            }
                            if (report.isAnyPermissionPermanentlyDenied) {
                                PermissionUtils.permissionPermanentlyDeniedDialog(
                                    activityRef.get()!!
                                )
                                return
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        token?.continuePermissionRequest()
                    }
                })
            return
        }
        playAudioInPlayer()
    }

    @Click(R.id.btnPause)
    fun pause() {
        RxBus2.publish(AudioPlayEventBus(PlaybackInfoListener.State.PAUSED, message, null))
        btnPauseImageView.visibility = android.view.View.GONE
        btnPlayImageView.visibility = android.view.View.VISIBLE
        txtCurrentDurationTV.text = PlayerUtil.toTimeSongString(duration)

    }

    @Click(R.id.cancel_download_iv)
    fun cancelDownload() {

    }


    @Click(R.id.start_download_iv)
    fun startAudioDownload() {
        RxBus2.publish(
            DownloadMediaEventBus(
                audioPlayerViewHolder,
                message
            )
        )
    }


    fun playAudioInPlayer() {
        val ref = activityRef.get() as ConversationActivity

        if (AppObjectController.currentPlayingAudioObject != null && AppObjectController.currentPlayingAudioObject?.chatId == message.chatId && ref.isAudioPlaying()) {
            btnPauseImageView.visibility = android.view.View.INVISIBLE
            btnPlayImageView.visibility = android.view.View.VISIBLE

        } else {
            btnPauseImageView.visibility = android.view.View.VISIBLE
            btnPlayImageView.visibility = android.view.View.INVISIBLE
            seekBarThumb.startAnimation(animBlink)
        }

        if (message.url.isNullOrEmpty().not()) {
            val audioObject = AudioType(
                message.url!!,
                EMPTY,
                Utils.getDurationOfMedia(
                    activityRef.get()!!,
                    message.downloadedLocalPath!!
                ).toInt(),
                0,
                true
            )
            audioObject.downloadedLocalPath = message.url
            RxBus2.publish(
                AudioPlayEventBus(
                    PlaybackInfoListener.State.PLAYING,
                    message,
                    audioObject
                )
            )
        } else {
            RxBus2.publish(
                AudioPlayEventBus(
                    PlaybackInfoListener.State.PLAYING,
                    message,
                    message.question!!.audioList?.get(0)!!
                )
            )
        }
    }

    @Recycle
    fun onRecycled() {
        compositeDisposable.clear()
    }
}