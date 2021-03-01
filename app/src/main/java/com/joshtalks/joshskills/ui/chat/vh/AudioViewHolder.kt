package com.joshtalks.joshskills.ui.chat.vh

import android.content.res.ColorStateList
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.PlayerUtil
import com.joshtalks.joshskills.core.playback.PlaybackInfoListener
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.eventbus.AudioPlayEventBus
import com.joshtalks.joshskills.repository.local.eventbus.DownloadMediaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.InternalSeekBarProgressEventBus
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.disposables.CompositeDisposable
import java.lang.ref.WeakReference
import java.util.*

class AudioViewHolder(
    view: View,
    private val activityRef: WeakReference<FragmentActivity>,
    userId: String
) : BaseViewHolder(view, userId),
    SeekBar.OnSeekBarChangeListener {
    private val rootSubView: FrameLayout = view.findViewById(R.id.root_sub_view)
    private val profileImage: AppCompatImageView = view.findViewById(R.id.profile_image)
    private val downloadContainer: FrameLayout = view.findViewById(R.id.download_container)
    private val compositeDisposable = CompositeDisposable()
    private var appAnalytics: AppAnalytics? = null
    private var message: ChatModel? = null
    private var audioViewSent: View = view.findViewById(R.id.audio_view_sent)
    private var audioViewReceived: View = view.findViewById(R.id.audio_view_received)
    private val cancelDownloadImageView: AppCompatImageView =
        view.findViewById(R.id.cancel_download_iv)
    private val startDownloadImageView: AppCompatImageView =
        view.findViewById(R.id.start_download_iv)
    private var progressBar: ProgressBar = view.findViewById(R.id.progress_bar)
    private val btnPlayImageView: AppCompatImageView = view.findViewById(R.id.btnPlay)
    private val btnPauseImageView: AppCompatImageView = view.findViewById(R.id.btnPause)
    private val seekBar: SeekBar = view.findViewById(R.id.seekBar)
    private val textMessageTime: AppCompatTextView = view.findViewById(R.id.message_time)
    private val txtCurrentDurationTV: AppCompatTextView = view.findViewById(R.id.txtCurrentDuration)
    private var animBlink: Animation? = null
    private var duration: Int = 0
    private var userSelectedPosition = 0


    init {
        startDownloadImageView.also { it ->
            it.setOnClickListener {
                message?.let {
                    RxBus2.publish(
                        DownloadMediaEventBus(
                            DOWNLOAD_STATUS.REQUEST_DOWNLOADING,
                            it.chatId,
                            url = getUrlForDownload(it),
                            type = BASE_MESSAGE_TYPE.AU,
                            chatModel = it
                        )
                    )
                }
            }
        }
        btnPlayImageView.also {
            it.setOnClickListener {
                play()
            }
        }

        btnPauseImageView.also {
            it.setOnClickListener {
                message?.let {
                    RxBus2.publish(AudioPlayEventBus(PlaybackInfoListener.State.PAUSED, it, null))
                }
                btnPauseImageView.visibility = View.GONE
                btnPlayImageView.visibility = VISIBLE
                txtCurrentDurationTV.text = PlayerUtil.toTimeSongString(duration)
                seekBar.isEnabled = false
            }
        }
    }


    override fun bind(message: ChatModel, previousChat: ChatModel?) {
        this.message = message
        setDefaultState()
        if (null != message.sender) {
            setViewHolderBG(previousChat?.sender, message.sender!!, rootSubView)
        }
        animBlink = AnimationUtils.loadAnimation(getAppContext(), R.anim.blink)
        message.sender?.let {
            if (it.id.equals(userId, ignoreCase = true)) {
                audioViewSent.visibility = VISIBLE
            } else {
                audioViewReceived.visibility = VISIBLE
            }
        }
        seekBar.setOnSeekBarChangeListener(this)
        textMessageTime.text =
            Utils.getMessageTimeInHours(message.created).toUpperCase(Locale.getDefault())
        addDrawableOnTime(message, textMessageTime)
        updateUI()
        audioPlayingStatus()

    }

    private fun setDefaultState() {
        profileImage.setImageResource(R.drawable.ic_user_rec_placeholder)
        seekBar.progress = 0
        txtCurrentDurationTV.text = EMPTY
        audioViewSent.visibility = View.GONE
        audioViewReceived.visibility = View.GONE
        downloadContainer.visibility = VISIBLE
        progressBar.visibility = View.INVISIBLE
        cancelDownloadImageView.visibility = View.INVISIBLE
        startDownloadImageView.visibility = View.INVISIBLE
        btnPlayImageView.visibility = View.INVISIBLE
        btnPauseImageView.visibility = View.INVISIBLE
        rootSubView.findViewById<ViewGroup>(R.id.tag_view).visibility = View.GONE
        seekBar.isEnabled = false
        seekBar.progress = message?.playProgress ?: 0
    }

    private fun updateUI() {
        duration = 0
        message?.let {
            if (it.url == null) {
                audioFromServer(it.question!!)
            } else {
                audioFromUser(it)
            }
        }
    }

    private fun audioFromUser(message: ChatModel) {
        if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED || message.downloadStatus == DOWNLOAD_STATUS.UPLOADED) {
            mediaDownloaded()
            duration = message.duration
        } else if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
            mediaDownloading()
            //downloadStart(message.url!!)
        } else if (message.downloadStatus == DOWNLOAD_STATUS.UPLOADING) {
            mediaUploading()
        } else {
            mediaNotDownloaded()
        }
        setDurationView()
    }

    private fun audioFromServer(question: Question) {
        question.audioList?.getOrNull(0)?.let { audio ->
            this.duration = audio.duration
            setDurationView()
            setAudioStatus(message?.downloadStatus!!, audio.audio_url)
        }
    }

    private fun setAudioStatus(downloadStatus: DOWNLOAD_STATUS, url: String) {
        when (downloadStatus) {
            DOWNLOAD_STATUS.DOWNLOADED -> {
                mediaDownloaded()
            }
            DOWNLOAD_STATUS.DOWNLOADING -> {
                //   downloadStart(url)
                mediaDownloading()
            }
            else -> {
                mediaNotDownloaded()
            }
        }
    }

    private fun setDurationView() {
        if (duration > 0) {
            txtCurrentDurationTV.text = PlayerUtil.toTimeSongString(duration)
            seekBar.max = duration
        }
    }

    private fun audioPlayingStatus() {
        AppObjectController.currentPlayingAudioObject?.run {
            try {
                if (this.chatId == message?.chatId) {
                    val ref = activityRef.get() as ConversationActivity
                    if (ref.isAudioPlaying()) {
                        playAudioUIUpdate()
                    } else {
                        pauseAudioUIUpdate()
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }


    private fun mediaNotDownloaded() {
        appAnalytics?.addParam(AnalyticsEvent.AUDIO_VIEW_STATUS.NAME, "Already downloaded")
        btnPlayImageView.visibility = View.INVISIBLE
        btnPauseImageView.visibility = View.INVISIBLE
        downloadContainer.visibility = VISIBLE
        startDownloadImageView.visibility = VISIBLE
        progressBar.visibility = View.INVISIBLE
        cancelDownloadImageView.visibility = View.INVISIBLE
    }

    private fun mediaDownloading() {
        appAnalytics?.addParam(AnalyticsEvent.AUDIO_VIEW_STATUS.NAME, "Not downloaded")
        downloadContainer.visibility = VISIBLE
        progressBar.visibility = VISIBLE
        cancelDownloadImageView.visibility = VISIBLE
        startDownloadImageView.visibility = View.INVISIBLE
    }

    private fun mediaDownloaded() {
        btnPauseImageView.visibility = View.INVISIBLE
        downloadContainer.visibility = View.GONE
        seekBar.thumb = ContextCompat.getDrawable(getAppContext(), R.drawable.seek_thumb)
        seekBar.thumbTintList =
            ColorStateList.valueOf(ContextCompat.getColor(getAppContext(), R.color.colorPrimary))
        seekBar.visibility = VISIBLE
        btnPlayImageView.visibility = VISIBLE
    }

    private fun mediaUploading() {
        seekBar.visibility = VISIBLE
        downloadContainer.visibility = VISIBLE
        progressBar.visibility = VISIBLE
        cancelDownloadImageView.visibility = VISIBLE
        startDownloadImageView.visibility = View.INVISIBLE
        btnPlayImageView.visibility = View.INVISIBLE
        btnPauseImageView.visibility = View.INVISIBLE
    }

    fun play() {
        if (PermissionUtils.isStoragePermissionEnabled(getAppContext())) {
            playAudioInPlayer()
        } else {
            PermissionUtils.storageReadAndWritePermission(getAppContext(),
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                playAudioInPlayer()
                                return

                            }
                            if (report.isAnyPermissionPermanentlyDenied) {
                                PermissionUtils.permissionPermanentlyDeniedDialog(activityRef.get()!!)
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
    }


    fun pause() {
        message?.let {
            RxBus2.publish(AudioPlayEventBus(PlaybackInfoListener.State.PAUSED, it, null))
            btnPauseImageView.visibility = View.GONE
            btnPlayImageView.visibility = VISIBLE
            txtCurrentDurationTV.text = PlayerUtil.toTimeSongString(duration)
            seekBar.isEnabled = false
        }
    }

    fun playAudioInPlayer() {
        try {
            message?.let {
                if (it.url.isNullOrEmpty().not()) {
                    val audioObject = AudioType(
                        it.url!!, EMPTY,
                        Utils.getDurationOfMedia(getAppContext(), it.downloadedLocalPath!!)
                            ?.toInt() ?: 0, 0, true
                    )
                    audioObject.downloadedLocalPath = it.url
                    //playAudioUIUpdate()
                    RxBus2.publish(
                        AudioPlayEventBus(
                            PlaybackInfoListener.State.PLAYING,
                            it,
                            audioObject
                        )
                    )
                } else {
                    //playAudioUIUpdate()
                    RxBus2.publish(
                        AudioPlayEventBus(
                            PlaybackInfoListener.State.PLAYING,
                            it,
                            it.question!!.audioList?.get(0)
                        )
                    )
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun playAudioUIUpdate() {
        btnPauseImageView.visibility = VISIBLE
        btnPlayImageView.visibility = View.GONE
        seekBar.isEnabled = true
    }

    private fun pauseAudioUIUpdate() {
        seekBar.isEnabled = false
        btnPauseImageView.visibility = View.GONE
        btnPlayImageView.visibility = VISIBLE
    }

    override fun unBind() {
        compositeDisposable.clear()
    }


    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            userSelectedPosition = progress
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        RxBus2.publish(
            InternalSeekBarProgressEventBus(
                userSelectedPosition
            )
        )
    }
}
