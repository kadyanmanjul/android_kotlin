package com.joshtalks.joshskills.ui.day_wise_course.reading

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.greentoad.turtlebody.mediapicker.MediaPicker
import com.greentoad.turtlebody.mediapicker.core.MediaPickerConfig
import com.joshtalks.joshcamerax.JoshCameraActivity
import com.joshtalks.joshcamerax.utils.ImageQuality
import com.joshtalks.joshcamerax.utils.Options
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ReadingPracticeFragmentWithoutFeedbackBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.EXPECTED_ENGAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.NPSEvent
import com.joshtalks.joshskills.repository.local.entity.PracticeEngagement
import com.joshtalks.joshskills.repository.local.entity.PracticeFeedback2
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.RemovePracticeAudioEventBus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.ui.day_wise_course.CapsuleActivityCallback
import com.joshtalks.joshskills.ui.extra.ImageShowFragment
import com.joshtalks.joshskills.ui.pdfviewer.PdfViewerActivity
import com.joshtalks.joshskills.ui.practise.PracticeViewModel
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.util.ExoAudioPlayer
import com.joshtalks.joshskills.util.ExoAudioPlayer.ProgressUpdateListener
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.muddzdev.styleabletoast.StyleableToast
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable
import java.util.ArrayList
import java.util.concurrent.TimeUnit

class ReadingFragmentWithoutFeedback : CoreJoshFragment(), Player.EventListener, AudioPlayerEventListener,
    ProgressUpdateListener {

    private var compositeDisposable = CompositeDisposable()

    private lateinit var binding: ReadingPracticeFragmentWithoutFeedbackBinding
    private lateinit var chatModel: ChatModel
    private var chatList: ArrayList<ChatModel>? = null
    private var sBound = false
    private var mUserIsSeeking = false
    private var isAudioRecordDone = false
    private var isVideoRecordDone = false
    private var isDocumentAttachDone = false
    private var scaleAnimation: Animation? = null
    private var startTime: Long = 0
    private var totalTimeSpend: Long = 0
    private var filePath: String? = null
    private var appAnalytics: AppAnalytics? = null
    private var audioManager: ExoAudioPlayer? = null
    var activityCallback: CapsuleActivityCallback? = null


    private val DOCX_FILE_MIME_TYPE = arrayOf(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/msword", "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/*",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.oasis.opendocument.text",
        "application/vnd.oasis.opendocument.spreadsheet"
    )


    private val practiceViewModel: PracticeViewModel by lazy {
        ViewModelProvider(this).get(PracticeViewModel::class.java)
    }

    companion object {

        const val PRACTISE_OBJECT = "practise_object"
        const val IMAGE_OR_VIDEO_SELECT_REQUEST_CODE = 1081
        const val TEXT_FILE_ATTACHMENT_REQUEST_CODE = 1082

        @JvmStatic
        fun instance(chatModelList: ArrayList<ChatModel>) = ReadingFragmentWithoutFeedback().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(PRACTISE_OBJECT, chatModelList)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        /*requireActivity().requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }*/
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            arguments?.let {
                chatList = it.getParcelableArrayList<ChatModel>(PRACTISE_OBJECT)
            }
        }
        totalTimeSpend = System.currentTimeMillis()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (chatList == null || chatList?.size == 0)
            return null
        else
            chatModel = chatList!!.get(0)

        binding =
            DataBindingUtil.inflate(inflater, R.layout.reading_practice_fragment_without_feedback, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        scaleAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.scale)

        appAnalytics = AppAnalytics.create(AnalyticsEvent.PRACTICE_SCREEN.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam("chatId", chatModel.chatId)

        setPracticeInfoView()
        addObserver()
        chatModel.question?.run {
            if (this.practiceEngagement.isNullOrEmpty()) {
                binding.submitAnswerBtn.visibility = VISIBLE
                appAnalytics?.addParam(AnalyticsEvent.PRACTICE_SOLVED.NAME, false)
                appAnalytics?.addParam(AnalyticsEvent.PRACTICE_STATUS.NAME, "Not Submitted")
                setViewAccordingExpectedAnswer()
            } else {
                binding.submitAnswerBtn.visibility = GONE
                //binding.improveAnswerBtn.visibility = VISIBLE
                binding.continueBtn.visibility = View.VISIBLE
                appAnalytics?.addParam(AnalyticsEvent.PRACTICE_SOLVED.NAME, true)
                appAnalytics?.addParam(AnalyticsEvent.PRACTICE_STATUS.NAME, "Already Submitted")
                setViewUserSubmitAnswer()
            }
        }

        coreJoshActivity?.feedbackEngagementStatus(chatModel.question)

        return binding.rootView
    }

    fun hidePracticeInputLayout() {
        binding.practiseInputHeader.visibility = View.GONE
        binding.practiseInputLayout.visibility = View.GONE
    }

    fun showPracticeInputLayout() {
        binding.practiseInputHeader.visibility = View.VISIBLE
        binding.practiseInputLayout.visibility = View.VISIBLE
    }

    fun showPracticeSubmitLayout() {
        binding.yourSubAnswerTv.visibility = View.VISIBLE
        binding.subPractiseSubmitLayout.visibility = View.VISIBLE
    }

    fun hidePracticeSubmitLayout() {
        binding.yourSubAnswerTv.visibility = View.GONE
        binding.subPractiseSubmitLayout.visibility = View.GONE
    }

    fun showImproveButton() {
        // binding.feedbackLayout.visibility = VISIBLE
        //binding.improveAnswerBtn.visibility = VISIBLE
        binding.continueBtn.visibility = View.VISIBLE
        binding.submitAnswerBtn.visibility = GONE
    }

    fun hideImproveButton() {
        binding.feedbackLayout.visibility = GONE
        binding.improveAnswerBtn.visibility = GONE
        binding.submitAnswerBtn.visibility = VISIBLE
        binding.continueBtn.visibility = View.GONE
    }


    override fun onResume() {
        super.onResume()
        subscribeRXBus()
        /*requireActivity().requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }*/
        try {
            binding.videoPlayer.onResume()
        } catch (ex: Exception) {
        }
        try {
            if (filePath.isNullOrEmpty().not()) {
                binding.videoPlayerSubmit.onResume()
            }
        } catch (ex: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()

        if (audioManager != null) {
            audioManager?.onPause()
        }

        try {
            binding.videoPlayer.onPause()
            pauseAllViewHolderAudio()

        } catch (ex: Exception) {

        }
        try {
            if (filePath.isNullOrEmpty().not()) {
                binding.videoPlayerSubmit.onPause()
            }
        } catch (ex: Exception) {

        }
    }

    private fun pauseAllViewHolderAudio() {
        val viewHolders = binding.audioList.allViewResolvers as List<PracticeAudioViewHolder>
        viewHolders.forEach { it ->
            it.pauseAudio()
        }
    }


    override fun onStop() {
        appAnalytics?.push()
        super.onStop()
        compositeDisposable.clear()
        try {
            binding.videoPlayer.onPause()

        } catch (ex: Exception) {
        }
        try {
            if (filePath.isNullOrEmpty().not()) {
                binding.videoPlayerSubmit.onPause()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
            try {
                binding.videoPlayer.onStop()

            } catch (ex: Exception) {
            }
            try {
                if (filePath.isNullOrEmpty().not()) {
                    binding.videoPlayerSubmit.onStop()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            audioManager?.release()
        } catch (ex: Exception) {

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        try {
            if (requestCode == IMAGE_OR_VIDEO_SELECT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                data?.let { intent ->
                    when {
                        intent.hasExtra(JoshCameraActivity.IMAGE_RESULTS) -> {
                            val returnValue =
                                intent.getStringArrayListExtra(JoshCameraActivity.IMAGE_RESULTS)
                            returnValue?.get(0)?.let {
                                filePath = it
                            }
                        }
                        intent.hasExtra(JoshCameraActivity.VIDEO_RESULTS) -> {
                            val videoPath = intent.getStringExtra(JoshCameraActivity.VIDEO_RESULTS)
                            videoPath?.run {
                                initVideoPractise(this)
                            }
                        }
                        else -> return
                    }
                }
            } else if (requestCode == TEXT_FILE_ATTACHMENT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                data?.data?.let {
                    requireActivity().contentResolver.query(it, null, null, null, null)
                        ?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            //val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            cursor.moveToFirst()
                            val fileName = cursor.getString(nameIndex)
                            val file = AppDirectory.copy2(
                                it,
                                AppDirectory.getSentFile(fileName)
                            )
                            file?.run {
                                filePath = this.absolutePath
                                isDocumentAttachDone = true
                                hidePracticeInputLayout()
                                showPracticeSubmitLayout()
                                binding.submitFileViewContainer.visibility = VISIBLE
                                binding.fileInfoAttachmentTv.text = fileName
                                enableSubmitButton()
                            }
                        }
                }
            }

        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            ex.printStackTrace()
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listen(RemovePracticeAudioEventBus::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe({
                    Handler(Looper.getMainLooper()).post {
                        binding.audioList.removeView(it.practiceAudioViewHolder)
                        chatModel.question?.run {
                            if (this.practiceEngagement.isNullOrEmpty()) {
                                showPracticeInputLayout()
                                binding.feedbackLayout.visibility = GONE
                                hidePracticeSubmitLayout()
                                disableSubmitButton()
                            } else {
                                hidePracticeInputLayout()
                                showImproveButton()
                            }
                        }
                    }
                }, {
                    it.printStackTrace()
                })
        )
    }

    private fun setPracticeInfoView() {
        chatModel.question?.run {
            appAnalytics?.addParam(
                AnalyticsEvent.PRACTICE_TYPE_PRESENT.NAME,
                "${this.material_type} Practice present"
            )
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
                    initializePractiseSeekBar()
                }

                BASE_MESSAGE_TYPE.IM -> {
                    binding.imageView.visibility = VISIBLE
                    this.imageList?.getOrNull(0)?.imageUrl?.let { path ->
                        setImageInImageView(path, binding.imageView)
                        binding.imageView.setOnClickListener {
                            ImageShowFragment.newInstance(path, "", "")
                                .show(childFragmentManager, "ImageShow")
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
                                requireActivity(),
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
                                requireActivity(),
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
            if ((this.material_type == BASE_MESSAGE_TYPE.TX).not()) {
                if (this.qText.isNullOrEmpty().not()) {
                    binding.practiseTextInfoLayout.visibility = VISIBLE
                    binding.infoTv2.visibility = VISIBLE
                    binding.infoTv2.text =
                        HtmlCompat.fromHtml(this.qText!!, HtmlCompat.FROM_HTML_MODE_LEGACY)
                }
            }
        }
    }

    private fun setViewAccordingExpectedAnswer() {
        chatModel.question?.run {
            showPracticeInputLayout()
            this.expectedEngageType?.let {
                if ((it == EXPECTED_ENGAGE_TYPE.TX).not()) {
                    binding.uploadPractiseView.visibility = VISIBLE
                    binding.uploadFileView.visibility = VISIBLE
                }
                when {
                    EXPECTED_ENGAGE_TYPE.TX == it -> {
                        binding.practiseInputHeader.text = getString(R.string.type_answer_label)
                        binding.etPractise.visibility = VISIBLE
                        binding.etPractise.addTextChangedListener(object : TextWatcher {
                            override fun afterTextChanged(s: Editable) {
                                if (s.isEmpty()) {
                                    disableSubmitButton()
                                } else {
                                    enableSubmitButton()
                                }
                            }

                            override fun beforeTextChanged(
                                s: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int
                            ) {
                            }

                            override fun onTextChanged(
                                s: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {
                            }

                        })

                    }
                    EXPECTED_ENGAGE_TYPE.AU == it -> {
                        binding.practiseInputHeader.text =
                            AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.READING_PRACTICE_TITLE)
                        binding.uploadPractiseView.setImageResource(R.drawable.recv_ic_mic_white)
                        audioRecordTouchListener()
                        binding.audioPractiseHint.visibility = VISIBLE
                    }
                    EXPECTED_ENGAGE_TYPE.VI == it -> {
                        binding.practiseInputHeader.text =
                            AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.READING_PRACTICE_TITLE)
                        binding.uploadPractiseView.setImageResource(R.drawable.ic_videocam)
                        setupFileUploadListener(it)
                    }
                    EXPECTED_ENGAGE_TYPE.IM == it -> {
                        binding.practiseInputHeader.text = getString(R.string.upload_answer_label)
                        binding.uploadPractiseView.setImageResource(R.drawable.ic_camera_2)
                        setupFileUploadListener(it)
                    }
                    EXPECTED_ENGAGE_TYPE.DX == it -> {
                        binding.practiseInputHeader.text = getString(R.string.upload_answer_label)
                        binding.uploadPractiseView.setImageResource(R.drawable.ic_file_upload)
                        setupFileUploadListener(it)
                        binding.uploadFileView.visibility = GONE

                    }
                }
            }
        }
    }

    private fun addObserver() {
        practiceViewModel.requestStatusLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                hidePracticeInputLayout()
                binding.submitAnswerBtn.visibility = GONE
                binding.progressLayout.visibility = GONE
                //binding.feedbackResultProgressLl.visibility = VISIBLE
                binding.rootView.postDelayed(Runnable {
                    binding.rootView.smoothScrollTo(
                        0,
                        binding.rootView.height
                    )
                }, 100)

                binding.feedbackResultLinearLl.visibility = GONE
                hideCancelButtonInRV()
                binding.feedbackLayout.setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                    )
                )
                binding.feedbackResultProgressLl.visibility = GONE
                binding.feedbackResultLinearLl.visibility = VISIBLE
                binding.progressLayout.visibility = GONE
                binding.submitAnswerBtn.visibility = GONE
                //binding.improveAnswerBtn.visibility = VISIBLE
                binding.continueBtn.visibility = View.VISIBLE

                CoroutineScope(Dispatchers.IO).launch {
                    chatModel.question?.interval?.run {
                        WorkManagerAdmin.determineNPAEvent(NPSEvent.PRACTICE_COMPLETED, this)
                    }
                    activityCallback?.onQuestionStatusUpdate(
                        QUESTION_STATUS.AT.name,
                        chatModel.question?.questionId?.toIntOrNull() ?: 0
                    )
                }

            } else {
                enableSubmitButton()
                binding.progressLayout.visibility = GONE
            }
        })

        practiceViewModel.practiceFeedback2LiveData.observe(viewLifecycleOwner, Observer {
            setFeedBackLayout(it)
            hideCancelButtonInRV()
            binding.feedbackLayout.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            binding.feedbackResultProgressLl.visibility = GONE
            binding.feedbackResultLinearLl.visibility = VISIBLE
            binding.progressLayout.visibility = GONE
            binding.submitAnswerBtn.visibility = GONE
            //binding.improveAnswerBtn.visibility = VISIBLE
            binding.continueBtn.visibility = View.VISIBLE
        })

        practiceViewModel.practiceEngagementData.observe(viewLifecycleOwner, Observer {
            updatePracticeFeedback(it)
        })
    }

    private fun updatePracticeFeedback(practiceEngagement: PracticeEngagement) {
        val viewHolders = binding.audioList.allViewResolvers as List<PracticeAudioViewHolder>
        viewHolders.forEach { it ->
            it.let {
                if (it.isEmpty()) {
                    it.updatePracticeEngagement(practiceEngagement)
                }
            }
        }
    }

    private fun hideCancelButtonInRV() {
        val viewHolders = binding.audioList.allViewResolvers as List<PracticeAudioViewHolder>
        viewHolders.forEach { it ->
            it.let {
                it.hideCancelButtons()
                //it.setSeekToZero()
            }
        }
    }

    private fun removePreviousAddedViewHolder() {
        val viewHolders = binding.audioList.allViewResolvers as List<PracticeAudioViewHolder>
        viewHolders.forEach { it ->
            it.let {
                if (it.isEmpty()) {
                    binding.audioList.removeView(it)
                }
            }
        }
    }

    fun setFeedBackLayout(feedback2: PracticeFeedback2?, isProcessing: Boolean = false) {
        //binding.feedbackLayout.visibility = VISIBLE
        if (isProcessing) {
            binding.progressLayout.visibility = VISIBLE
            binding.feedbackGrade.visibility = GONE
            binding.feedbackDescription.visibility = GONE
        } else if (feedback2 != null) {
            binding.feedbackGrade.visibility = VISIBLE
            binding.feedbackDescription.visibility = VISIBLE
            binding.progressLayout.visibility = GONE
            binding.feedbackGrade.text = feedback2.grade
            binding.feedbackDescription.text = feedback2.text
        }
    }

    private fun setViewUserSubmitAnswer() {
        chatModel.question?.run {
            this.expectedEngageType?.let {
                hidePracticeInputLayout()
                showPracticeSubmitLayout()
                binding.yourSubAnswerTv.visibility = VISIBLE
                val params: ViewGroup.MarginLayoutParams =
                    binding.subPractiseSubmitLayout.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = Utils.dpToPx(20)
                binding.subPractiseSubmitLayout.layoutParams = params
                binding.yourSubAnswerTv.text = getString(R.string.your_submitted_answer)
                val practiseEngagement = this.practiceEngagement?.get(0)
                when {
                    EXPECTED_ENGAGE_TYPE.TX == it -> {
                        binding.etSubmitText.visibility = VISIBLE
                        binding.etSubmitText.text = practiseEngagement?.text
                        binding.etSubmitText.isFocusableInTouchMode = false
                        binding.etSubmitText.isEnabled = false
                    }
                    EXPECTED_ENGAGE_TYPE.AU == it -> {
                        initRV()
                        addAudioListRV(this.practiceEngagement)
                        filePath = practiseEngagement?.answerUrl

                        //initializePractiseSeekBar()
                    }
                    EXPECTED_ENGAGE_TYPE.VI == it -> {
                        filePath = practiseEngagement?.answerUrl
                        binding.videoPlayerSubmit.visibility = VISIBLE

                        if (PermissionUtils.isStoragePermissionEnabled(requireActivity()) && AppDirectory.isFileExist(
                                practiseEngagement?.localPath
                            )
                        ) {
                            filePath = practiseEngagement?.localPath
                        }

                        filePath?.run {
                            binding.videoPlayerSubmit.setUrl(filePath)
                            binding.videoPlayerSubmit.fitToScreen()
                            binding.videoPlayerSubmit.setPlayListener {
                                VideoPlayerActivity.startVideoActivity(
                                    requireActivity(),
                                    null,
                                    null,
                                    filePath
                                )
                                /* FullScreenVideoFragment.newInstance(filePath!!)
                                     .show(supportFragmentManager, "VideoPlay")*/
                            }
                            binding.videoPlayerSubmit.downloadStreamButNotPlay()

                        }
                    }
                    EXPECTED_ENGAGE_TYPE.IM == it -> {

                    }
                    EXPECTED_ENGAGE_TYPE.DX == it -> {
                        filePath = practiseEngagement?.answerUrl
                        binding.submitFileViewContainer.visibility = VISIBLE
                        binding.fileInfoAttachmentTv.text = Utils.getFileNameFromURL(filePath)
                    }
                    else -> {

                    }
                }
            }
        }
    }

    private fun initRV() {
        val linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.audioList.builder.setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
        val divider = DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        divider.setDrawable(
            ColorDrawable(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.seek_bar_background
                )
            )
        )
        binding.audioList.addItemDecoration(divider)
    }

    private fun addAudioListRV(practiceEngagement: List<PracticeEngagement>?) {
        showPracticeSubmitLayout()
        binding.yourSubAnswerTv.visibility = View.VISIBLE
        if(practiceEngagement.isNullOrEmpty().not())
            binding.practiseSubmitLayout.visibility = VISIBLE
        binding.subPractiseSubmitLayout.visibility = VISIBLE
        binding.audioList.visibility = VISIBLE
        practiceEngagement?.let { practiceList ->
            if (practiceList.isNullOrEmpty().not()) {
                practiceList.forEach { practice ->
                    binding.audioList.addView(
                        PracticeAudioViewHolder(
                            practice,
                            context,
                            practice.answerUrl
                        )
                    )
                    if (practice.practiceFeedback != null) {
                        //binding.feedbackLayout.visibility = VISIBLE
                        //binding.feedbackGrade.text = practice.practiceFeedback!!.grade
                        //binding.feedbackDescription.text = practice.practiceFeedback!!.text
                    }
                }
            }
        }
    }


    private fun setImageInImageView(url: String, imageView: ImageView) {
        binding.progressBarImageView.visibility = VISIBLE


        Glide.with(AppObjectController.joshApplication)
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


    private fun initializePractiseSeekBar() {
        binding.practiseSeekbar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                var userSelectedPosition = 0
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    mUserIsSeeking = true
                }

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        userSelectedPosition = progress
                    }
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    mUserIsSeeking = false
                    audioManager?.seekTo(userSelectedPosition.toLong())
                }
            })
        val viewHolders = binding.audioList.allViewResolvers as List<*>
        viewHolders.forEach {
            it?.let {
                if (it is PracticeAudioViewHolder && it.isSeekBaarInitialized()) {
                    it.initializePractiseSeekBar()
                    //it.setSeekToZero()
                }
            }
        }
    }


    private fun setupFileUploadListener(expectedEngageType: EXPECTED_ENGAGE_TYPE) {
        binding.uploadPractiseView.setOnClickListener {
            when {
                EXPECTED_ENGAGE_TYPE.VI == expectedEngageType -> {
                    uploadMedia()
                }
                EXPECTED_ENGAGE_TYPE.IM == expectedEngageType -> {
                    uploadMedia()
                }
                EXPECTED_ENGAGE_TYPE.DX == expectedEngageType -> {
                    uploadTextFileChooser()
                }
            }
        }
    }

    fun chooseFile() {
        chatModel.question?.expectedEngageType?.let { expectedEngageType ->
            PermissionUtils.cameraRecordStorageReadAndWritePermission(
                requireActivity(),
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                if (EXPECTED_ENGAGE_TYPE.VI == expectedEngageType) {
                                    selectVideoFromStorage()
                                } else if (EXPECTED_ENGAGE_TYPE.AU == expectedEngageType) {
                                    selectAudioFromStorage()
                                }
                                return
                            }
                            if (report.isAnyPermissionPermanentlyDenied) {
                                PermissionUtils.cameraStoragePermissionPermanentlyDeniedDialog(
                                    requireActivity()
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
    }


    private fun uploadMedia() {
        PermissionUtils.cameraRecordStorageReadAndWritePermission(
            requireActivity(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            val options = Options.init()
                                .setRequestCode(IMAGE_OR_VIDEO_SELECT_REQUEST_CODE)
                                .setCount(1)
                                .setFrontfacing(false)
                                .setPath(AppDirectory.getTempPath())
                                .setImageQuality(ImageQuality.HIGH)
                                .setScreenOrientation(Options.SCREEN_ORIENTATION_PORTRAIT)

                            JoshCameraActivity.startJoshCameraxActivity(
                                requireActivity(),
                                options
                            )
                            return

                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.cameraStoragePermissionPermanentlyDeniedDialog(
                                requireActivity()
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


    @SuppressLint("CheckResult")
    private fun selectVideoFromStorage() {
        val pickerConfig = MediaPickerConfig()
            .setUriPermanentAccess(true)
            .setAllowMultiSelection(false)
            .setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        MediaPicker.with(requireActivity(), MediaPicker.MediaTypes.VIDEO)
            .setConfig(pickerConfig)
            .setFileMissingListener(object :
                MediaPicker.MediaPickerImpl.OnMediaListener {
                override fun onMissingFileWarning() {
                }
            })
            .onResult()
            .subscribeOn(Schedulers.io())
            .subscribe({
                it.let {
                    it[0].path?.let { path ->
                        initVideoPractise(path)
                    }
                }
            }, {
                it.printStackTrace()
            })
    }

    @SuppressLint("CheckResult")
    private fun selectAudioFromStorage() {
        val pickerConfig = MediaPickerConfig()
            .setUriPermanentAccess(true)
            .setAllowMultiSelection(false)
            .setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        MediaPicker.with(requireActivity(), MediaPicker.MediaTypes.AUDIO)
            .setConfig(pickerConfig)
            .setFileMissingListener(object :
                MediaPicker.MediaPickerImpl.OnMediaListener {
                override fun onMissingFileWarning() {
                }
            })
            .onResult()
            .subscribe({
                it?.getOrNull(0)?.path?.let { audioFilePath ->
                    isAudioRecordDone = true
                    val tempPath = Utils.getPathFromUri(audioFilePath)
                    val recordUpdatedPath = AppDirectory.getAudioSentFile(tempPath).absolutePath
                    AppDirectory.copy(tempPath, recordUpdatedPath)
                    filePath = recordUpdatedPath
                    audioAttachmentInit()
                }
            }, {
                it.printStackTrace()

            })
    }

    private fun audioAttachmentInit() {
        showPracticeSubmitLayout()
        binding.practiseSubmitLayout.visibility = VISIBLE
        binding.subPractiseSubmitLayout.visibility = VISIBLE
        binding.audioList.visibility = VISIBLE
        removePreviousAddedViewHolder()
        binding.audioList.addView(PracticeAudioViewHolder(null, context, filePath))
        initializePractiseSeekBar()
        enableSubmitButton()
    }

    private fun uploadTextFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, DOCX_FILE_MIME_TYPE)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        startActivityForResult(intent, TEXT_FILE_ATTACHMENT_REQUEST_CODE)
    }

    private fun recordPermission() {
        PermissionUtils.audioRecordStorageReadAndWritePermission(
            requireActivity(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            binding.uploadPractiseView.setOnClickListener(null)
                            audioRecordTouchListener()
                            return
                        }
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

    @SuppressLint("ClickableViewAccessibility")
    private fun audioRecordTouchListener() {
        binding.uploadPractiseView.setOnTouchListener { _, event ->
            if (PermissionUtils.isAudioAndStoragePermissionEnable(requireContext()).not()) {
                recordPermission()
                return@setOnTouchListener true
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.videoPlayer.onPause()

                    binding.rootView.requestDisallowInterceptTouchEvent(true)
                    binding.counterContainer.visibility = VISIBLE
                    binding.uploadPractiseView.startAnimation(scaleAnimation)
                    requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    appAnalytics?.addParam(AnalyticsEvent.AUDIO_RECORD.NAME, "Audio Recording")
                    //appAnalytics?.create(AnalyticsEvent.AUDIO_RECORD.NAME).push()
                    binding.counterTv.base = SystemClock.elapsedRealtime()
                    startTime = System.currentTimeMillis()
                    binding.counterTv.start()
                    val params =
                        binding.counterContainer.layoutParams as ViewGroup.MarginLayoutParams
//                    params.topMargin = binding.rootView.scrollY
                    practiceViewModel.startRecord()
                    binding.audioPractiseHint.visibility = GONE
                }
                MotionEvent.ACTION_MOVE -> {
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    binding.rootView.requestDisallowInterceptTouchEvent(false)
                    binding.counterTv.stop()
                    practiceViewModel.stopRecording()
                    binding.uploadPractiseView.clearAnimation()
                    binding.counterContainer.visibility = GONE
                    binding.audioPractiseHint.visibility = VISIBLE
                    requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                    val timeDifference =
                        TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - TimeUnit.MILLISECONDS.toSeconds(
                            startTime
                        )
                    if (timeDifference > 1) {
                        practiceViewModel.recordFile?.let {
                            isAudioRecordDone = true
                            filePath = AppDirectory.getAudioSentFile(null).absolutePath
                            AppDirectory.copy(it.absolutePath, filePath!!)
                            audioAttachmentInit()
                            Handler().postDelayed({
                                binding.submitAnswerBtn.parent.requestChildFocus(
                                    binding.submitAnswerBtn,
                                    binding.submitAnswerBtn
                                )
                            }, 200)
                        }


                    }
                }
            }

            true
        }
    }

    fun playPracticeAudio() {
        if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) <= 0) {
            StyleableToast.Builder(AppObjectController.joshApplication).gravity(Gravity.BOTTOM)
                .text(getString(R.string.volume_up_message)).cornerRadius(16)
                .length(Toast.LENGTH_LONG)
                .solidBackground().show()
        }
        appAnalytics?.addParam(AnalyticsEvent.PRACTICE_EXTRA.NAME, "Audio Played")

        if (coreJoshActivity?.currentAudio == null) {
            onPlayAudio(chatModel, chatModel.question?.audioList?.getOrNull(0)!!)
        } else {
            if (coreJoshActivity?.currentAudio == chatModel.question?.audioList?.getOrNull(0)?.audio_url) {
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

    fun removeAudioPractise() {
        /*filePath = null
        coreJoshActivity?.currentAudio = null
        hidePracticeSubmitLayout()
        binding.submitAudioViewContainer.visibility = GONE
        isAudioRecordDone = false
        binding.submitPractiseSeekbar.progress = 0
        binding.submitPractiseSeekbar.max = 0
        binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
        if (isAudioPlaying()) {
            audioManager?.resumeOrPause()
        }
        disableSubmitButton()
        appAnalytics?.addParam(AnalyticsEvent.PRACTICE_EXTRA.NAME, "Audio practise removed")*/

    }

    private fun initVideoPractise(path: String) {
        val videoSentFile = AppDirectory.videoSentFile()
        AppDirectory.copy(path, videoSentFile.absolutePath)
        filePath = videoSentFile.absolutePath
        isVideoRecordDone = true
        showPracticeSubmitLayout()
        binding.videoPlayerSubmit.init()
        binding.videoPlayerSubmit.visibility = VISIBLE
        binding.videoPlayerSubmit.setUrl(filePath)
        binding.videoPlayerSubmit.fitToScreen()
        binding.videoPlayerSubmit.downloadStreamButNotPlay()
        binding.videoPlayerSubmit.setPlayListener {
            VideoPlayerActivity.startVideoActivity(
                requireActivity(),
                null,
                null,
                filePath
            )
        }
        enableSubmitButton()
    }

    private fun checkIsPlayer(): Boolean {
        return audioManager != null
    }

    private fun isAudioPlaying(): Boolean {
        return this.checkIsPlayer() && this.audioManager!!.isPlaying()
    }

    private fun onPlayAudio(chatModel: ChatModel, audioObject: AudioType) {
        coreJoshActivity?.currentAudio = audioObject.audio_url
        val audioList = java.util.ArrayList<AudioType>()
        audioList.add(audioObject)
        audioManager = ExoAudioPlayer.getInstance()
        audioManager?.playerListener = this
        audioManager?.play(coreJoshActivity?.currentAudio!!)
        audioManager?.setProgressUpdateListener(this)
        if (filePath.isNullOrEmpty().not() && coreJoshActivity?.currentAudio == filePath) {

            val viewHolders = binding.audioList.allViewResolvers as List<*>
            viewHolders.forEach {
                if (it is PracticeAudioViewHolder) {
                    it.playPauseBtn.state = MaterialPlayPauseDrawable.State.Pause
                }
            }
        } else {
            binding.btnPlayInfo.state = MaterialPlayPauseDrawable.State.Pause
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        error.printStackTrace()
    }

    fun openAttachmentFile() {
        filePath?.let {
            Utils.openFile(requireActivity(), it)
        }
    }

    fun removeFileAttachment() {
        filePath = null
        isDocumentAttachDone = false
        showPracticeInputLayout()
        hidePracticeSubmitLayout()
        binding.submitFileViewContainer.visibility = GONE
        binding.fileInfoAttachmentTv.text = EMPTY
        disableSubmitButton()
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

    fun improvePractice() {
        if (chatModel.question != null && chatModel.question!!.expectedEngageType != null) {
            val engageType = chatModel.question?.expectedEngageType
            chatModel.question?.expectedEngageType?.let {
                if (EXPECTED_ENGAGE_TYPE.AU == it) {
                    showPracticeInputLayout()
                    setViewAccordingExpectedAnswer()
                    hideImproveButton()
                    disableSubmitButton()
                    return
                } else {
                    return
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CapsuleActivityCallback)
            activityCallback = context
    }

    fun submitPractise() {
        if (chatModel.question != null && chatModel.question!!.expectedEngageType != null) {
            val engageType = chatModel.question?.expectedEngageType
            chatModel.question?.expectedEngageType?.let {
                if (EXPECTED_ENGAGE_TYPE.TX == it && binding.etPractise.text.isNullOrEmpty()) {
                    showToast(getString(R.string.submit_practise_msz))
                    return
                } else if (EXPECTED_ENGAGE_TYPE.AU == it && isAudioRecordDone.not()) {
                    showToast(getString(R.string.submit_practise_msz))
                    return
                } else if (EXPECTED_ENGAGE_TYPE.VI == it && isVideoRecordDone.not()) {
                    showToast(getString(R.string.submit_practise_msz))
                    return
                } else if (EXPECTED_ENGAGE_TYPE.DX == it && isDocumentAttachDone.not()) {
                    showToast(getString(R.string.submit_practise_msz))
                    return
                }

                appAnalytics?.addParam(
                    AnalyticsEvent.PRACTICE_SCREEN_TIME.NAME,
                    System.currentTimeMillis() - totalTimeSpend
                )
                appAnalytics?.addParam(AnalyticsEvent.PRACTICE_SOLVED.NAME, true)
                appAnalytics?.addParam(AnalyticsEvent.PRACTICE_STATUS.NAME, "Submitted")
                appAnalytics?.addParam(
                    AnalyticsEvent.PRACTICE_TYPE_SUBMITTED.NAME,
                    "$it Practice Submitted"
                )
                appAnalytics?.addParam(AnalyticsEvent.PRACTICE_SUBMITTED.NAME, "Submit Practice $")

                val requestEngage = RequestEngage()
                requestEngage.text = binding.etPractise.text.toString()
                requestEngage.localPath = filePath
                requestEngage.duration =
                    Utils.getDurationOfMedia(requireActivity(), filePath)?.toInt()
                requestEngage.feedbackRequire = chatModel.question?.feedback_require
                requestEngage.question = chatModel.question?.questionId!!
                requestEngage.mentor = Mentor.getInstance().getId()
                if (it == EXPECTED_ENGAGE_TYPE.AU || it == EXPECTED_ENGAGE_TYPE.VI || it == EXPECTED_ENGAGE_TYPE.DX) {
                    requestEngage.answerUrl = filePath
                }
                //binding.progressLayout.visibility = INVISIBLE
                binding.feedbackLayout.visibility = GONE
                binding.progressLayout.visibility = VISIBLE
                binding.feedbackGrade.visibility = GONE
                binding.feedbackDescription.visibility = GONE
                disableSubmitButton()
                practiceViewModel.submitPractise(chatModel, requestEngage, engageType)
            }
        }
    }

    fun onReadingContinueClick() {
        activityCallback?.onNextTabCall(binding.continueBtn)
    }

/*

    override fun onBackPressed() {
        super.onBackPressed()
        requireActivity().finishAndRemoveTask()
    }
*/

    override fun onPlayerPause() {
        //binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
    }

    override fun onPlayerResume() {
        //binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Pause
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
        // binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
        //binding.submitPractiseSeekbar.progress = 0
        audioManager?.seekTo(0)
        audioManager?.onPause()
        audioManager?.setProgressUpdateListener(null)
    }

    override fun onProgressUpdate(progress: Long) {
        // binding.submitPractiseSeekbar.progress = progress.toInt()
    }

    override fun onDurationUpdate(duration: Long?) {
        //  duration?.toInt()?.let { binding.submitPractiseSeekbar.max = it }
    }

}
