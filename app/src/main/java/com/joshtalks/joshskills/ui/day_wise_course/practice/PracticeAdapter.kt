package com.joshtalks.joshskills.ui.day_wise_course.practice

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.SystemClock
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.databinding.PracticeItemLayoutBinding
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.ui.pdfviewer.PdfViewerActivity
import com.joshtalks.joshskills.ui.practise.PracticeViewModel
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.util.ExoAudioPlayer
import com.muddzdev.styleabletoast.StyleableToast
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable
import java.util.concurrent.TimeUnit
import kotlin.random.Random.Default.nextInt

class PracticeAdapter(
    val context: Context,
    val practiceViewModel: PracticeViewModel,
    val itemList: ArrayList<ChatModel>,
    val clickListener: PracticeClickListeners
) :
    RecyclerView.Adapter<PracticeAdapter.PracticeViewHolder>() {

    var audioManager = ExoAudioPlayer.getInstance()
    var currentChatModel: ChatModel? = null
    var currentPlayingPosition: Int = 0
    private var isFirstTime: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PracticeViewHolder {
        val binding = PracticeItemLayoutBinding.inflate(LayoutInflater.from(context), parent, false)
        return PracticeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PracticeViewHolder, position: Int) {
        holder.bind(itemList[position], position)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class PracticeViewHolder(val binding: PracticeItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root), AudioPlayerEventListener,
        ExoAudioPlayer.ProgressUpdateListener {
        private var startTime: Long = 0L
        var filePath: String? = null
        var appAnalytics: AppAnalytics? = null
        var chatModel: ChatModel? = null

        fun bind(chatModel: ChatModel, position: Int) {
            this.chatModel = chatModel
            if (isFirstTime && chatModel.question?.status != QUESTION_STATUS.AT) {
                isFirstTime = false
                binding.practiceContentLl.visibility = VISIBLE
                binding.expandIv.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_remove
                    )
                )
                if (position > 0)
                    clickListener.focusChild(position - 1)
            } else {
                binding.practiceContentLl.visibility = GONE
                binding.expandIv.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_add
                    )
                )
            }
            appAnalytics = AppAnalytics.create(AnalyticsEvent.PRACTICE_SCREEN.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam("chatId", chatModel.chatId)

            setPracticeInfoView(chatModel)

            binding.titleView.setOnClickListener {
                if (binding.practiceContentLl.visibility == GONE) {
                    binding.practiceContentLl.visibility = VISIBLE
                    binding.expandIv.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.ic_remove
                        )
                    )
                } else {
                    binding.practiceContentLl.visibility = GONE
                    binding.expandIv.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.ic_add
                        )
                    )
                }
            }
            binding.btnPlayInfo.setOnClickListener {
                appAnalytics?.addParam(AnalyticsEvent.PRACTICE_EXTRA.NAME, "Audio Played")
                playPracticeAudio(chatModel, layoutPosition)
            }

            binding.submitBtnPlayInfo.setOnClickListener {
                appAnalytics?.addParam(
                    AnalyticsEvent.PRACTICE_EXTRA.NAME,
                    "Already Submitted audio Played"
                )
                playSubmitPracticeAudio(chatModel, layoutPosition)
//                filePath = chatModel.downloadedLocalPath
                val state =
                    if (chatModel.isPlaying) {
                        currentChatModel?.isPlaying = true
                        MaterialPlayPauseDrawable.State.Pause
                    } else {
                        currentChatModel?.isPlaying = false
                        MaterialPlayPauseDrawable.State.Play
                    }
                binding.submitBtnPlayInfo.state = state
            }

            binding.submitPractiseSeekbar.setOnSeekBarChangeListener(
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
                        if (currentPlayingPosition == layoutPosition)
                            audioManager?.seekTo(userSelectedPosition.toLong())
                    }
                })


            binding.ivCancel.setOnClickListener {
                chatModel.filePath = null
                removeAudioPractise()
                removeAudioPractice()
            }

            binding.submitAnswerBtn.setOnClickListener {
                isFirstTime = true
                if (chatModel.filePath == null) {
                    showToast(context.getString(R.string.submit_practise_msz))
                    return@setOnClickListener
                }

                if (clickListener.submitPractice(chatModel)) {
                    if (isAudioPlaying()) {
                        binding.submitPractiseSeekbar.progress = 0
                        binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
                        chatModel.isPlaying=false
                        audioManager?.resumeOrPause()
                    }
                    appAnalytics?.addParam(AnalyticsEvent.PRACTICE_SOLVED.NAME, true)
                    appAnalytics?.addParam(AnalyticsEvent.PRACTICE_STATUS.NAME, "Submitted")
                    appAnalytics?.addParam(
                        AnalyticsEvent.PRACTICE_TYPE_SUBMITTED.NAME,
                        "$it Practice Submitted"
                    )
                    appAnalytics?.addParam(
                        AnalyticsEvent.PRACTICE_SUBMITTED.NAME,
                        "Submit Practice $"
                    )
                }

            }
        }

        //===============================
        override fun onPlayerPause() {
        }

        override fun onPlayerResume() {
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
            audioManager?.onPause()
            audioManager?.setProgressUpdateListener(null)
            audioManager?.seekTo(0)
            binding.progressBarImageView.progress = 0
            binding.practiseSeekbar.progress = 0
            binding.submitPractiseSeekbar.progress = 0
            binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
            currentChatModel?.isPlaying = false
        }

        override fun onProgressUpdate(progress: Long) {
            currentChatModel?.playProgress = progress.toInt()
            if (currentPlayingPosition != -1) {
                binding.progressBarImageView.progress = progress.toInt()
                binding.practiseSeekbar.progress = progress.toInt()
                binding.submitPractiseSeekbar.progress = progress.toInt()
//                notifyItemChanged(layoutPosition)
            }
        }

        override fun onDurationUpdate(duration: Long?) {
            //duration?.toInt()?.let { binding.submitPractiseSeekbar.max = it }
        }

        private fun checkIsPlayer(): Boolean {
            return audioManager != null
        }

        private fun isAudioPlaying(): Boolean {
            return this.checkIsPlayer() && audioManager!!.isPlaying()
        }

        private fun onPlayAudio(chatModel: ChatModel, audioObject: AudioType, position: Int) {

            currentPlayingPosition = position

            currentChatModel = chatModel
            val audioList = java.util.ArrayList<AudioType>()
            audioList.add(audioObject)
            audioManager = ExoAudioPlayer.getInstance()
            audioManager?.playerListener = this
            audioManager?.play(audioObject.audio_url)
            audioManager?.setProgressUpdateListener(this)

            chatModel.isPlaying = chatModel.isPlaying.not()
        }

        fun playPracticeAudio(chatModel: ChatModel, position: Int) {
            if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) <= 0) {
                StyleableToast.Builder(AppObjectController.joshApplication).gravity(Gravity.BOTTOM)
                    .text(context.getString(R.string.volume_up_message)).cornerRadius(16)
                    .length(Toast.LENGTH_LONG)
                    .solidBackground().show()
            }

            if (currentChatModel == null) {
                chatModel.question?.audioList?.getOrNull(0)
                    ?.let {
                        onPlayAudio(chatModel, it, position)
                    }
            } else {
                if (currentChatModel == chatModel) {
                    if (checkIsPlayer()) {
                        audioManager?.setProgressUpdateListener(this)
                        audioManager?.resumeOrPause()
                    } else {
                        onPlayAudio(
                            chatModel,
                            chatModel.question?.audioList?.getOrNull(0)!!,
                            position
                        )
                    }
                } else {
                    onPlayAudio(chatModel, chatModel.question?.audioList?.getOrNull(0)!!, position)
                }
            }
        }

        fun playSubmitPracticeAudio(chatModel: ChatModel, position: Int) {
            try {
                val audioType = AudioType()
                audioType.audio_url = filePath!!
                audioType.downloadedLocalPath = filePath!!
                audioType.duration =
                    Utils.getDurationOfMedia(context, filePath!!)?.toInt() ?: 0
                audioType.id = nextInt().toString()

                binding.progressBarImageView.max = audioType.duration
                binding.practiseSeekbar.max = audioType.duration
                binding.submitPractiseSeekbar.max = audioType.duration
                if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) <= 0) {
                    StyleableToast.Builder(AppObjectController.joshApplication)
                        .gravity(Gravity.BOTTOM)
                        .text(context.getString(R.string.volume_up_message)).cornerRadius(16)
                        .length(Toast.LENGTH_LONG)
                        .solidBackground().show()
                }

                if (currentChatModel == null) {
                    onPlayAudio(chatModel, audioType, position)
                } else {
                    if (audioManager?.currentPlayingUrl?.isNotEmpty() == true && audioManager?.currentPlayingUrl == audioType.audio_url) {
                        if (checkIsPlayer()) {
                            currentPlayingPosition = position
                            audioManager?.setProgressUpdateListener(this)
                            chatModel.isPlaying = chatModel.isPlaying.not()
                            audioManager?.resumeOrPause()
//                            notifyItemChanged(layoutPosition)
                        } else {
                            onPlayAudio(chatModel, audioType, position)
                        }
                    } else {
                        onPlayAudio(chatModel, audioType, position)

                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }

        fun removeAudioPractise() {
            if (isAudioPlaying()) {
                audioManager?.resumeOrPause()
            }
        }

        fun pauseAudio() {
            chatModel?.let {
                if (chatModel!!.isPlaying) {
                    playSubmitPracticeAudio(it, layoutPosition)
                    currentChatModel?.isPlaying = false
                    binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
                }
            }
        }

        //============================================================================
        private fun setPracticeInfoView(chatModel: ChatModel) {
            chatModel.question?.run {
                binding.practiceTitleTv.text =
                    context.getString(
                        R.string.word_tag,
                        layoutPosition + 1,
                        itemList.size,
                        this.practiceWord
                    )
                if (this.status != QUESTION_STATUS.NA) {
                    binding.practiceTitleTv.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_check,
                        0,
                        0,
                        0
                    )
                } else {
                    binding.practiceTitleTv.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_check_grey,
                        0,
                        0,
                        0
                    )
                }
                when (this.material_type) {
                    BASE_MESSAGE_TYPE.AU -> {
                        binding.audioViewContainer.visibility = VISIBLE
                        this.audioList?.getOrNull(0)?.audio_url?.let {
                            binding.btnPlayInfo.tag = it
                            binding.practiseSeekbar.max = this.audioList?.getOrNull(0)?.duration!!
                            if (binding.practiseSeekbar.max == 0) {
                                binding.practiseSeekbar.max = 2_00_000
                            }
                        }
                        initializePractiseSeekBar(chatModel)

                    }
                    BASE_MESSAGE_TYPE.IM -> {
                        binding.imageView.visibility = VISIBLE
                        this.imageList?.getOrNull(0)?.imageUrl?.let { path ->
                            setImageInImageView(path, binding.imageView)
                            binding.imageView.setOnClickListener {
//                                ImageShowFragment.newInstance(path, "", "")
//                                    .show(supportFragmentManager, "ImageShow")
                            }
                        }
                    }
                    BASE_MESSAGE_TYPE.VI -> {
                        binding.videoPlayer.visibility = VISIBLE
                        this.videoList?.getOrNull(0)?.video_url?.let {
                            binding.videoPlayer.setUrl(it)
                            binding.videoPlayer.fitToScreen()
                            binding.videoPlayer.setPlayListener {
                                val videoId = this.videoList?.getOrNull(0)?.id
                                val videoUrl = this.videoList?.getOrNull(0)?.video_url
                                VideoPlayerActivity.startVideoActivity(
                                    context,
                                    "",
                                    videoId,
                                    videoUrl
                                )
                            }
                            binding.videoPlayer.downloadStreamButNotPlay()
                        }
                    }
                    BASE_MESSAGE_TYPE.PD -> {
                        binding.imageView.visibility = VISIBLE
                        binding.imageView.setImageResource(R.drawable.ic_practise_pdf_ph)
                        this.pdfList?.getOrNull(0)?.let { pdfType ->
                            binding.imageView.setOnClickListener {
                                PdfViewerActivity.startPdfActivity(
                                    context,
                                    pdfType.id,
                                    EMPTY
                                )

                            }
                        }
                    }

                    BASE_MESSAGE_TYPE.TX -> {
                        this.qText?.let {
                            binding.infoTv.visibility = VISIBLE
                            binding.infoTv.text =
                                HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
                        }
                    }
                    else -> {

                    }
                }

                if ((this.material_type == BASE_MESSAGE_TYPE.TX).not() && this.qText.isNullOrEmpty()
                        .not()
                ) {
                    binding.infoTv2.text =
                        HtmlCompat.fromHtml(this.qText!!, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    binding.infoTv2.visibility = VISIBLE
                }

                if (this.status == QUESTION_STATUS.NA) {
                    binding.submitAnswerBtn.visibility = VISIBLE
                    setViewAccordingExpectedAnswer(chatModel)
                } else {
                    hidePracticeInputLayout()
                    binding.submitAnswerBtn.visibility = GONE
                    setViewUserSubmitAnswer(chatModel)
                }

            }
        }

        private fun setImageInImageView(url: String, imageView: ImageView) {
            binding.progressBarImageView.visibility = VISIBLE

            Glide.with(context)
                .load(url)
                .override(Target.SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(FitCenter())
                )
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false

                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.progressBarImageView.visibility = GONE

                        return false
                    }

                })

                .into(imageView)
        }

        private fun setViewAccordingExpectedAnswer(chatModel: ChatModel) {
            chatModel.question?.run {
                showPracticeInputLayout()
                this.expectedEngageType?.let {
                    binding.uploadPractiseView.visibility = VISIBLE

                    binding.practiseInputHeader.text = AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.READING_PRACTICE_TITLE)
                    binding.uploadPractiseView.setImageResource(R.drawable.recv_ic_mic_white)
                    audioRecordTouchListener(chatModel)
                    binding.audioPractiseHint.visibility = VISIBLE
                    binding.submitAudioViewContainer.visibility = GONE
                    binding.yourSubAnswerTv.visibility = GONE
                }
            }
        }

        private fun setViewUserSubmitAnswer(chatModel: ChatModel) {
            chatModel.question?.run {
                this.expectedEngageType?.let {
                    hidePracticeInputLayout()
                    showPracticeSubmitLayout()
                    binding.yourSubAnswerTv.visibility = VISIBLE
                    val params: ViewGroup.MarginLayoutParams =
                        binding.subPractiseSubmitLayout.layoutParams as ViewGroup.MarginLayoutParams
                    params.topMargin = Utils.dpToPx(20)
                    binding.subPractiseSubmitLayout.layoutParams = params
                    binding.yourSubAnswerTv.text = context.getString(R.string.your_submitted_answer)
                    if (practiceEngagement.isNullOrEmpty() && this.status == QUESTION_STATUS.IP) {
                        filePath = chatModel.filePath
                        binding.submitPractiseSeekbar.max =
                            Utils.getDurationOfMedia(context, filePath!!)
                                ?.toInt() ?: 0
                    } else {
                        val practiseEngagement = this.practiceEngagement?.getOrNull(0)
                        if (EXPECTED_ENGAGE_TYPE.AU == it) {
                            binding.submitAudioViewContainer.visibility = VISIBLE
                        }
                        if (PermissionUtils.isStoragePermissionEnabled(context) && AppDirectory.isFileExist(
                                practiseEngagement?.localPath
                            )
                        ) {
                            filePath = practiseEngagement?.localPath
                            binding.submitPractiseSeekbar.max =
                                Utils.getDurationOfMedia(context, filePath!!)
                                    ?.toInt() ?: 0
                        } else {
                            filePath = practiseEngagement?.answerUrl
                            if (practiseEngagement?.duration != null || practiseEngagement?.duration == 0) {
                                binding.submitPractiseSeekbar.max = practiseEngagement.duration!!
                            } else {
                                binding.submitPractiseSeekbar.max = 1_00_000
                            }
                        }
                    }

                    initializePractiseSeekBar(chatModel)
                    binding.ivCancel.visibility = GONE
                }
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun audioRecordTouchListener(chatModel: ChatModel) {
            binding.uploadPractiseView.setOnTouchListener { _, event ->
                if (PermissionUtils.isAudioAndStoragePermissionEnable(context).not()) {
                    clickListener.askRecordPermission()
                    return@setOnTouchListener true
                }
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        binding.videoPlayer.onPause()
                        binding.rootView.requestDisallowInterceptTouchEvent(true)
                        binding.counterContainer.visibility = VISIBLE
                        val scaleAnimation = AnimationUtils.loadAnimation(context, R.anim.scale)
                        binding.uploadPractiseView.startAnimation(scaleAnimation)
                        binding.counterTv.base = SystemClock.elapsedRealtime()
                        startTime = System.currentTimeMillis()
                        binding.counterTv.start()
                        startRecording(startTime)
                        clickListener.startRecording(chatModel, layoutPosition, startTime)
                        binding.audioPractiseHint.visibility = GONE

                        appAnalytics?.addParam(AnalyticsEvent.AUDIO_RECORD.NAME, "Audio Recording")
                    }
                    MotionEvent.ACTION_MOVE -> {
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        binding.rootView.requestDisallowInterceptTouchEvent(false)
                        binding.counterTv.stop()
                        val stopTime = System.currentTimeMillis()
                        stopRecording(chatModel, stopTime)
                        clickListener.stopRecording(chatModel, layoutPosition, stopTime)
                        binding.uploadPractiseView.clearAnimation()
                        binding.counterContainer.visibility = GONE
                        binding.audioPractiseHint.visibility = VISIBLE
//                        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                        val timeDifference =
                            TimeUnit.MILLISECONDS.toSeconds(stopTime) - TimeUnit.MILLISECONDS.toSeconds(
                                startTime
                            )
                        if (timeDifference > 1) {
                            audioAttachmentInit(chatModel)
                        }
                    }
                }

                true
            }
        }

        private fun startRecording(startTime: Long) {
            this.startTime = startTime
            practiceViewModel.startRecordAudio(null)
        }

        fun stopRecording(chatModel: ChatModel, stopTime: Long) {
            practiceViewModel.stopRecordingAudio(false)
            val timeDifference =
                TimeUnit.MILLISECONDS.toSeconds(stopTime) - TimeUnit.MILLISECONDS.toSeconds(
                    startTime
                )
            if (timeDifference > 1) {
                practiceViewModel.recordFile?.let {
                    filePath = AppDirectory.getAudioSentFile(null).absolutePath
                    chatModel.filePath = filePath
                    AppDirectory.copy(it.absolutePath, filePath!!)
                }

            }
        }

        private fun audioAttachmentInit(chatModel: ChatModel) {
            showPracticeSubmitLayout()
            binding.submitAudioViewContainer.visibility = VISIBLE
            initializePractiseSeekBar(chatModel)
            binding.submitPractiseSeekbar.max =
                Utils.getDurationOfMedia(context, filePath)?.toInt() ?: 0
            enableSubmitButton()
        }

        private fun initializePractiseSeekBar(chatModel: ChatModel) {
            binding.practiseSeekbar.progress = chatModel.playProgress
            binding.practiseSeekbar.setOnSeekBarChangeListener(
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
                        clickListener.onSeekChange(userSelectedPosition.toLong())
                    }
                })
            binding.submitPractiseSeekbar.setOnSeekBarChangeListener(
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
                        clickListener.onSeekChange(userSelectedPosition.toLong())
                    }
                })
        }

        private fun removeAudioPractice() {
            hidePracticeSubmitLayout()
            binding.submitAudioViewContainer.visibility = GONE
            binding.submitPractiseSeekbar.progress = 0
            binding.submitPractiseSeekbar.max = 0
            binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
            disableSubmitButton()
            appAnalytics?.addParam(AnalyticsEvent.PRACTICE_EXTRA.NAME, "Audio practise removed")
        }

        private fun disableSubmitButton() {
            binding.submitAnswerBtn.apply {
                isEnabled = false
                isClickable = false
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.seek_bar_background
                    )
                )
            }
        }

        private fun enableSubmitButton() {
            binding.submitAnswerBtn.apply {
                isEnabled = true
                isClickable = true
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.button_color
                    )
                )
            }
            requestFocus(
                binding.submitAnswerBtn
            )
        }

        private fun requestFocus(view: View) {
            Handler().postDelayed({
                view.parent.requestChildFocus(
                    view,
                    view
                )
            }, 200)
        }

        private fun hidePracticeInputLayout() {
            binding.practiseInputHeader.visibility = GONE
            binding.practiceInputLl.visibility = GONE
        }

        private fun showPracticeInputLayout() {
            binding.practiseInputHeader.visibility = VISIBLE
            binding.practiceInputLl.visibility = VISIBLE
        }

        private fun showPracticeSubmitLayout() {
            binding.yourSubAnswerTv.visibility = VISIBLE
            binding.subPractiseSubmitLayout.visibility = VISIBLE
        }

        private fun hidePracticeSubmitLayout() {
            binding.yourSubAnswerTv.visibility = GONE
            binding.subPractiseSubmitLayout.visibility = GONE
        }
    }

    interface PracticeClickListeners {
        fun playPracticeAudio(chatModel: ChatModel, position: Int)
        fun playSubmitPracticeAudio(chatModel: ChatModel, position: Int)
        fun removeAudioPractise(chatModel: ChatModel)
        fun submitPractice(chatModel: ChatModel): Boolean
        fun onSeekChange(seekTo: Long)
        fun startRecording(chatModel: ChatModel, position: Int, startTimeUnit: Long)
        fun stopRecording(chatModel: ChatModel, position: Int, stopTime: Long)
        fun askRecordPermission()
        fun focusChild(position: Int)
    }
}