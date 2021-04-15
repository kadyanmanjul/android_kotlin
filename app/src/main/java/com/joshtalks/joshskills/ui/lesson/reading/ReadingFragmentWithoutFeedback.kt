package com.joshtalks.joshskills.ui.lesson.reading

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.SystemClock
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
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.material.snackbar.Snackbar
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
import com.joshtalks.joshskills.core.extension.setImageAndFitCenter
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.isCallOngoing
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ReadingPracticeFragmentWithoutFeedbackBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.EXPECTED_ENGAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.LessonMaterialType
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion
import com.joshtalks.joshskills.repository.local.entity.PendingTask
import com.joshtalks.joshskills.repository.local.entity.PracticeEngagement
import com.joshtalks.joshskills.repository.local.entity.PracticeFeedback2
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.RemovePracticeAudioEventBus
import com.joshtalks.joshskills.repository.local.eventbus.SnackBarEvent
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.extra.ImageShowFragment
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import com.joshtalks.joshskills.ui.pdfviewer.CURRENT_VIDEO_PROGRESS_POSITION
import com.joshtalks.joshskills.ui.pdfviewer.PdfViewerActivity
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
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable
import timber.log.Timber

class ReadingFragmentWithoutFeedback :
    CoreJoshFragment(),
    Player.EventListener,
    AudioPlayerEventListener,
    ProgressUpdateListener {

    private var compositeDisposable = CompositeDisposable()

    private lateinit var binding: ReadingPracticeFragmentWithoutFeedbackBinding
    private var mUserIsSeeking = false
    private var isAudioRecordDone = false
    private var scaleAnimation: Animation? = null
    private var startTime: Long = 0
    private var totalTimeSpend: Long = 0
    private var filePath: String? = null
    private var appAnalytics: AppAnalytics? = null
    private var audioManager: ExoAudioPlayer? = null
    private var currentLessonQuestion: LessonQuestion? = null
    var lessonActivityListener: LessonActivityListener? = null

    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(requireActivity()).get(LessonViewModel::class.java)
    }

    var openVideoPlayerActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getLongExtra(
                CURRENT_VIDEO_PROGRESS_POSITION,
                0
            )?.let { progress ->
                binding.videoPlayer.setProgress(progress)
                binding.videoPlayer.onResume()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LessonActivityListener)
            lessonActivityListener = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        totalTimeSpend = System.currentTimeMillis()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.reading_practice_fragment_without_feedback,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        binding.rootView.layoutTransition?.setAnimateParentHierarchy(false)
        scaleAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.scale)

        addObserver()

        return binding.rootView
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
            if (isVisible) {
                // binding.videoPlayer.onResume()
            } else {
                // binding.videoPlayer.onPause()
                if (audioManager != null) {
                    audioManager?.onPause()
                }
            }
        } catch (ex: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        binding.videoPlayer.onPause()
        pauseAllAudioAndUpdateViews()
    }

    private fun pauseAllAudioAndUpdateViews() {
        try {
            if (audioManager != null) {
                audioManager?.onPause()
                binding.btnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
            }
            pauseAllViewHolderAudio()
        } catch (ex: Exception) {
            Timber.d(ex)
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
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
            try {
                binding.videoPlayer.onStop()
            } catch (ex: Exception) {
            }

            audioManager?.release()
        } catch (ex: Exception) {
        }
    }

    fun hidePracticeInputLayout() {
        binding.practiseInputHeader.visibility = GONE
        binding.practiseInputLabel.visibility = GONE
        binding.practiseInputLayout.visibility = GONE
    }

    fun showPracticeInputLayout() {
        binding.practiseInputHeader.visibility = VISIBLE
        binding.practiseInputLabel.visibility = VISIBLE
        binding.practiseInputLayout.visibility = VISIBLE
    }

    fun showPracticeSubmitLayout() {
        binding.yourSubAnswerTv.visibility = VISIBLE
        binding.subPractiseSubmitLayout.visibility = VISIBLE
    }

    fun hidePracticeSubmitLayout() {
        //binding.yourSubAnswerTv.visibility = GONE
        binding.subPractiseSubmitLayout.visibility = GONE
    }

    fun showImproveButton() {
        // binding.feedbackLayout.visibility = VISIBLE
        // binding.improveAnswerBtn.visibility = VISIBLE
        binding.continueBtn.visibility = VISIBLE
        binding.submitAnswerBtn.visibility = GONE
    }

    fun hideImproveButton() {
        binding.feedbackLayout.visibility = GONE
        binding.improveAnswerBtn.visibility = GONE
        binding.submitAnswerBtn.visibility = VISIBLE
        binding.continueBtn.visibility = GONE
    }

    private fun pauseAllViewHolderAudio() {
        val viewHolders = binding.audioList.allViewResolvers as List<PracticeAudioViewHolder>
        viewHolders.forEach {
            it.pauseAudio()
        }
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listen(RemovePracticeAudioEventBus::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe(
                    {
                        AppObjectController.uiHandler.post {
                            binding.audioList.removeView(it.practiceAudioViewHolder)
                            currentLessonQuestion?.run {
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
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(SnackBarEvent::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe(
                    {
                        showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, it.pointsSnackBarText)
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
    }

    private fun setPracticeInfoView() {
        currentLessonQuestion?.run {
            appAnalytics?.addParam(
                AnalyticsEvent.PRACTICE_TYPE_PRESENT.NAME,
                "${this.materialType} Practice present"
            )
            initializeAudioViewForDemoAudio(this)
            when (this.materialType) {
                LessonMaterialType.AU -> {
                    // initializeAudioViewForDemoAudio(this)
                }

                LessonMaterialType.IM -> {
                    binding.imageView.visibility = VISIBLE
                    this.imageList?.getOrNull(0)?.imageUrl?.let { path ->
                        binding.imageView.setImageAndFitCenter(path, context)
                        binding.imageView.setOnClickListener {
                            ImageShowFragment.newInstance(path, "", "")
                                .show(childFragmentManager, "ImageShow")
                        }
                    }
                }
                LessonMaterialType.VI -> {
                    binding.videoPlayer.visibility = VISIBLE
                    this.videoList?.getOrNull(0)?.let { video ->
                        video.video_url?.let {
                            setVideoThumbnail(video.video_image_url)
                            binding.videoPlayer.setUrl(it)
                            binding.videoPlayer.fitToScreen()
                            binding.videoPlayer.setPlayListener {
                                val videoId = this.videoList?.getOrNull(0)?.id
                                val videoUrl = this.videoList?.getOrNull(0)?.video_url
                                val currentVideoProgressPosition = binding.videoPlayer.getProgress()
                                openVideoPlayerActivity.launch(
                                    VideoPlayerActivity.getActivityIntent(
                                        requireContext(),
                                        "",
                                        videoId,
                                        videoUrl,
                                        currentVideoProgressPosition,
                                        conversationId = getConversationId()
                                    )
                                )
                            }
                            binding.videoPlayer.downloadStreamButNotPlay()
                        }
                    }
                }
                LessonMaterialType.PD -> {
                    binding.imageView.visibility = VISIBLE
                    binding.imageView.setImageResource(R.drawable.ic_practise_pdf_ph)
                    this.pdfList?.getOrNull(0)?.let { pdfType ->
                        binding.imageView.setOnClickListener {
                            PdfViewerActivity.startPdfActivity(
                                requireActivity(),
                                pdfType.id,
                                EMPTY,
                                conversationId = requireActivity().intent.getStringExtra(
                                    CONVERSATION_ID
                                )
                            )
                        }
                    }
                }
                LessonMaterialType.TX -> {
                    this.qText?.let {
                        binding.infoTv.visibility = VISIBLE
                        binding.infoTv.text =
                            HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    }
                }
                else -> {
                }
            }
            if ((this.materialType == LessonMaterialType.TX).not()) {
                if (this.qText.isNullOrEmpty().not()) {
                    binding.practiseTextInfoLayout.visibility = GONE
                    binding.infoTv2.visibility = VISIBLE
                    binding.infoTv2.text =
                        HtmlCompat.fromHtml(this.qText!!, HtmlCompat.FROM_HTML_MODE_LEGACY)
                }
            }
        }
    }

    private fun initializeAudioViewForDemoAudio(lessonQuestion: LessonQuestion) {
        lessonQuestion.audioList?.getOrNull(0)?.audio_url?.let {
            binding.audioViewContainer.visibility = VISIBLE
            binding.btnPlayInfo.tag = it
            binding.practiseSeekbar.max = lessonQuestion.audioList?.getOrNull(0)?.duration!!
            if (binding.practiseSeekbar.max == 0) {
                binding.practiseSeekbar.max = 2_00_000
            }
        }
        initializePractiseSeekBar()
    }

    private fun setVideoThumbnail(thumbnailUrl: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            val thumbnailDrawable: Drawable? =
                Utils.getDrawableFromUrl(thumbnailUrl)
            if (thumbnailDrawable != null) {
                AppObjectController.uiHandler.post {
                    binding.videoPlayer.useArtwork = true
                    binding.videoPlayer.defaultArtwork = thumbnailDrawable
//                    val imgArtwork: ImageView = binding.videoPlayer.findViewById(R.id.exo_artwork) as ImageView
//                    imgArtwork.setImageDrawable(thumbnailDrawable)
//                    imgArtwork.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setViewAccordingExpectedAnswer() {
        currentLessonQuestion?.run {
            showPracticeInputLayout()
            binding.recordingViewFrame.visibility = VISIBLE
            binding.hintContainer.visibility = VISIBLE
            binding.practiseInputHeader.text =
                AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.READING_PRACTICE_TITLE)
            binding.recordingView.setImageResource(R.drawable.recv_ic_mic_white)
            audioRecordTouchListener()
            binding.audioPractiseHint.visibility = VISIBLE
        }
    }

    private fun addObserver() {

        viewModel.lessonQuestionsLiveData.observe(
            viewLifecycleOwner,
            {
                currentLessonQuestion = it.filter { it.chatType == CHAT_TYPE.RP }.getOrNull(0)

                currentLessonQuestion?.run {

                    appAnalytics = AppAnalytics.create(AnalyticsEvent.PRACTICE_SCREEN.NAME)
                        .addBasicParam()
                        .addUserDetails()
                        .addParam("lesson_id", this.lessonId)
                        .addParam("question_id", this.id)

                    if (this.practiceEngagement.isNullOrEmpty()) {
                        binding.submitAnswerBtn.visibility = VISIBLE
                        appAnalytics?.addParam(AnalyticsEvent.PRACTICE_SOLVED.NAME, false)
                        appAnalytics?.addParam(AnalyticsEvent.PRACTICE_STATUS.NAME, "Not Submitted")
                        setViewAccordingExpectedAnswer()
                    } else {
                        binding.submitAnswerBtn.visibility = GONE
                        // binding.improveAnswerBtn.visibility = VISIBLE
                        binding.continueBtn.visibility = VISIBLE
                        appAnalytics?.addParam(AnalyticsEvent.PRACTICE_SOLVED.NAME, true)
                        appAnalytics?.addParam(
                            AnalyticsEvent.PRACTICE_STATUS.NAME,
                            "Already Submitted"
                        )
                        setViewUserSubmitAnswer()
                    }
                }

                setPracticeInfoView()
            }
        )

        viewModel.requestStatusLiveData.observe(
            viewLifecycleOwner,
            {
                if (it) {
                    showCompletedPractise()
                } else {
                    enableSubmitButton()
                    binding.progressLayout.visibility = GONE
                }
            }
        )

        viewModel.practiceFeedback2LiveData.observe(
            viewLifecycleOwner,
            {
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
                // binding.improveAnswerBtn.visibility = VISIBLE
                binding.continueBtn.visibility = VISIBLE
            }
        )

        viewModel.practiceEngagementData.observe(
            viewLifecycleOwner,
            {
                updatePracticeFeedback(it)
                if (it.pointsList.isNullOrEmpty().not()) {
                    showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, it.pointsList?.get(0))
                }
            }
        )
    }

    private fun showCompletedPractise() {
        hidePracticeInputLayout()
        binding.submitAnswerBtn.visibility = GONE
        binding.progressLayout.visibility = GONE
        // binding.feedbackResultProgressLl.visibility = VISIBLE
        binding.rootView.postDelayed(
            {
                binding.rootView.smoothScrollTo(
                    0,
                    binding.rootView.height
                )
            },
            100
        )

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
        // binding.improveAnswerBtn.visibility = VISIBLE
        binding.continueBtn.visibility = VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            lessonActivityListener?.onQuestionStatusUpdate(
                QUESTION_STATUS.AT,
                currentLessonQuestion?.id
            )
            lessonActivityListener?.onSectionStatusUpdate(2, true)
        }
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
        viewHolders.forEach {
            it.hideCancelButtons()
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
        // binding.feedbackLayout.visibility = VISIBLE
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
        currentLessonQuestion?.run {
            hidePracticeInputLayout()
            showPracticeSubmitLayout()
            binding.yourSubAnswerTv.visibility = VISIBLE
            val params: ViewGroup.MarginLayoutParams =
                binding.subPractiseSubmitLayout.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = Utils.dpToPx(20)
            binding.subPractiseSubmitLayout.layoutParams = params
            binding.yourSubAnswerTv.text = getString(R.string.your_submitted_answer)
            val practiseEngagement = this.practiceEngagement?.get(0)
            initRV()
            addAudioListRV(this.practiceEngagement)
            filePath = practiseEngagement?.answerUrl

            // initializePractiseSeekBar()
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
        binding.yourSubAnswerTv.visibility = VISIBLE
        if (practiceEngagement.isNullOrEmpty().not())
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
                        ) {
                            binding.btnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
                        }
                    )
                    if (practice.practiceFeedback != null) {
                        // binding.feedbackLayout.visibility = VISIBLE
                        // binding.feedbackGrade.text = practice.practiceFeedback!!.grade
                        // binding.feedbackDescription.text = practice.practiceFeedback!!.text
                    }
                }
            }
        }
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
                    // it.setSeekToZero()
                }
            }
        }
    }

    private fun audioAttachmentInit() {
        showPracticeSubmitLayout()
        binding.practiseSubmitLayout.visibility = VISIBLE
        binding.subPractiseSubmitLayout.visibility = VISIBLE
        binding.audioList.visibility = VISIBLE
        removePreviousAddedViewHolder()
        binding.audioList.addView(
            PracticeAudioViewHolder(null, context, filePath) {
                binding.btnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
            }
        )
        initializePractiseSeekBar()
        enableSubmitButton()
    }

    private fun recordPermission() {
        PermissionUtils.audioRecordStorageReadAndWritePermission(
            requireActivity(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            binding.recordingView.setOnClickListener(null)
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
            }
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun audioRecordTouchListener() {
        binding.recordingView.setOnTouchListener { _, event ->
            if (isCallOngoing()) {
                return@setOnTouchListener false
            }
            if (PermissionUtils.isAudioAndStoragePermissionEnable(requireContext()).not()) {
                recordPermission()
                return@setOnTouchListener true
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isAudioRecording = true
                    binding.videoPlayer.onPause()
                    pauseAllAudioAndUpdateViews()
                    binding.rootView.requestDisallowInterceptTouchEvent(true)
                    binding.counterTv.visibility = VISIBLE
                    binding.recordingViewFrame.layoutTransition?.setAnimateParentHierarchy(false)
                    binding.recordingViewFrame.startAnimation(scaleAnimation)
                    binding.recordingViewFrame.layoutTransition?.setAnimateParentHierarchy(false)
                    requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    appAnalytics?.addParam(AnalyticsEvent.AUDIO_RECORD.NAME, "Audio Recording")
                    // appAnalytics?.create(AnalyticsEvent.AUDIO_RECORD.NAME).push()
                    binding.counterTv.base = SystemClock.elapsedRealtime()
                    startTime = System.currentTimeMillis()
                    binding.counterTv.start()
                    val params =
                        binding.counterTv.layoutParams as ViewGroup.MarginLayoutParams
//                    params.topMargin = binding.rootView.scrollY
                    viewModel.startRecord()
                    binding.audioPractiseHint.visibility = GONE
                }
                MotionEvent.ACTION_MOVE -> {
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isAudioRecording = false
                    binding.rootView.requestDisallowInterceptTouchEvent(false)
                    binding.counterTv.stop()
                    viewModel.stopRecording()
                    binding.recordingViewFrame.clearAnimation()
                    binding.counterTv.visibility = GONE
                    binding.audioPractiseHint.visibility = VISIBLE
                    requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    val timeDifference =
                        TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - TimeUnit.MILLISECONDS.toSeconds(
                            startTime
                        )
                    if (timeDifference > 1) {
                        viewModel.recordFile?.let {
                            isAudioRecordDone = true
                            filePath = AppDirectory.getAudioSentFile(null).absolutePath
                            AppDirectory.copy(it.absolutePath, filePath!!)
                            audioAttachmentInit()
                            AppObjectController.uiHandler.postDelayed(
                                {
                                    binding.submitAnswerBtn.parent.requestChildFocus(
                                        binding.submitAnswerBtn,
                                        binding.submitAnswerBtn
                                    )
                                },
                                200
                            )
                        }
                    }
                }
            }
            true
        }
    }

    fun playPracticeAudio() {
        if (isAudioRecording.not()) {
            if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) <= 0) {
                StyleableToast.Builder(AppObjectController.joshApplication).gravity(Gravity.BOTTOM)
                    .text(getString(R.string.volume_up_message)).cornerRadius(16)
                    .length(Toast.LENGTH_LONG)
                    .solidBackground().show()
            }
            appAnalytics?.addParam(AnalyticsEvent.PRACTICE_EXTRA.NAME, "Audio Played")

            if (audioManager?.currentPlayingUrl != null &&
                audioManager?.currentPlayingUrl == currentLessonQuestion?.audioList?.getOrNull(0)?.audio_url
            ) {
                audioManager?.setProgressUpdateListener(this)
                audioManager?.resumeOrPause()
            } else {
                onPlayAudio(
                    currentLessonQuestion?.audioList?.getOrNull(0)!!
                )
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

    private fun checkIsPlayer(): Boolean {
        return audioManager != null
    }

    private fun isAudioPlaying(): Boolean {
        return this.checkIsPlayer() && this.audioManager!!.isPlaying()
    }

    private fun onPlayAudio(audioObject: AudioType) {
        coreJoshActivity?.currentAudio = audioObject.audio_url
        val audioList = ArrayList<AudioType>()
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
                    binding.btnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
                }
            }
        } else {
            val viewHolders = binding.audioList.allViewResolvers as List<*>
            viewHolders.forEach {
                if (it is PracticeAudioViewHolder) {
                    it.playPauseBtn.state = MaterialPlayPauseDrawable.State.Play
                }
            }
            binding.btnPlayInfo.state = MaterialPlayPauseDrawable.State.Pause
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        error.printStackTrace()
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
        if (currentLessonQuestion?.expectedEngageType != null) {
            currentLessonQuestion?.expectedEngageType?.let {
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

    fun submitPractise() {
        CoroutineScope(Dispatchers.IO).launch {
            currentLessonQuestion?.expectedEngageType?.let {
                if (EXPECTED_ENGAGE_TYPE.AU == it && isAudioRecordDone.not()) {
                    showToast(getString(R.string.submit_practise_msz))
                } else {
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
                    appAnalytics?.addParam(
                        AnalyticsEvent.PRACTICE_SUBMITTED.NAME,
                        "Submit Practice $"
                    )

                    val requestEngage = RequestEngage()
                    requestEngage.localPath = filePath
                    requestEngage.duration =
                        Utils.getDurationOfMedia(requireActivity(), filePath)?.toInt()
                    // requestEngage.feedbackRequire = currentLessonQuestion.feedback_require
                    requestEngage.questionId = currentLessonQuestion!!.id
                    requestEngage.mentor = Mentor.getInstance().getId()
                    if (it == EXPECTED_ENGAGE_TYPE.AU) {
                        requestEngage.answerUrl = filePath
                    }
                    AppObjectController.uiHandler.post {
                        // binding.progressLayout.visibility = INVISIBLE
                        binding.feedbackLayout.visibility = GONE
                        binding.progressLayout.visibility = VISIBLE
                        binding.feedbackGrade.visibility = GONE
                        binding.feedbackDescription.visibility = GONE
                        binding.recordingViewFrame.visibility = GONE
                        binding.hintContainer.visibility = GONE
                        disableSubmitButton()
                    }
                    // practiceViewModel.submitPractise(chatModel, requestEngage, engageType)
                    viewModel.getPointsForVocabAndReading(currentLessonQuestion!!.id)
                    viewModel.addTaskToService(requestEngage, PendingTask.READING_PRACTICE_OLD)
                    currentLessonQuestion!!.status = QUESTION_STATUS.IP
                    delay(300)
                    AppObjectController.uiHandler.post {
                        showCompletedPractise()
                    }
                }
            }
        }
    }

    fun onReadingContinueClick() {
        lessonActivityListener?.onNextTabCall(2)
    }

/*

    override fun onBackPressed() {
        super.onBackPressed()
        requireActivity().finishAndRemoveTask()
    }
*/

    override fun onPlayerPause() {
        binding.btnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
    }

    override fun onPlayerResume() {
        binding.btnPlayInfo.state = MaterialPlayPauseDrawable.State.Pause
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
        binding.btnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
        binding.practiseSeekbar.progress = 0
        audioManager?.seekTo(0)
        audioManager?.onPause()
        audioManager?.setProgressUpdateListener(null)
    }

    override fun onProgressUpdate(progress: Long) {
        binding.practiseSeekbar.progress = progress.toInt()
    }

    override fun onDurationUpdate(duration: Long?) {
        duration?.toInt()?.let { binding.practiseSeekbar.max = it }
    }

    companion object {
        var isAudioRecording = false
        private const val IMAGE_OR_VIDEO_SELECT_REQUEST_CODE = 1081
        private const val TEXT_FILE_ATTACHMENT_REQUEST_CODE = 1082
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

        @JvmStatic
        fun getInstance() = ReadingFragmentWithoutFeedback()
    }
}
