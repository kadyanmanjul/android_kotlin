package com.joshtalks.joshskills.ui.day_wise_course.reading

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
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
import com.joshtalks.joshskills.databinding.ReadingPracticeFragmentBinding
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.EXPECTED_ENGAGE_TYPE
import com.joshtalks.joshskills.ui.pdfviewer.PdfViewerActivity
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.util.ExoAudioPlayer
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.util.ArrayList
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable

class ReadingFragment : Fragment() {

    private lateinit var binding: ReadingPracticeFragmentBinding

    private lateinit var appAnalytics: AppAnalytics
    private var audioManager: ExoAudioPlayer? = null
    private var filePath: String? = null

    private var chatModelList: ArrayList<ChatModel>? = null

    companion object {
        const val READING_OBJECT = "reading_object"

        @JvmStatic
        fun instance(chatModelList: ArrayList<ChatModel>) = ReadingFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(READING_OBJECT, chatModelList)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            chatModelList = arguments?.getParcelableArrayList<ChatModel>(READING_OBJECT)
        }
        if (chatModelList == null) {
            requireActivity().finish()
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.reading_practice_fragment, container, false)

        chatModelList?.forEach { setPracticeInfoView(it) }
        return binding.root
    }


    fun setPracticeInfoView(chatModel: ChatModel) {
        chatModel.question?.run {
            when (this.material_type) {
                /* BASE_MESSAGE_TYPE.AU -> {
                     binding.audioViewContainer.visibility = View.VISIBLE
                     this.audioList?.getOrNull(0)?.audio_url?.let {
                         binding.btnPlayInfo.tag = it
                         binding.practiseSeekbar.max = this.audioList?.getOrNull(0)?.duration!!
                         if (binding.practiseSeekbar.max == 0) {
                             binding.practiseSeekbar.max = 2_00_000
                         }
                     }
                     initializePractiseSeekBar(chatModel)

                 }*/
                BASE_MESSAGE_TYPE.IM -> {
                    binding.imageView.visibility = View.VISIBLE
                    this.imageList?.getOrNull(0)?.imageUrl?.let { path ->
                        setImageInImageView(path, binding.imageView)
                        binding.imageView.setOnClickListener {
//                                ImageShowFragment.newInstance(path, "", "")
//                                    .show(supportFragmentManager, "ImageShow")
                        }
                    }
                }
                BASE_MESSAGE_TYPE.VI -> {
                    binding.videoPlayer.visibility = View.VISIBLE
                    this.videoList?.getOrNull(0)?.video_url?.let {
                        binding.videoPlayer.setUrl(it)
                        binding.videoPlayer.fitToScreen()
                        binding.videoPlayer.setPlayListener {
                            val videoId = this.videoList?.getOrNull(0)?.id
                            val videoUrl = this.videoList?.getOrNull(0)?.video_url
                            VideoPlayerActivity.startVideoActivity(
                                requireContext(),
                                "",
                                videoId,
                                videoUrl
                            )
                        }
                        binding.videoPlayer.downloadStreamButNotPlay()
                    }
                }
                BASE_MESSAGE_TYPE.PD -> {
                    binding.imageView.visibility = View.VISIBLE
                    binding.imageView.setImageResource(R.drawable.ic_practise_pdf_ph)
                    this.pdfList?.getOrNull(0)?.let { pdfType ->
                        binding.imageView.setOnClickListener {
                            PdfViewerActivity.startPdfActivity(
                                requireContext(),
                                pdfType.id,
                                EMPTY
                            )

                        }
                    }
                }

                BASE_MESSAGE_TYPE.TX -> {
                    this.qText?.let {
                        binding.infoTv.visibility = View.VISIBLE
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
                binding.infoTv2.visibility = View.VISIBLE
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
        binding.progressBarImageView.visibility = View.VISIBLE

        Glide.with(requireContext())
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
                binding.uploadPractiseView.visibility = View.VISIBLE

                binding.practiseInputHeader.text =
                    requireContext().getString(R.string.record_answer_label)
                binding.uploadPractiseView.setImageResource(R.drawable.recv_ic_mic_white)
//                audioRecordTouchListener(chatModel)
                binding.audioPractiseHint.visibility = View.VISIBLE

            }
        }
    }

    private fun setViewUserSubmitAnswer(chatModel: ChatModel) {
        chatModel.question?.run {
            this.expectedEngageType?.let {
                hidePracticeInputLayout()
                showPracticeSubmitLayout()
                binding.yourSubAnswerTv.visibility = View.VISIBLE
                val params: ViewGroup.MarginLayoutParams =
                    binding.subPractiseSubmitLayout.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = Utils.dpToPx(20)
                binding.subPractiseSubmitLayout.layoutParams = params
                binding.yourSubAnswerTv.text =
                    requireContext().getString(R.string.your_submitted_answer)
                val practiseEngagement = this.practiceEngagement?.get(0)
                when {
                    EXPECTED_ENGAGE_TYPE.AU == it -> {
                        binding.submitAudioViewContainer.visibility = View.VISIBLE
                    }
                }
                filePath = practiseEngagement?.answerUrl
                if (PermissionUtils.isStoragePermissionEnabled(requireContext()) && AppDirectory.isFileExist(
                        practiseEngagement?.localPath
                    )
                ) {
                    filePath = practiseEngagement?.localPath
                    binding.submitPractiseSeekbar.max =
                        Utils.getDurationOfMedia(requireContext(), filePath!!)
                            ?.toInt() ?: 0
                } else {
                    if (practiseEngagement?.duration != null) {
                        binding.submitPractiseSeekbar.max = practiseEngagement.duration
                    } else {
                        binding.submitPractiseSeekbar.max = 1_00_000
                    }
                }


//                initializePractiseSeekBar(chatModel)
                binding.ivCancel.visibility = View.GONE
            }
        }
    }
/*
    @SuppressLint("ClickableViewAccessibility")
    private fun audioRecordTouchListener(chatModel: ChatModel) {
        binding.uploadPractiseView.setOnTouchListener { _, event ->
            if (PermissionUtils.isAudioAndStoragePermissionEnable(requireContext()).not()) {
                askRecordPermission()
                return@setOnTouchListener true
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.videoPlayer.onPause()
                    binding.rootView.requestDisallowInterceptTouchEvent(true)
                    binding.counterContainer.visibility = View.VISIBLE
                    val scaleAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.scale)
                    binding.uploadPractiseView.startAnimation(scaleAnimation)
                    binding.counterTv.base = SystemClock.elapsedRealtime()
                    startTime = System.currentTimeMillis()
                    binding.counterTv.start()
                    startRecording(chatModel, layoutPosition, startTime)
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
                    binding.audioPractiseHint.visibility = View.VISIBLE

                    val timeDifference =
                        TimeUnit.MILLISECONDS.toSeconds(stopTime) - TimeUnit.MILLISECONDS.toSeconds(
                            startTime
                        )
                    if (timeDifference > 1) {
                        audioAttachmentInit(chatModel)
                        */
/*practiceViewModel.recordFile?.let {
//                                isAudioRecordDone = true
                            filePath = AppDirectory.getAudioSentFile(null).absolutePath
                            AppDirectory.copy(it.absolutePath, filePath!!)
                            audioAttachmentInit(chatModel)
                        }*//*


                    }
                }
            }

            true
        }
    }

    fun playPracticeAudio() {
        if (Utils.getCurrentMediaVolume(applicationContext) <= 0) {
            StyleableToast.Builder(applicationContext).gravity(Gravity.BOTTOM)
                .text(getString(R.string.volume_up_message)).cornerRadius(16)
                .length(Toast.LENGTH_LONG)
                .solidBackground().show()
        }
        appAnalytics.addParam(AnalyticsEvent.PRACTICE_EXTRA.NAME, "Audio Played")

        if (currentAudio == null) {
            onPlayAudio(chatModel, chatModel.question?.audioList?.getOrNull(0)!!)
        } else {
            if (currentAudio == chatModel.question?.audioList?.getOrNull(0)?.audio_url) {
                if (checkIsPlayer()) {
                    audioManager?.setProgressUpdateListener(this)
                    audioManager?.resumeOrPause()
                } else {
                    onPlayAudio(chatModel, chatModel.question?.audioList?.getOrNull(0)!!)
                }
            } else {
                onPlayAudio(chatModel, chatModel.question?.audioList?.getOrNull(0)!!)
            }
        }

    }

    fun playSubmitPracticeAudio() {
        try {
            val audioType = AudioType()
            audioType.audio_url = filePath!!
            audioType.downloadedLocalPath = filePath!!
            audioType.duration = Utils.getDurationOfMedia(this, filePath!!)?.toInt() ?: 0
            audioType.id = Random.nextInt().toString()
            appAnalytics.addParam(
                AnalyticsEvent.PRACTICE_EXTRA.NAME,
                "Already Submitted audio Played"
            )

            val state =
                if (binding.submitBtnPlayInfo.state == MaterialPlayPauseDrawable.State.Pause && audioManager!!.isPlaying()) {
                    audioManager?.setProgressUpdateListener(this)
                    MaterialPlayPauseDrawable.State.Play
                } else {
                    MaterialPlayPauseDrawable.State.Pause
                }
            binding.submitBtnPlayInfo.state = state

            if (Utils.getCurrentMediaVolume(applicationContext) <= 0) {
                StyleableToast.Builder(applicationContext).gravity(Gravity.BOTTOM)
                    .text(getString(R.string.volume_up_message)).cornerRadius(16)
                    .length(Toast.LENGTH_LONG)
                    .solidBackground().show()
            }

            if (currentAudio == null) {
                onPlayAudio(chatModel, audioType)
            } else {
                if (currentAudio == audioType.audio_url) {
                    if (checkIsPlayer()) {
                        audioManager?.setProgressUpdateListener(this)
                        audioManager?.resumeOrPause()
                    } else {
                        onPlayAudio(chatModel, audioType)
                    }
                } else {
                    onPlayAudio(chatModel, audioType)

                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    fun removeAudioPractise() {
        filePath = null
        currentAudio = null
        binding.practiseSubmitLayout.visibility = View.GONE
        binding.submitAudioViewContainer.visibility = View.GONE
        isAudioRecordDone = false
        binding.submitPractiseSeekbar.progress = 0
        binding.submitPractiseSeekbar.max = 0
        binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
        if (isAudioPlaying()) {
            audioManager?.resumeOrPause()
        }
        disableSubmitButton()
        appAnalytics.addParam(AnalyticsEvent.PRACTICE_EXTRA.NAME, "Audio practise removed")

    }

    private fun initVideoPractise(path: String) {
        val videoSentFile = AppDirectory.videoSentFile()
        AppDirectory.copy(path, videoSentFile.absolutePath)
        filePath = videoSentFile.absolutePath
        isVideoRecordDone = true
        binding.practiseSubmitLayout.visibility = View.VISIBLE
        binding.videoPlayerSubmit.init()
        binding.videoPlayerSubmit.visibility = View.VISIBLE
        binding.videoPlayerSubmit.setUrl(filePath)
        binding.videoPlayerSubmit.fitToScreen()
        binding.videoPlayerSubmit.downloadStreamButNotPlay()
        binding.videoPlayerSubmit.setPlayListener {
            VideoPlayerActivity.startVideoActivity(
                this@PractiseSubmitActivity,
                null,
                null,
                filePath
            )
        }
        enableSubmitButton()
        scrollToEnd()
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
*/

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

    fun askRecordPermission() {

        PermissionUtils.audioRecordStorageReadAndWritePermission(
            requireActivity(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        /*if (flag) {
                            binding.uploadPractiseView.setOnClickListener(null)
                            audioRecordTouchListener()
                            return
                        }*/
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(
                                requireActivity(),
                                R.string.record_permission_message
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
    }

    private fun audioAttachmentInit(chatModel: ChatModel) {
        showPracticeSubmitLayout()
        binding.submitAudioViewContainer.visibility = View.VISIBLE
//        initializePractiseSeekBar(chatModel)
        binding.submitPractiseSeekbar.max =
            Utils.getDurationOfMedia(requireContext(), filePath)?.toInt() ?: 0
        enableSubmitButton()
    }

    fun removeAudioPractice() {
        hidePracticeSubmitLayout()
        binding.submitAudioViewContainer.visibility = View.GONE
        binding.submitPractiseSeekbar.progress = 0
        binding.submitPractiseSeekbar.max = 0
        binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
        disableSubmitButton()
        appAnalytics.addParam(AnalyticsEvent.PRACTICE_EXTRA.NAME, "Audio practise removed")
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