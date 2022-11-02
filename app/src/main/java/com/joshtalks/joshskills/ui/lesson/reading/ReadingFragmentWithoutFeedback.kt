package com.joshtalks.joshskills.ui.lesson.reading

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.*
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.get
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.base.getAndroidDownloadFolder
import com.joshtalks.joshskills.base.getVideoFilePath
import com.joshtalks.joshskills.base.saveVideoQ
import com.joshtalks.joshskills.constants.*
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.JoshVideoPlayer
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.extension.setImageAndFitCenter
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.core.videotranscoder.enforceSingleScrollDirection
import com.joshtalks.joshskills.databinding.ReadingPracticeFragmentWithoutFeedbackBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.eventbus.RemovePracticeAudioEventBus
import com.joshtalks.joshskills.repository.local.eventbus.SnackBarEvent
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.PurchasePopupType
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.chat.DEFAULT_TOOLTIP_DELAY_IN_MS
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import com.joshtalks.joshskills.ui.lesson.READING_POSITION
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
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2core.DownloadBlock
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable
import timber.log.Timber
import zeroonezero.android.audio_mixer.AudioMixer
import zeroonezero.android.audio_mixer.input.GeneralAudioInput
import java.io.File
import java.io.IOException
import java.lang.Runnable
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

private const val TAG = "ReadingFragmentWithoutFeedback"

class ReadingFragmentWithoutFeedback :
    CoreJoshFragment(),
    Player.EventListener,
    AudioPlayerEventListener,
    ProgressUpdateListener {

    private val CLICK_OFFSET_PERIOD = 300L
    private var compositeDisposable = CompositeDisposable()

    private lateinit var binding: ReadingPracticeFragmentWithoutFeedbackBinding
    private val events = EventLiveData
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
    private var video: String? = null
    private var videoDownPath: String? = null
    private var outputFile: String = EMPTY
    private var downloadID: Long = -1
    lateinit var fileName: String
    private var fileDir: String = EMPTY
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex(false)
    private var muxerJob: Job? = null
    private var internetAvailableFlag: Boolean = true
    private val praticAudioAdapter: PracticeAudioAdapter by lazy { PracticeAudioAdapter(context) }
    private var linearLayoutManager: LinearLayoutManager? = null
    private var onDownloadCompleteListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID == id) {
                fileDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + File.separator + fileName
                scope.launch {
                    AppObjectController.appDatabase.chatDao().updateReadingTable(
                        currentLessonQuestion!!.questionId,
                        fileDir,
                        true
                    )
                }
                setVideoPlayerWuthUrl(fileDir)
            }
        }
    }

    private fun setVideoPlayerWuthUrl(fileDir: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.mergedVideo.setUrl(fileDir)
            // binding.videoPlayer.setCourseId(course_id)
            binding.mergedVideo.fitToScreen()
            binding.mergedVideo.onStart()
            binding.mergedVideo.downloadStreamButNotPlay()
            binding.mergedVideo.setClickListners()
            binding.mergedVideo.setControllerButtonCallback(object : JoshVideoPlayer.ControllerButtonCallback {
                override fun onPlay() {
                    playVideoEvent()
                }

                override fun onWatchAgain() {
                    playVideoEvent()
                }

            })
        }
    }

    private val pauseAnimationCallback by lazy {
        Runnable {
            if (audioManager?.isPlaying() == false)
                showRecordHintAnimation()
        }
    }

    private val longPressAnimationCallback by lazy {
        Runnable {
            hideRecordHindAnimation()
        }
    }

    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(requireActivity())[LessonViewModel::class.java]
    }

    private val progressAnimator by lazy {
        ValueAnimator.ofInt(0, 60).apply {
            duration = 1850
            addUpdateListener {
                binding.progressAnimation.progress = it.animatedValue as Int
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    binding.recordingView.scaleX = 0.95f
                    binding.recordingView.scaleY = 0.95f
                    binding.recordingView.backgroundTintList = ContextCompat.getColorStateList(
                        AppObjectController.joshApplication,
                        R.color.highlight_btn_color
                    )
                }

                override fun onAnimationEnd(animation: Animator) {
                    binding.progressAnimation.progress = 0
                    binding.recordingView.scaleX = 1f
                    binding.recordingView.scaleY = 1f
                    binding.recordingView.backgroundTintList = ContextCompat.getColorStateList(
                        AppObjectController.joshApplication,
                        R.color.button_color
                    )
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {}

            })
        }
    }

    private var currentTooltipIndex = 0
    private val lessonTooltipList by lazy {
        listOf(
            "हम यहां अपने पढ़ने और उच्चारण में सुधार करेंगे",
            "और धीरे धीरे हम native speaker की तरह बोलना सीखेंगे"
        )
    }

    private var lessonID = -1

    var openVideoPlayerActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getLongExtra(
                CURRENT_VIDEO_PROGRESS_POSITION,
                0
            )?.let { progress ->
                binding.videoPlayer.progress = progress
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
        linearLayoutManager = LinearLayoutManager(requireActivity())
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
        binding.rootView.layoutTransition?.setAnimateParentHierarchy(false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scaleAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.scale)
        addObserver()
        if (PrefManager.hasKey(HAS_SEEN_READING_PLAY_ANIMATION).not() || PrefManager.getBoolValue(
                HAS_SEEN_READING_PLAY_ANIMATION
            ).not()
        ) {
            binding.playInfoHint.visibility = VISIBLE
        }
        events.observe(viewLifecycleOwner) {
            when (it.what) {
                PERMISSION_FROM_READING_GRANTED -> download()
                SHARE_VIDEO -> inviteFriends(it.obj as Intent)
                SUBMIT_BUTTON_CLICK -> submitPractise()
                CANCEL_BUTTON_CLICK -> closeRecordedView()
                SHOW_VIDEO_VIEW -> binding.practiseSubmitLayout.visibility = VISIBLE
                VIDEO_AUDIO_MERGED_PATH -> {
                    binding.progressDialog.visibility = GONE
                    outputFile = it.obj as String
                    viewModel.sendOutputToFullScreen(outputFile)
                    setVideoPlayerWuthUrl(outputFile)
                }
                INCREASE_AUDIO_VOLUME -> {

                }
                VIDEO_AUDIO_MUX_FAILED -> {
                    muxVideoOldMethod(filePath!!)
                }
            }
        }
        binding.videoLayout.clipToOutline = true
    }

    override fun onResume() {
        super.onResume()
        showRecordHintAnimation()
        subscribeRXBus()
        if (PrefManager.hasKey(HAS_SEEN_READING_SCREEN).not()) {
            PrefManager.put(HAS_SEEN_READING_SCREEN, true)
        }
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
        binding.mergedVideo.onPause()
        pauseAllAudioAndUpdateViews()
    }

    private fun getPermissionAndDownloadVideo(url: String) {
        PermissionUtils.storageReadAndWritePermission(requireContext(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            downloadDigitalCopy(url)
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            if (isAdded && activity != null)
                                PermissionUtils.permissionPermanentlyDeniedDialog(requireActivity())
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

    private fun downloadDigitalCopy(url: String) {
        registerDownloadReceiver()
        fileName = Utils.getFileNameFromURL(url)

        val request: DownloadManager.Request =
            DownloadManager.Request(Uri.parse(url))
                .setTitle(getString(R.string.app_name))
                .setDescription("Downloading video")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            request.setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
        }

        val downloadManager =
            AppObjectController.joshApplication.getSystemService(Context.DOWNLOAD_SERVICE) as (DownloadManager)
        downloadID = downloadManager.enqueue(request)
    }

    private fun registerDownloadReceiver() {
        AppObjectController.joshApplication.registerReceiver(
            onDownloadCompleteListener,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }


    private fun showRecordHintAnimation() {
        if (PrefManager.getBoolValue(
                HAS_SEEN_READING_PLAY_ANIMATION
            ) && (PrefManager.hasKey(HAS_SEEN_READING_HAND_TOOLTIP)
                .not() || PrefManager.getBoolValue(
                HAS_SEEN_READING_HAND_TOOLTIP
            ).not())
        ) {
            binding.blackFrameContainer.visibility = VISIBLE
            binding.practiseInfoContainer.setBackgroundColor(Color.parseColor("#88000000"))
            binding.readingHoldHint.visibility = VISIBLE
            binding.progressAnimation.visibility = VISIBLE
            var isChildAnimationStared = false
            binding.readingHoldHint.addAnimatorUpdateListener {
                val startAnimation = 0.033 * 4
                val currentValue = it.animatedFraction
                if (startAnimation <= currentValue && !isChildAnimationStared) {
                    isChildAnimationStared = true
                    startProgressAnimation()
                }
            }
            binding.readingHoldHint.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {}

                override fun onAnimationCancel(animation: Animator) {
                    progressAnimator.cancel()
                }

                override fun onAnimationRepeat(animation: Animator) {
                    isChildAnimationStared = false
                }

            })
            binding.readingHoldHint.cancelAnimation()
            binding.readingHoldHint.playAnimation()
        }
    }

    private fun startProgressAnimation() {
        progressAnimator.start()
    }

    private fun hideRecordHindAnimation() {
        if (PrefManager.hasKey(HAS_SEEN_READING_HAND_TOOLTIP).not() || PrefManager.getBoolValue(
                HAS_SEEN_READING_HAND_TOOLTIP
            ).not()
        ) {
            PrefManager.put(HAS_SEEN_READING_HAND_TOOLTIP, value = true)
            binding.blackFrameContainer.visibility = GONE
            binding.practiseInfoContainer.setBackgroundColor(Color.parseColor("#FFFFFF"))
            binding.readingHoldHint.visibility = GONE
            binding.readingHoldHint.cancelAnimation()
            binding.progressAnimation.visibility = GONE
            binding.recordingView.scaleX = 1f
            binding.recordingView.scaleY = 1f
            binding.recordingView.backgroundTintList = ContextCompat.getColorStateList(
                AppObjectController.joshApplication,
                R.color.button_color
            )
        }
    }

    private fun pauseAllAudioAndUpdateViews() {
        try {
            if (audioManager != null) {
                audioManager?.onPause()
                onPlayerPause()
//                binding.btnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
            }
            binding.videoPlayer.onPause()
            binding.mergedVideo.onPause()
            pauseAllViewHolderAudio()
        } catch (ex: Exception) {
            Timber.d(ex)
        }
    }

    override fun onStop() {
        appAnalytics?.push()
        super.onStop()
        binding.mergedVideo.onPause()
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
                scope.cancel()
                muxerJob?.cancel()
            } catch (ex: Exception) {
            }

            audioManager?.release()
        } catch (ex: Exception) {
        }
    }

    private fun showTooltip() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (PrefManager.getBoolValue(HAS_SEEN_READING_TOOLTIP, defValue = false)) {
                withContext(Dispatchers.Main) {
                    binding.lessonTooltipLayout.visibility = GONE
                }
            } else {
                delay(DEFAULT_TOOLTIP_DELAY_IN_MS)
                if (viewModel.lessonLiveData.value?.lessonNo == 1) {
                    withContext(Dispatchers.Main) {
                        binding.joshTextView.text = lessonTooltipList[currentTooltipIndex]
                        binding.txtTooltipIndex.text =
                            "${currentTooltipIndex + 1} of ${lessonTooltipList.size}"
                        binding.lessonTooltipLayout.visibility = VISIBLE
                    }
                }
            }
        }
    }

    private fun showNextTooltip() {
        if (currentTooltipIndex < lessonTooltipList.size - 1) {
            currentTooltipIndex++
            binding.joshTextView.text = lessonTooltipList[currentTooltipIndex]
            binding.txtTooltipIndex.text =
                "${currentTooltipIndex + 1} of ${lessonTooltipList.size}"
        } else {
            binding.lessonTooltipLayout.visibility = GONE
            PrefManager.put(HAS_SEEN_READING_TOOLTIP, true)
        }
    }

    fun hideTooltip() {
        binding.lessonTooltipLayout.visibility = GONE
        PrefManager.put(HAS_SEEN_READING_TOOLTIP, true)
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
        binding.practiseSubmitLayout.visibility = VISIBLE
    }

    fun hidePracticeSubmitLayout() {
        //binding.yourSubAnswerTv.visibility = GONE
        binding.practiseSubmitLayout.visibility = GONE
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
        for (i in 0 until binding.audioListRv.getChildCount()) {
            val holder: PracticeAudioAdapter.PracticeAudioViewHolder =
                binding.audioListRv.findViewHolderForAdapterPosition(i) as PracticeAudioAdapter.PracticeAudioViewHolder
            holder.pauseAudio()
        }
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listen(RemovePracticeAudioEventBus::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe(
                    {
                        AppObjectController.uiHandler.post {
                            try {
                                MixPanelTracker.publishEvent(MixPanelEvent.READING_RECORDING_DELETE)
                                    .addParam(ParamKeys.LESSON_ID, lessonID)
                                    .push()
                                if (binding.audioListRv[it.index] != null) {
                                    binding.audioListRv.removeViewAt(it.index)
                                    currentLessonQuestion?.run {
                                        if (this.practiceEngagement.isNullOrEmpty()) {
                                            showPracticeInputLayout()
                                            binding.feedbackLayout.visibility = GONE
                                            //binding.yourSubAnswerTv.text = getString(R.string.your_answer)
                                            hidePracticeSubmitLayout()
                                            disableSubmitButton()
                                        } else {
                                            hidePracticeInputLayout()
                                            showImproveButton()
                                        }
                                    }
                                } else {
                                    showToast("Null")
                                }
                            } catch (ex: Exception) {
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
                            //ImageShowFragment.newInstance(path, "", "")
                            //    .show(childFragmentManager, "ImageShow")
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
                            binding.videoPlayer.setFullScreenListener {
                                val videoId = this.videoList?.getOrNull(0)?.id
                                val videoUrl = this.videoList?.getOrNull(0)?.video_url
                                val currentVideoProgressPosition = binding.videoPlayer.progress
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
                            if (isAdded && activity != null) {
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
                }
                LessonMaterialType.TX -> {
                    this.qText?.let {
                        binding.infoTv.visibility = VISIBLE
                        binding.infoTv.text =
                            HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    }
                }
                else -> {
                    binding.imageView.visibility = VISIBLE
                    this.imageList?.getOrNull(0)?.imageUrl?.let { path ->
                        binding.imageView.setImageAndFitCenter(path, context)
                        binding.imageView.setOnClickListener {
                        }
                    }
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
            if (isAdded && activity != null) {
                val thumbnailDrawable: Drawable? =
                    Utils.getDrawableFromUrl(requireContext(), thumbnailUrl)
                if (thumbnailDrawable != null) {
                    AppObjectController.uiHandler.post {
                        binding.videoPlayer.useArtwork = true
                        binding.videoPlayer.defaultArtwork = thumbnailDrawable
//                    val imgArtwork: ImageView = binding.videoPlayer.findViewById(R.id.exo_artwork) as ImageView
//                    imgArtwork.setImageDrawable(thumbnailDrawable)
//                    imgArtwork.visibility = View.VISIBLE
                    }
                }
            } else {
                showToast(getString(R.string.something_went_wrong))
            }
        }
    }

    private fun setViewAccordingExpectedAnswer() {
        currentLessonQuestion?.run {
            showPracticeInputLayout()
            binding.recordingViewFrame.visibility = VISIBLE
            binding.recordTransparentContainer.visibility = VISIBLE
            binding.audioPractiseHint.visibility = VISIBLE
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
            viewLifecycleOwner
        ) {
            currentLessonQuestion = it.filter { it.chatType == CHAT_TYPE.RP }.getOrNull(0)
            video = currentLessonQuestion?.videoList?.getOrNull(0)?.video_url
            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    fetchVideo()
                }
            }
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
                    if (video.isNullOrBlank().not()) {
                        binding.videoLayout.visibility = VISIBLE
                        binding.mergedVideo.visibility = VISIBLE
                        binding.ivClose.visibility = GONE
                        binding.btnWhatsapp.visibility = VISIBLE
                        if (this.practiceEngagement?.get(0)?.localPath.isNullOrEmpty()) {
                            scope.launch {
                                if (AppObjectController.appDatabase.chatDao()
                                        .getDownloadedVideoStatus(currentLessonQuestion!!.questionId) != null && AppObjectController.appDatabase.chatDao()
                                        .getDownloadedVideoStatus(currentLessonQuestion!!.questionId)
                                ) {
                                    val submittedVideoPath =
                                        AppObjectController.appDatabase.chatDao()
                                            .getDownloadedVideoPath(currentLessonQuestion!!.questionId)
                                    setVideoPlayerWuthUrl(submittedVideoPath)
                                } else {
                                    val url = currentLessonQuestion!!.practiceEngagement?.get(
                                        0
                                    )?.answerUrl
                                    if (url != null && url.isNotBlank()) {
                                        getPermissionAndDownloadVideo(url)
                                    }
                                }
                            }
                        } else {
                            this.practiceEngagement?.get(0)?.localPath?.let { it1 ->
                                setVideoPlayerWuthUrl(
                                    it1
                                )
                            }
                        }
                        binding.btnWhatsapp.setOnClickListener {
                            viewModel.saveReadingPracticeImpression(
                                REEL_SHARED_RP,
                                lessonID.toString()
                            )
                            scope.launch {
                                if (currentLessonQuestion?.practiceEngagement?.get(0)?.localPath.isNullOrEmpty()
                                        .not()
                                ) {
                                    viewModel.shareVideoForAudio(
                                        currentLessonQuestion?.practiceEngagement?.get(
                                            0
                                        )?.localPath.toString()
                                    )
                                } else if (AppObjectController.appDatabase.chatDao()
                                        .getDownloadedVideoStatus(currentLessonQuestion!!.questionId)
                                ) {
                                    viewModel.shareVideoForAudio(
                                        AppObjectController.appDatabase.chatDao()
                                            .getDownloadedVideoPath(currentLessonQuestion!!.questionId)
                                    )
                                }
                            }
                        }
                    }
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

        viewModel.practiceFeedback2LiveData.observe(
            viewLifecycleOwner
        ) {
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

        viewModel.practiceEngagementData.observe(
            viewLifecycleOwner
        ) {
            updatePracticeFeedback(it)
            if (it.pointsList.isNullOrEmpty().not()) {
                showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, it.pointsList?.get(0))
                PrefManager.put(
                    LESSON_COMPLETE_SNACKBAR_TEXT_STRING,
                    it.pointsList!!.last(),
                    false
                )
            }
        }
        binding.btnNextStep.setOnClickListener {
            showNextTooltip()
        }

        viewModel.lessonId.observe(
            viewLifecycleOwner
        ) {
            lessonID = it
        }
    }

    private fun fetchVideo() {
        if (video.isNullOrEmpty().not()) {
            binding.info.visibility = VISIBLE
            scope.launch {
                AppObjectController.appDatabase.chatDao().insertReadingVideoDownloadedPath(
                    ReadingVideo(currentLessonQuestion!!.questionId, isDownloaded = false)
                )
            }
            if (currentLessonQuestion?.videoList?.getOrNull(0)?.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                scope.launch {
                    videoDownPath = AppObjectController.appDatabase.chatDao()
                        .getCompressedVideo(currentLessonQuestion!!.questionId)
                }
            } else {
                download()
            }
        }
    }

    private fun download() {
        if (PermissionUtils.isStoragePermissionEnabled(requireContext())) {
            downloadVideos()
        } else {
            viewModel.askStoragePermission()
        }
    }

    private fun downloadVideos() {
        currentLessonQuestion?.videoList?.let {
            if (it.isNotEmpty()) {
                scope.launch { mutex.lock() }
                DownloadUtils.downloadFile(
                    it[0].video_url!!,
                    AppDirectory.docsReceivedFile(it[0].video_url!!).absolutePath,
                    currentLessonQuestion!!.id,
                    null,
                    downloadListener,
                    true,
                    currentLessonQuestion
                )
            }
        }
    }

    private var downloadListener = object : FetchListener {
        override fun onAdded(download: Download) {
        }

        override fun onCancelled(download: Download) {
            DownloadUtils.removeCallbackListener(download.tag)
            currentLessonQuestion?.downloadStatus = DOWNLOAD_STATUS.FAILED
            muxerJob?.cancel()
            unlockDownloadLock()
        }

        override fun onCompleted(download: Download) {
            DownloadUtils.removeCallbackListener(download.tag)
            currentLessonQuestion?.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
            videoDownPath = download.file
            unlockDownloadLock()
            scope.launch {
                if (videoDownPath.isNullOrEmpty().not()) {
                    AppObjectController.appDatabase.chatDao().insertCompressedVideo(
                        CompressedVideo(currentLessonQuestion!!.questionId, videoDownPath!!)
                    )
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
            DownloadUtils.removeCallbackListener(download.tag)
            currentLessonQuestion?.downloadStatus = DOWNLOAD_STATUS.FAILED
            muxerJob?.cancel()
            unlockDownloadLock()
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
            currentLessonQuestion?.downloadStatus = DOWNLOAD_STATUS.DOWNLOADING
        }

        override fun onWaitingNetwork(download: Download) {
        }
    }

    private fun unlockDownloadLock() {
        try {
            mutex.unlock()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    private fun showCompletedPractise() {
        hidePracticeInputLayout()
        if (PrefManager.getBoolValue(IS_FREE_TRIAL))
            viewModel.getCoursePopupData(PurchasePopupType.READING_COMPLETED)
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
        if (video.isNullOrEmpty().not()) {
            binding.ivClose.visibility = GONE
            binding.btnWhatsapp.visibility = VISIBLE
            binding.btnWhatsapp.setOnClickListener {
                viewModel.shareVideoForAudio(outputFile)
                viewModel.saveReadingPracticeImpression(
                    REEL_SHARED_RP,
                    lessonID.toString()
                )
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            lessonActivityListener?.onQuestionStatusUpdate(
                QUESTION_STATUS.AT,
                currentLessonQuestion?.id
            )
            lessonActivityListener?.onSectionStatusUpdate(READING_POSITION, true)
        }
    }

    private fun updatePracticeFeedback(practiceEngagement: PracticeEngagement) {
        for (i in 0 until binding.audioListRv.getChildCount()) {
            val holder: RecyclerView.ViewHolder =
                binding.audioListRv.findViewHolderForAdapterPosition(i) as PracticeAudioAdapter.PracticeAudioViewHolder
            if (holder is PracticeAudioAdapter.PracticeAudioViewHolder && holder.isEmpty()) {
                holder.updatePracticeEngagement(practiceEngagement)
            }
        }
    }

    private fun hideCancelButtonInRV() {
        for (i in 0 until binding.audioListRv.getChildCount()) {
            val holder: PracticeAudioAdapter.PracticeAudioViewHolder =
                binding.audioListRv.findViewHolderForAdapterPosition(i) as PracticeAudioAdapter.PracticeAudioViewHolder
            holder.hideCancelButtons()
        }
    }

    private fun removePreviousAddedViewHolder() {
        for (i in 0 until binding.audioListRv.getChildCount()) {
            val holder: PracticeAudioAdapter.PracticeAudioViewHolder =
                binding.audioListRv.findViewHolderForAdapterPosition(i) as PracticeAudioAdapter.PracticeAudioViewHolder
            if (holder.isEmpty()) {
                binding.audioListRv.removeViewAt(i)
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
            if (video.isNullOrEmpty().not()) {
                if (currentLessonQuestion?.expectedEngageType == EXPECTED_ENGAGE_TYPE.VI)
                    filePath = practiseEngagement?.answerUrl
            } else {
                filePath = practiseEngagement?.answerUrl
            }

            // initializePractiseSeekBar()
        }
    }

    private fun initRV() {
        try {
            val divider = DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
            divider.setDrawable(
                ColorDrawable(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.seek_bar_background
                    )
                )
            )
            linearLayoutManager?.isSmoothScrollbarEnabled = true
            binding.audioListRv.layoutManager = linearLayoutManager
            binding.audioListRv.setHasFixedSize(true)
            binding.audioListRv.addItemDecoration(divider)
            binding.audioListRv.enforceSingleScrollDirection()
            binding.audioListRv.adapter = praticAudioAdapter
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun addAudioListRV(practiceEngagement: List<PracticeEngagement>?) {
        showPracticeSubmitLayout()
        binding.yourSubAnswerTv.visibility = VISIBLE
        if (practiceEngagement.isNullOrEmpty().not())
            binding.practiseSubmitLayout.visibility = VISIBLE
        binding.subPractiseSubmitLayout.visibility = VISIBLE
        if (video.isNullOrEmpty().not()) {
            binding.mergedVideo.visibility = VISIBLE
            binding.btnWhatsapp.visibility = VISIBLE
        } else {
            binding.audioListRv.visibility = VISIBLE
        }
        practiceEngagement?.let { practiceList ->
            val list = arrayListOf<PracticeEngagementWrapper>()
            if (practiceList.isNullOrEmpty().not()) {
                practiceList.forEach { practice ->
                    list.add(PracticeEngagementWrapper(practice, practice.answerUrl))
                }
                praticAudioAdapter?.updateList(list)
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
        for (i in 0 until binding.audioListRv.getChildCount()) {
            val holder: RecyclerView.ViewHolder =
                binding.audioListRv.findViewHolderForAdapterPosition(i) as PracticeAudioAdapter.PracticeAudioViewHolder
            if (holder is PracticeAudioAdapter.PracticeAudioViewHolder) {
                holder.initializePractiseSeekBar()
            }
        }
    }

    private fun audioAttachmentInit() {
        showPracticeSubmitLayout()
        if (video.isNullOrEmpty().not()) {
            observeNetwork()
            addVideoView()
            viewModel.showVideoOnFullScreen()
            binding.practiseSubmitLayout.visibility = GONE
        } else {
            binding.subPractiseSubmitLayout.visibility = VISIBLE
            binding.audioListRv.visibility = VISIBLE
            binding.practiseSubmitLayout.visibility = VISIBLE
            initRV()
            removePreviousAddedViewHolder()
            praticAudioAdapter?.addNewItem(PracticeEngagementWrapper(null, filePath))
            initializePractiseSeekBar()
        }
        enableSubmitButton()
    }

    private fun addVideoView() {
        binding.practiseSubmitLayout.visibility = VISIBLE
        binding.videoLayout.visibility = VISIBLE
        binding.mergedVideo.visibility = VISIBLE
        binding.subAnswerLayout.visibility = VISIBLE
        binding.ivClose.visibility = VISIBLE
        binding.progressDialog.visibility = VISIBLE
    }

    private fun recordPermission() {
        if (isAdded && activity != null) {
            PermissionUtils.audioRecordStorageReadAndWritePermission(
                requireActivity(),
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                binding.recordingView.setOnClickListener(null)
                                binding.recordTransparentContainer.setOnClickListener(null)
                                audioRecordTouchListener()
                                return
                            }
                            if (isAdded && activity != null) {
                                if (report.isAnyPermissionPermanentlyDenied) {
                                    PermissionUtils.permissionPermanentlyDeniedDialog(
                                        requireActivity(),
                                        R.string.record_permission_message
                                    )
                                    return
                                }
                            } else {
                                showToast(getString(R.string.something_went_wrong))
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
        } else {
            showToast(getString(R.string.something_went_wrong))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun audioRecordTouchListener() {
        binding.recordTransparentContainer.setOnTouchListener { _, event ->
            if (isAdded && activity != null) {
                if (isCallOngoing()) {
                    return@setOnTouchListener false
                }
                if (PermissionUtils.isAudioAndStoragePermissionEnable(requireContext()).not()) {
                    recordPermission()
                    return@setOnTouchListener true
                }
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.rootView.requestDisallowInterceptTouchEvent(true)
                    binding.counterTv.visibility = VISIBLE
                    isAudioRecording = true
                    binding.recordingView.startAnimation(scaleAnimation)

                    pauseAllAudioAndUpdateViews()
                    binding.progressBarImageView.progress = 0
                    binding.practiseSeekbar.progress = 0
                    audioManager?.seekTo(0)
                    binding.mergedVideo.seekToStart()
                    if (isAdded && activity != null)
                        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    appAnalytics?.addParam(AnalyticsEvent.AUDIO_RECORD.NAME, "Audio Recording")
                    binding.counterTv.base = SystemClock.elapsedRealtime()
                    startTime = System.currentTimeMillis()
                    binding.counterTv.start()
                    val params = binding.counterTv.layoutParams as ViewGroup.MarginLayoutParams
                    viewModel.startRecord()
                    binding.audioPractiseHint.visibility = GONE
                    AppObjectController.uiHandler.postDelayed(longPressAnimationCallback, 600)
                }
                MotionEvent.ACTION_MOVE -> {
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isAudioRecording = false
                    binding.rootView.requestDisallowInterceptTouchEvent(false)
                    binding.counterTv.stop()
                    viewModel.stopRecording()
                    binding.recordingView.clearAnimation()
                    binding.counterTv.visibility = GONE
                    binding.audioPractiseHint.visibility = VISIBLE
                    if (isAdded && activity != null)
                        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    val timeDifference =
                        TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - TimeUnit.MILLISECONDS.toSeconds(
                            startTime
                        )
                    if (timeDifference > 1) {
                        if (Utils.isInternetAvailable()) {
                            viewModel.recordFile?.let {
                                isAudioRecordDone = true
//                            Log.e("Ayaaz","${currentLessonQuestion?.videoList?.get(0)?.video_url}")
//                            if(!currentLessonQuestion?.videoList?.get(0)?.video_url.isNullOrEmpty())
//                            viewModel.showVideoOnFullScreen()

                                filePath = AppDirectory.getAudioSentFile(null).absolutePath
                                AppDirectory.copy(it.absolutePath, filePath!!)
                                audioAttachmentInit()
                                MixPanelTracker.publishEvent(MixPanelEvent.READING_RECORD)
                                    .addParam(ParamKeys.LESSON_ID, lessonID)
                                    .addParam(ParamKeys.RECORD_DURATION, timeDifference)
                                    .push()
                                AppObjectController.uiHandler.postDelayed(
                                    {
                                        binding.submitAnswerBtn.parent.requestChildFocus(
                                            binding.submitAnswerBtn,
                                            binding.submitAnswerBtn
                                        )
                                    }, 200
                                )

                                val duration = event.eventTime - event.downTime

                                if (duration < CLICK_OFFSET_PERIOD) {
                                    AppObjectController.uiHandler.removeCallbacks(
                                        longPressAnimationCallback
                                    )
                                    showRecordHintAnimation()
                                }
                                if (video.isNullOrEmpty().not()) {
                                    viewModel.saveReadingPracticeImpression(
                                        RECORD_READING_VIDEO,
                                        lessonID.toString()
                                    )
                                }
                                muxerJob = scope.launch {
                                    mutex.withLock {
                                        if (isActive && isAdded && video != null) {
                                            if (videoDownPath != null) {
                                                mergeTwoAudiosIntoOne()
                                            } else {
                                                try {
                                                    video = null
                                                    withContext(Dispatchers.Main) {
                                                        showToast(getString(R.string.generic_message_for_error))
                                                        viewModel.closeCurrentFragment()
                                                        binding.videoLayout.visibility = GONE
                                                        audioAttachmentInit()
                                                    }
                                                } catch (ex: Exception) {
                                                    LogException.catchException(ex)
                                                }
                                            }
                                            //muxVideoOldMethod()
                                        }
                                    }
                                }
                            }
                        } else {
                            showToast(getString(R.string.internet_not_available_msz))
                        }
                    }
                }
            }
            true
        }
    }

    private fun mergeTwoAudiosIntoOne() {
        try {
            val input1 = GeneralAudioInput(requireContext(), Uri.parse(filePath), null)
            input1.volume = 5f
            val out2 = extractAudioFromVideo(videoDownPath!!)
            val input2 = GeneralAudioInput(requireContext(), Uri.parse(out2), null)

            val mergedAudioPath = getAudioFilePathMP3()
            val audioMixer = AudioMixer(mergedAudioPath)
            audioMixer.addDataSource(input1)
            audioMixer.addDataSource(input2)
            audioMixer.setSampleRate(32000)
            audioMixer.setBitRate(48000)
            audioMixer.setChannelCount(2)
            Log.d(
                TAG,
                "mergeTwoAudiosIntoOne() called ${input1.sampleRate} ${input1.bitrate} innpt2 ${input2.sampleRate} ${input2.bitrate}"
            )
            audioMixer.mixingType = AudioMixer.MixingType.PARALLEL
            audioMixer.setProcessingListener(object : AudioMixer.ProcessingListener {
                override fun onProgress(progress: Double) {
                    Log.d(TAG, "onProgress() called with: progress = $progress")
                }

                override fun onEnd() {
                    Log.d(TAG, "onEnd() called $mergedAudioPath $filePath")
                    muxVideoOldMethod(mergedAudioPath)
                    audioMixer.release()
                }

            })

            audioMixer.start()
            audioMixer.processAsync()
        } catch (ex: Exception) {
            muxVideoOldMethod(filePath!!)
        }
    }

    fun mux(audioFile: String, videoFile: String) {
        if (File(outputFile).exists()) {
            File(outputFile).delete()
        }
        if (Build.VERSION.SDK_INT >= 29) {
            if (isAdded) {
                outputFile = saveVideoQ(
                    requireContext(),
                    videoDownPath ?: EMPTY
                ) ?: EMPTY
            }
        } else {
            outputFile = getVideoFilePath()
        }

        // Init extractors which will get encoded frames
        val videoExtractor = MediaExtractor()
        videoExtractor.setDataSource(requireContext(), Uri.parse(videoFile), null)
        videoExtractor.selectTrack(0) // Assuming only one track per file. Adjust code if this is not the case.
        val videoFormat = videoExtractor.getTrackFormat(0)

        val audioExtractor = MediaExtractor()
        audioExtractor.setDataSource(audioFile)
        audioExtractor.selectTrack(0) // Assuming only one track per file. Adjust code if this is not the case.
        val audioFormat = audioExtractor.getTrackFormat(0)
        Log.d(TAG, "mux() called with: audioFormat = $audioFormat, videoFormat = $videoFormat")
        // Init muxer
        val muxer = MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val videoIndex = muxer.addTrack(videoFormat)
        val audioIndex = muxer.addTrack(audioFormat)
        muxer.start()

        // Prepare buffer for copying
        val maxChunkSize = 1024 * 1024
        val buffer = ByteBuffer.allocate(maxChunkSize)
        val bufferInfo = MediaCodec.BufferInfo()

        // Copy Video
        while (true) {
            val chunkSize = videoExtractor.readSampleData(buffer, 0)

            if (chunkSize > 0) {
                bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
                bufferInfo.size = chunkSize
                muxer.writeSampleData(videoIndex, buffer, bufferInfo)
                videoExtractor.advance()

            } else {
                break
            }
        }

        // Copy audio
        while (true) {
            val chunkSize = audioExtractor.readSampleData(buffer, 0)
            if (chunkSize >= 0) {
                bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
                bufferInfo.size = chunkSize

                muxer.writeSampleData(audioIndex, buffer, bufferInfo)
                audioExtractor.advance()
            } else {
                break
            }
        }

        // Cleanup
        muxer.stop()
        muxer.release()
        videoExtractor.release()
        audioExtractor.release()
        if (isAdded && activity != null) {
            requireActivity().runOnUiThread {
                setVideoPlayerWuthUrl(outputFile)
                binding.progressDialog.visibility = GONE
                viewModel.sendOutputToFullScreen(outputFile)
            }
        }
        Log.d(TAG, "mux() called with: outputFile = $outputFile ")
    }

    private fun muxVideoOldMethod(audio: String) {
        if (File(outputFile).exists()) {
            File(outputFile).delete()
        }
        if (Build.VERSION.SDK_INT >= 29) {
            if (isAdded) {
                outputFile = saveVideoQ(
                    requireContext(),
                    videoDownPath ?: EMPTY
                ) ?: EMPTY
            }
        } else {
            outputFile = getVideoFilePath()
        }
        audioVideoMuxer(audio)
        if (isAdded && activity != null) {
            requireActivity().runOnUiThread {
                binding.progressDialog.visibility = GONE
                viewModel.sendOutputToFullScreen(outputFile)
            }
        }
    }

    private fun getAudioFilePathAAC(): String {
        return getAndroidDownloadFolder()?.absolutePath + "/" + "JoshSkill-" + System.currentTimeMillis() + ".aac"
    }

    private fun getAudioFilePathMP3(): String {
        return getAndroidDownloadFolder()?.absolutePath + "/" + "JoshSkill-" + System.currentTimeMillis() + ".mp3"
    }

    private fun extractAudioFromVideo(videoPath: String): String {
        val outputAudioPath = getAudioFilePathAAC()
        val videoAudioExtractor: MediaExtractor = MediaExtractor()
        var videoAudioFormat: MediaFormat = MediaFormat()
        val sampleSize = 256 * 1024
        var sawEOS = false
        var frameCount = 0
        val offset = 100

        videoAudioExtractor.setDataSource(videoPath)
        for (i in 0 until videoAudioExtractor.trackCount) {
            videoAudioFormat = videoAudioExtractor.getTrackFormat(i)
            val mime = videoAudioFormat.getString(MediaFormat.KEY_MIME);
            if (mime!!.startsWith("audio/")) {
                videoAudioExtractor.selectTrack(i)
            }
        }

        val muxer: MediaMuxer = MediaMuxer(
            outputAudioPath,
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        )

        val videoAudioTrack = muxer.addTrack(videoAudioFormat)
        val videoAudioBuffer: ByteBuffer = ByteBuffer.allocate(sampleSize)
        val videoAudioBufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
        videoAudioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        muxer.start()

        while (!sawEOS) {
            frameCount++

            videoAudioBufferInfo.offset = offset
            videoAudioBufferInfo.size = videoAudioExtractor.readSampleData(videoAudioBuffer, offset)

            if (videoAudioBufferInfo.size < 0) {
                sawEOS = true
                videoAudioBufferInfo.size = 0
            } else {
                videoAudioBufferInfo.presentationTimeUs = videoAudioExtractor.getSampleTime()
                videoAudioBufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
                muxer.writeSampleData(videoAudioTrack, videoAudioBuffer, videoAudioBufferInfo)
                videoAudioExtractor.advance()
            }
        }

        muxer.stop()
        muxer.release()
        videoAudioExtractor.release()

        return outputAudioPath
    }

    fun audioVideoMuxer(filePath: String) {
        try {
            val videoExtractor: MediaExtractor = MediaExtractor()
            val audioExtractor: MediaExtractor = MediaExtractor()
            audioExtractor.setDataSource(filePath!!)
            audioExtractor.selectTrack(0)
            val audioFormat: MediaFormat = audioExtractor.getTrackFormat(0)
            videoDownPath?.let { videoExtractor.setDataSource(it) }
            videoExtractor.selectTrack(0)
            val videoFormat: MediaFormat = videoExtractor.getTrackFormat(0)

            val muxer: MediaMuxer = MediaMuxer(
                outputFile,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )

            val videoTrack = muxer.addTrack(videoFormat)
            val audioTrack = muxer.addTrack(audioFormat)

            var sawEOS = false
            var frameCount = 0
            val offset = 100
            val sampleSize = 256 * 1024
            val videoBuf: ByteBuffer = ByteBuffer.allocate(sampleSize)
            val audioBuf: ByteBuffer = ByteBuffer.allocate(sampleSize)
            val videoBufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
            val audioBufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()

            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            muxer.start()
            while (!sawEOS) {
                videoBufferInfo.offset = offset
                videoBufferInfo.size = videoExtractor.readSampleData(videoBuf, offset)

                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
                    sawEOS = true
                    videoBufferInfo.size = 0

                } else {
                    videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime()
                    videoBufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
                    muxer.writeSampleData(videoTrack, videoBuf, videoBufferInfo)
                    videoExtractor.advance()

                    frameCount++
                }
            }

            var sawEOS2 = false
            var frameCount2 = 0
            while (!sawEOS2) {
                frameCount2++

                audioBufferInfo.offset = offset
                audioBufferInfo.size = audioExtractor.readSampleData(audioBuf, offset)

                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
                    sawEOS2 = true
                    audioBufferInfo.size = 0
                } else {
                    audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime()
                    audioBufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                    muxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo)
                    audioExtractor.advance()
                }
            }

            muxer.stop()
            muxer.release()
            setVideoPlayerWuthUrl(outputFile)
            val op = MediaExtractor()
            op.setDataSource(outputFile)
            for (i in 0..op.trackCount - 1) {
                Log.d(TAG, "audioVideoMuxer() called with: OP = ${op.getTrackFormat(i)}")
            }

        } catch (e: IOException) {
            Timber.e(e)
        } catch (e: Exception) {
            Timber.e(e)
        }

    }

    fun inviteFriends(waIntent: Intent) {
        try {
            startActivity(Intent.createChooser(waIntent, "Share with"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun closeRecordedView() {
        try {
            binding.mergedVideo.onPause()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        binding.practiseSubmitLayout.visibility = GONE
        disableSubmitButton()
    }

    fun playVideoEvent() {
        if (video.isNullOrEmpty().not()) {
            viewModel.saveReadingPracticeImpression(
                VIDEO_PLAYED_RP,
                lessonID.toString()
            )
        }
    }

    private fun gainAudioFocus() {
        val mAudioManager =
            context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager!!.requestAudioFocus(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener {
                    }.build()
            )
        } else {
            mAudioManager?.requestAudioFocus(
                { },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    fun playPracticeAudio() {
        gainAudioFocus()
        if (PrefManager.hasKey(HAS_SEEN_READING_PLAY_ANIMATION).not() || PrefManager.getBoolValue(
                HAS_SEEN_READING_PLAY_ANIMATION
            ).not()
        ) {
            PrefManager.put(HAS_SEEN_READING_PLAY_ANIMATION, true)
            binding.playInfoHint.visibility = GONE
        }
        if (isAudioRecording.not()) {
            if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) <= 0) {
                StyleableToast.Builder(AppObjectController.joshApplication).gravity(Gravity.BOTTOM)
                    .text(getString(R.string.volume_up_message)).cornerRadius(16)
                    .length(Toast.LENGTH_LONG)
                    .solidBackground().show()
            }
            appAnalytics?.addParam(AnalyticsEvent.PRACTICE_EXTRA.NAME, "Audio Played")
            if (binding.btnPlayInfo.state == MaterialPlayPauseDrawable.State.Play) {
                MixPanelTracker.publishEvent(MixPanelEvent.READING_PLAY)
                    .addParam(ParamKeys.LESSON_ID, lessonID)
                    .push()
            } else {
                MixPanelTracker.publishEvent(MixPanelEvent.READING_PAUSE)
                    .addParam(ParamKeys.LESSON_ID, lessonID)
                    .push()
            }
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
            for (i in 0 until binding.audioListRv.getChildCount()) {
                val holder: RecyclerView.ViewHolder =
                    binding.audioListRv.findViewHolderForAdapterPosition(i) as PracticeAudioAdapter.PracticeAudioViewHolder
                if (holder is PracticeAudioAdapter.PracticeAudioViewHolder) {
                    holder.setPlayPauseBtnState(MaterialPlayPauseDrawable.State.Pause)
                    binding.btnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
                }
            }
        } else {
            for (i in 0 until binding.audioListRv.getChildCount()) {
                val holder: RecyclerView.ViewHolder =
                    binding.audioListRv.findViewHolderForAdapterPosition(i) as PracticeAudioAdapter.PracticeAudioViewHolder
                if (holder is PracticeAudioAdapter.PracticeAudioViewHolder) {
                    holder.setPlayPauseBtnState(MaterialPlayPauseDrawable.State.Play)
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
        if (Utils.isInternetAvailable()) {
            MixPanelTracker.publishEvent(MixPanelEvent.READING_SUBMIT)
                .addParam(ParamKeys.LESSON_ID, lessonID)
                .push()
            CoroutineScope(Dispatchers.IO).launch {
                currentLessonQuestion?.expectedEngageType?.let {
                    if (EXPECTED_ENGAGE_TYPE.AU == it && isAudioRecordDone.not()) {
                        showToast(getString(R.string.submit_practise_msz))
                    } else if (EXPECTED_ENGAGE_TYPE.VI == it && isAudioRecordDone.not()) {
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
                        AppObjectController.uiHandler.post {
                            if (video.isNullOrBlank().not()) {
                                binding.btnWhatsapp.visibility = VISIBLE
                                binding.practiseSubmitLayout.visibility = VISIBLE
                                binding.continueBtn.visibility = VISIBLE
                            }
                        }
                        MixPanelTracker.publishEvent(MixPanelEvent.READING_COMPLETED)
                            .addParam(ParamKeys.LESSON_ID, lessonID)
                            .push()
                        val requestEngage = RequestEngage()
                        if (it == EXPECTED_ENGAGE_TYPE.VI) {
                            requestEngage.localPath = outputFile
                        } else {
                            requestEngage.localPath = filePath
                        }
                        requestEngage.duration =
                            Utils.getDurationOfMedia(requireActivity(), filePath)?.toInt()
                        // requestEngage.feedbackRequire = currentLessonQuestion.feedback_require
                        requestEngage.questionId = currentLessonQuestion!!.id
                        requestEngage.mentor = Mentor.getInstance().getId()
                        if (it == EXPECTED_ENGAGE_TYPE.AU) {
                            requestEngage.answerUrl = filePath
                        } else if (it == EXPECTED_ENGAGE_TYPE.VI) {
                            requestEngage.answerUrl = outputFile
                        }
                        AppObjectController.uiHandler.post {
                            // binding.progressLayout.visibility = INVISIBLE
                            binding.feedbackLayout.visibility = GONE
                            binding.progressLayout.visibility = VISIBLE
                            binding.feedbackGrade.visibility = GONE
                            binding.feedbackDescription.visibility = GONE
                            binding.recordingViewFrame.visibility = GONE
                            binding.recordTransparentContainer.visibility = GONE
                            //binding.readingHoldHint.visibility = GONE
                            binding.audioPractiseHint.visibility = GONE
                            binding.counterTv.visibility = GONE
                            binding.yourSubAnswerTv.text = getString(R.string.your_submitted_answer)
                            disableSubmitButton()
                        }
                        // practiceViewModel.submitPractise(chatModel, requestEngage, engageType)
                        if (video.isNullOrEmpty().not()) {
                            viewModel.saveReadingPracticeImpression(
                                SUBMIT_READING_VIDEO,
                                lessonID.toString()
                            )
                        }
                        viewModel.getPointsForVocabAndReading(currentLessonQuestion!!.id)
                        viewModel.addTaskToService(requestEngage, PendingTask.READING_PRACTICE_OLD)
                        //viewModel.updatePracticeEngagement(requestEngage)
                        currentLessonQuestion!!.status = QUESTION_STATUS.IP
                        delay(300)
                        AppObjectController.uiHandler.post {
                            showCompletedPractise()
                        }

                    }
                }
            }
        } else {
            showToast(getString(R.string.internet_not_available_msz))
        }
    }

    fun onReadingContinueClick() {
        MixPanelTracker.publishEvent(MixPanelEvent.READING_CONTINUE)
            .addParam(ParamKeys.LESSON_ID, lessonID)
            .push()
        lessonActivityListener?.onNextTabCall(
            READING_POSITION.minus(
                if (PrefManager.getBoolValue(IS_A2_C1_RETENTION_ENABLED)) 0
                else 1
            )
        )
    }

/*

    override fun onBackPressed() {
        super.onBackPressed()
        requireActivity().finishAndRemoveTask()
    }
*/

    override fun onPlayerPause() {
        binding.btnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
        AppObjectController.uiHandler.removeCallbacks(pauseAnimationCallback)
        AppObjectController.uiHandler.postDelayed(pauseAnimationCallback, 1000)
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
        showRecordHintAnimation()
    }

    override fun onProgressUpdate(progress: Long) {
        binding.practiseSeekbar.progress = progress.toInt()
        lifecycleScope.launchWhenStarted { }
    }

    override fun onDurationUpdate(duration: Long?) {
        duration?.toInt()?.let { binding.practiseSeekbar.max = it }
    }

    companion object {
        var isAudioRecording = false
        private const val IMAGE_OR_VIDEO_SELECT_REQUEST_CODE = 1081
        private const val TEXT_FILE_ATTACHMENT_REQUEST_CODE = 1082
        private const val DOWNLOAD_VIDEO_REQUEST_CODE = 1846
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

    private fun observeNetwork() {
        if (isAdded && activity!=null) {
            compositeDisposable.add(
                ReactiveNetwork.observeNetworkConnectivity(requireActivity())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { connectivity ->
                        internetAvailableFlag =
                            connectivity.state() == NetworkInfo.State.CONNECTED && connectivity.available()
                        if (!internetAvailableFlag && videoDownPath == null) {
                            disableSubmitButton()
                            showToast("Internet not available")
                        } else {
                            enableSubmitButton()
                        }
                    }
            )
        }
    }

}
