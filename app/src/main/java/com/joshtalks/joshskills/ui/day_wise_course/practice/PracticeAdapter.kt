package com.joshtalks.joshskills.ui.day_wise_course.practice

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.SeekBar
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
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.PracticeItemLayoutBinding
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.EXPECTED_ENGAGE_TYPE
import com.joshtalks.joshskills.ui.pdfviewer.PdfViewerActivity
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import java.util.concurrent.TimeUnit
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable

class PracticeAdapter(
    val context: Context,
    val itemList: ArrayList<ChatModel>,
    val clickListener: PracticeClickListeners
) :
    RecyclerView.Adapter<PracticeAdapter.PracticeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PracticeViewHolder {
        val binding = PracticeItemLayoutBinding.inflate(LayoutInflater.from(context), parent, false)

        return PracticeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PracticeViewHolder, position: Int) {
        holder.bind(itemList.get(position))
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class PracticeViewHolder(val binding: PracticeItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var startTime: Long = 0L
        var filePath: String? = null
        var appAnalytics: AppAnalytics? = null

        fun bind(chatModel: ChatModel) {
            appAnalytics = AppAnalytics.create(AnalyticsEvent.PRACTICE_SCREEN.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam("chatId", chatModel.chatId)
            setPracticeInfoView(chatModel)

            binding.practiceTitleTv.setOnClickListener {
                if (binding.practiceContentLl.visibility == View.GONE) {
                    binding.practiceContentLl.visibility = View.VISIBLE
                    binding.practiceTitleTv.setCompoundDrawablesWithIntrinsicBounds(
                        null,
                        null,
                        ContextCompat.getDrawable(context, R.drawable.ic_remove),
                        null
                    )
                } else {
                    binding.practiceContentLl.visibility = View.GONE
                    binding.practiceTitleTv.setCompoundDrawablesWithIntrinsicBounds(
                        null,
                        null,
                        ContextCompat.getDrawable(context, R.drawable.ic_add),
                        null
                    )
                }
            }
            binding.btnPlayInfo.setOnClickListener {
                appAnalytics?.addParam(AnalyticsEvent.PRACTICE_EXTRA.NAME, "Audio Played")
                clickListener.playPracticeAudio(chatModel, layoutPosition)
            }

            binding.submitBtnPlayInfo.setOnClickListener {
                appAnalytics?.addParam(
                    AnalyticsEvent.PRACTICE_EXTRA.NAME,
                    "Already Submitted audio Played"
                )
                clickListener.playSubmitPracticeAudio(chatModel, layoutPosition)
                val state =
                    if (chatModel.isPlaying) {
                        MaterialPlayPauseDrawable.State.Play
                    } else {
                        MaterialPlayPauseDrawable.State.Pause
                    }
                binding.submitBtnPlayInfo.state = state
            }

            binding.ivCancel.setOnClickListener {
                chatModel.filePath = null
                clickListener.removeAudioPractise(chatModel)
                removeAudioPractice()
            }

            binding.submitAnswerBtn.setOnClickListener {

                if (chatModel.filePath == null) {
                    showToast(context.getString(R.string.submit_practise_msz))
                    return@setOnClickListener
                }

                if (clickListener.submitPractise(chatModel)) {
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

        fun setPracticeInfoView(chatModel: ChatModel) {
            chatModel.question?.run {
                when (this.material_type) {
                    BASE_MESSAGE_TYPE.AU -> {
                        binding.audioViewContainer.visibility = View.VISIBLE
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
                }

                if ((this.material_type == BASE_MESSAGE_TYPE.TX).not() && this.qText.isNullOrEmpty()
                        .not()
                ) {
                    binding.infoTv2.text =
                        HtmlCompat.fromHtml(this.qText!!, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    binding.infoTv2.visibility = VISIBLE
                }

                if (this.practiceEngagement.isNullOrEmpty()) {
                    binding.submitAnswerBtn.visibility = View.VISIBLE
                    setViewAccordingExpectedAnswer(chatModel)
                } else {
                    hidePracticeInputLayout()
                    binding.submitAnswerBtn.visibility = View.GONE
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
                        binding.progressBarImageView.visibility = View.GONE

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
                    binding.uploadFileView.visibility = VISIBLE

                    binding.practiseInputHeader.text =
                        context.getString(R.string.record_answer_label)
                    binding.uploadPractiseView.setImageResource(R.drawable.recv_ic_mic_white)
                    audioRecordTouchListener(chatModel)
                    binding.audioPractiseHint.visibility = VISIBLE

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
                    val practiseEngagement = this.practiceEngagement?.get(0)
                    when {
                        EXPECTED_ENGAGE_TYPE.AU == it -> {
                            binding.submitAudioViewContainer.visibility = VISIBLE
                        }
                    }
                    filePath = practiseEngagement?.answerUrl
                    if (PermissionUtils.isStoragePermissionEnabled(context) && AppDirectory.isFileExist(
                            practiseEngagement?.localPath
                        )
                    ) {
                        filePath = practiseEngagement?.localPath
                        binding.submitPractiseSeekbar.max =
                            Utils.getDurationOfMedia(context, filePath!!)
                                ?.toInt() ?: 0
                    } else {
                        if (practiseEngagement?.duration != null) {
                            binding.submitPractiseSeekbar.max = practiseEngagement.duration
                        } else {
                            binding.submitPractiseSeekbar.max = 1_00_000
                        }
                    }


                    initializePractiseSeekBar(chatModel)
                    binding.ivCancel.visibility = View.GONE
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
                        clickListener.startRecording(chatModel, layoutPosition, startTime)
                        binding.audioPractiseHint.visibility = View.GONE

                        appAnalytics?.addParam(AnalyticsEvent.AUDIO_RECORD.NAME, "Audio Recording")
                    }
                    MotionEvent.ACTION_MOVE -> {
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        binding.rootView.requestDisallowInterceptTouchEvent(false)
                        binding.counterTv.stop()
                        val stopTime = System.currentTimeMillis()
                        clickListener.stopRecording(chatModel, layoutPosition, stopTime)
                        binding.uploadPractiseView.clearAnimation()
                        binding.counterContainer.visibility = View.GONE
                        binding.audioPractiseHint.visibility = VISIBLE
//                        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                        val timeDifference =
                            TimeUnit.MILLISECONDS.toSeconds(stopTime) - TimeUnit.MILLISECONDS.toSeconds(
                                startTime
                            )
                        if (timeDifference > 1) {
                            audioAttachmentInit(chatModel)
                            /*practiceViewModel.recordFile?.let {
//                                isAudioRecordDone = true
                                filePath = AppDirectory.getAudioSentFile(null).absolutePath
                                AppDirectory.copy(it.absolutePath, filePath!!)
                                audioAttachmentInit(chatModel)
                            }*/

                        }
                    }
                }

                true
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

        fun removeAudioPractice() {
            hidePracticeSubmitLayout()
            binding.submitAudioViewContainer.visibility = View.GONE
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
        }

        fun hidePracticeInputLayout() {
            binding.practiseInputHeader.visibility = View.GONE
            binding.practiceInputLl.visibility = View.GONE
        }

        fun showPracticeInputLayout() {
            binding.practiseInputHeader.visibility = View.VISIBLE
            binding.practiceInputLl.visibility = View.VISIBLE
        }

        fun showPracticeSubmitLayout() {
            binding.yourSubAnswerTv.visibility = View.VISIBLE
            binding.subPractiseSubmitLayout.visibility = View.VISIBLE
        }

        fun hidePracticeSubmitLayout() {
            binding.yourSubAnswerTv.visibility = View.GONE
            binding.subPractiseSubmitLayout.visibility = View.GONE
        }
    }

    interface PracticeClickListeners {
        fun playPracticeAudio(chatModel: ChatModel, position: Int)
        fun playSubmitPracticeAudio(chatModel: ChatModel, position: Int)
        fun removeAudioPractise(chatModel: ChatModel)
        fun submitPractise(chatModel: ChatModel): Boolean
        fun onSeekChange(seekTo: Long)
        fun startRecording(chatModel: ChatModel, position: Int, startTimeUnit: Long)
        fun stopRecording(chatModel: ChatModel, position: Int, stopTime: Long)
        fun askRecordPermission()
    }
}