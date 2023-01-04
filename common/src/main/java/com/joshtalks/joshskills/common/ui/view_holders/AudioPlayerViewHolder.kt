package com.joshtalks.joshskills.common.ui.view_holders


import android.content.res.ColorStateList
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.EMPTY
import com.joshtalks.joshskills.common.core.PermissionUtils
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.common.core.analytics.AppAnalytics
import com.joshtalks.joshskills.common.core.custom_ui.PlayerUtil
import com.joshtalks.joshskills.common.core.io.AppDirectory
import com.joshtalks.joshskills.common.core.playback.PlaybackInfoListener
import com.joshtalks.joshskills.common.core.service.DownloadUtils
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.entity.AudioType
import com.joshtalks.joshskills.common.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel
import com.joshtalks.joshskills.common.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.common.repository.local.eventbus.AudioPlayEventBus
import com.joshtalks.joshskills.common.repository.local.eventbus.DownloadCompletedEventBus
import com.joshtalks.joshskills.common.repository.local.eventbus.InternalSeekBarProgressEventBus
import com.joshtalks.joshskills.common.ui.chat.ConversationActivity
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
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.roundToInt


class AudioPlayerViewHolder(
    activityRef: WeakReference<FragmentActivity>,
    message: ChatModel,
    previousMessage: ChatModel?
) :
    BaseChatViewHolder(activityRef, message, previousMessage) {


    lateinit var audioView: RelativeLayout


    lateinit var rootView: FrameLayout


    lateinit var rootSubView: FrameLayout


    lateinit var messageView: RelativeLayout


    lateinit var audioViewSent: android.view.View


    lateinit var audioViewReceived: android.view.View


    lateinit var profileImage: AppCompatImageView


    lateinit var downloadContainer: FrameLayout


    lateinit var startDownloadImageView: AppCompatImageView


    lateinit var progressBar: ProgressBar


    lateinit var cancelDownloadImageView: AppCompatImageView


    lateinit var btnPlayImageView: AppCompatImageView


    lateinit var btnPauseImageView: AppCompatImageView


    lateinit var seekBar: SeekBar


    lateinit var txtCurrentDurationTV: AppCompatTextView


    lateinit var messageTimeTV: AppCompatTextView

    private var animBlink: Animation? = null
    lateinit var audioPlayerViewHolder: AudioPlayerViewHolder
    private lateinit var appAnalytics: AppAnalytics
    private var duration: Int = 0
    private val compositeDisposable = CompositeDisposable()
    private var eta = System.currentTimeMillis()


    private var downloadListener = object : FetchListener {
        override fun onAdded(download: Download) {

        }

        override fun onCancelled(download: Download) {
            appAnalytics.addParam(AnalyticsEvent.AUDIO_DOWNLOAD_STATUS.NAME, "Cancelled").push()
        }

        override fun onCompleted(download: Download) {
            eta = System.currentTimeMillis() - eta
            if (eta >= 10000000)
                eta = 500
            appAnalytics.addParam(AnalyticsEvent.TIME_TAKEN_DOWNLOAD.NAME, eta)
            appAnalytics.addParam(AnalyticsEvent.AUDIO_DOWNLOAD_STATUS.NAME, "Completed")
                .push()
            DownloadUtils.removeCallbackListener(download.tag)
            DownloadUtils.updateDownloadStatus(download.file, download.extras) {
                com.joshtalks.joshskills.common.messaging.RxBus2.publish(DownloadCompletedEventBus(audioPlayerViewHolder, message))
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
            appAnalytics.addParam(AnalyticsEvent.AUDIO_DOWNLOAD_STATUS.NAME, "Failed error")

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
        //seekBar.thumb=null
        txtCurrentDurationTV.text = EMPTY
        audioViewSent.visibility = android.view.View.GONE
        audioViewReceived.visibility = android.view.View.GONE
        downloadContainer.visibility = android.view.View.VISIBLE
        progressBar.visibility = android.view.View.INVISIBLE
        cancelDownloadImageView.visibility = android.view.View.INVISIBLE
        startDownloadImageView.visibility = android.view.View.INVISIBLE
        btnPlayImageView.visibility = android.view.View.INVISIBLE
        btnPauseImageView.visibility = android.view.View.INVISIBLE
        rootSubView.findViewById<ViewGroup>(R.id.tag_view).visibility = android.view.View.GONE

        seekBar.isEnabled = false
        seekBar.progress = message.playProgress
        appAnalytics = AppAnalytics.create(AnalyticsEvent.AUDIO_VH.NAME)
            .addBasicParam()
            .addUserDetails()

        message.parentQuestionObject?.run {
            val relativeParams = messageView.layoutParams as ViewGroup.MarginLayoutParams
            relativeParams.setMargins(
                0,
                getAppContext().resources.getDimension(R.dimen.tag_height).roundToInt(),
                0,
                0
            )
            messageView.layoutParams = relativeParams
            addLinkToTagMessage(rootView, this, message.sender)
        }
        if (message.chatId.isNotEmpty() && sId == message.chatId) {
            highlightedViewForSomeTime(rootView)
        }


        message.sender?.let {
            if (it.id.equals(getUserId(), ignoreCase = true)) {
                audioViewSent.visibility = android.view.View.VISIBLE
            } else {
                audioViewReceived.visibility = android.view.View.VISIBLE
            }
            setViewHolderBG(previousMessage?.sender, it, rootView, rootSubView, null)
        }

        seekBar.setOnSeekBarChangeListener(
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
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    com.joshtalks.joshskills.common.messaging.RxBus2.publish(
                        InternalSeekBarProgressEventBus(
                            userSelectedPosition
                        )
                    )
                }
            })

        updateUI()
        audioPlayingStatus()
        updateTime(messageTimeTV)
        messageTimeTV.text =
            Utils.getMessageTimeInHours(message.created).toUpperCase(Locale.getDefault())

    }

    private fun audioPlayingStatus() {
        AppObjectController.currentPlayingAudioObject?.run {
            try {

                if (this.chatId == message.chatId) {
                    val ref = activityRef.get() as ConversationActivity
                    if (ref.isAudioPlaying()) {
                        btnPauseImageView.visibility = android.view.View.VISIBLE
                        btnPlayImageView.visibility = android.view.View.GONE
                        seekBar.isEnabled = true
                    } else {
                        seekBar.isEnabled = false
                        btnPauseImageView.visibility = android.view.View.GONE
                        btnPlayImageView.visibility = android.view.View.VISIBLE
                    }

                }
            } catch (ex: Exception) {
                try {
                    FirebaseCrashlytics.getInstance().recordException(ex)
                }catch (ex:Exception){
                    ex.printStackTrace()
                }
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
                appAnalytics.addParam(AnalyticsEvent.AUDIO_DURATION.NAME, duration)
                    .addParam(AnalyticsEvent.AUDIO_ID.NAME, audioTypeObj.id)
                    .addParam("ChatId", message.chatId)

                when (message.downloadStatus) {
                    DOWNLOAD_STATUS.DOWNLOADED -> {
                        mediaDownloaded()
                    }
                    DOWNLOAD_STATUS.DOWNLOADING -> {
                        downloadStart(audioTypeObj.audio_url)
                        mediaDownloading()
                    }
                    else -> {
                        mediaNotDownloaded()
                    }
                }

            } else {
                if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED || message.downloadStatus == DOWNLOAD_STATUS.UPLOADED) {
                    mediaDownloaded()
                    duration = Utils.getDurationOfMedia(
                        activityRef.get()!!,
                        message.downloadedLocalPath!!
                    )?.toInt() ?: 0
                } else if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
                    mediaDownloading()
                    downloadStart(message.url!!)
                } else if (message.downloadStatus == DOWNLOAD_STATUS.UPLOADING) {
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
        appAnalytics.addParam(AnalyticsEvent.AUDIO_VIEW_STATUS.NAME, "Already downloaded")
        btnPlayImageView.visibility = android.view.View.INVISIBLE
        btnPauseImageView.visibility = android.view.View.INVISIBLE
        downloadContainer.visibility = android.view.View.VISIBLE
        startDownloadImageView.visibility = android.view.View.VISIBLE
        //seekBar.visibility = android.view.View.INVISIBLE
        progressBar.visibility = android.view.View.INVISIBLE
        cancelDownloadImageView.visibility = android.view.View.INVISIBLE
    }

    private fun mediaDownloading() {
        appAnalytics.addParam(AnalyticsEvent.AUDIO_VIEW_STATUS.NAME, "Not downloaded")
        downloadContainer.visibility = android.view.View.VISIBLE
        progressBar.visibility = android.view.View.VISIBLE
        cancelDownloadImageView.visibility = android.view.View.VISIBLE
        startDownloadImageView.visibility = android.view.View.INVISIBLE
    }

    private fun mediaDownloaded() {
        seekBar.thumb = ContextCompat.getDrawable(getAppContext(), R.drawable.seek_thumb)
        seekBar.thumbTintList =
            ColorStateList.valueOf(ContextCompat.getColor(getAppContext(), R.color.primary_500))
        btnPlayImageView.visibility = android.view.View.VISIBLE
        seekBar.visibility = android.view.View.VISIBLE
        btnPauseImageView.visibility = android.view.View.INVISIBLE
        downloadContainer.visibility = android.view.View.INVISIBLE
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
        downloadContainer.visibility = android.view.View.VISIBLE
        progressBar.visibility = android.view.View.VISIBLE
        cancelDownloadImageView.visibility = android.view.View.VISIBLE
        startDownloadImageView.visibility = android.view.View.INVISIBLE
        btnPlayImageView.visibility = android.view.View.INVISIBLE
        btnPauseImageView.visibility = android.view.View.INVISIBLE
    }

    
    fun play() {
        if (PermissionUtils.isStoragePermissionEnabled(activityRef.get()!!).not()) {
            PermissionUtils.storageReadAndWritePermission(
                activityRef.get()!!,
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

    
    fun pause() {
        com.joshtalks.joshskills.common.messaging.RxBus2.publish(AudioPlayEventBus(PlaybackInfoListener.State.PAUSED, message, null))
        btnPauseImageView.visibility = android.view.View.GONE
        btnPlayImageView.visibility = android.view.View.VISIBLE
        txtCurrentDurationTV.text = PlayerUtil.toTimeSongString(duration)
        seekBar.isEnabled = false

    }

    
    fun cancelDownload() {

    }


    
    fun startAudioDownload() {
        /*  RxBus2.publish(
              DownloadMediaEventBus(
                  audioPlayerViewHolder,
                  message
              )
          )*/
    }


    fun playAudioInPlayer() {
        try {
            if (message.url.isNullOrEmpty().not()) {
                val audioObject = AudioType(
                    message.url!!,
                    EMPTY,
                    Utils.getDurationOfMedia(
                        activityRef.get()!!,
                        message.downloadedLocalPath!!
                    )?.toInt() ?: 0,
                    0,
                    true
                )
                audioObject.downloadedLocalPath = message.url
                com.joshtalks.joshskills.common.messaging.RxBus2.publish(
                    AudioPlayEventBus(
                        PlaybackInfoListener.State.PLAYING,
                        message,
                        audioObject
                    )
                )
            } else {
                com.joshtalks.joshskills.common.messaging.RxBus2.publish(
                    AudioPlayEventBus(
                        PlaybackInfoListener.State.PLAYING,
                        message,
                        message.question!!.audioList?.get(0)!!
                    )
                )
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun getRoot(): FrameLayout {
        return rootView
    }

    @Recycle
    fun onRecycled() {
        compositeDisposable.clear()
    }

}