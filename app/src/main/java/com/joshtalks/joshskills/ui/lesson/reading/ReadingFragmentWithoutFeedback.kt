package com.joshtalks.joshskills.ui.lesson.reading

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.*
import android.content.res.ColorStateList
import android.graphics.Color
import com.joshtalks.joshskills.BuildConfig
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.*
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
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
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
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.chat.DEFAULT_TOOLTIP_DELAY_IN_MS
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import com.joshtalks.joshskills.ui.lesson.READING_POSITION
import com.joshtalks.joshskills.ui.pdfviewer.CURRENT_VIDEO_PROGRESS_POSITION
import com.joshtalks.joshskills.ui.pdfviewer.PdfViewerActivity
import com.joshtalks.joshskills.ui.referral.USER_SHARE_SHORT_URL
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.util.ExoAudioPlayer
import com.joshtalks.joshskills.util.ExoAudioPlayer.ProgressUpdateListener
import com.joshtalks.videocompressor.CompressionListener
import com.joshtalks.videocompressor.VideoCompressor
import com.joshtalks.videocompressor.VideoQuality
import com.joshtalks.videocompressor.config.Configuration
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.muddzdev.styleabletoast.StyleableToast
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2core.DownloadBlock
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Defines
import io.branch.referral.util.LinkProperties
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.reading_practice_fragment_without_feedback.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException
import java.lang.Runnable
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList


private const val TAG = "ReadingFragmentWithoutFeedback"
class ReadingFragmentWithoutFeedback :
    CoreJoshFragment(),
    Player.EventListener,
    AudioPlayerEventListener,
    ProgressUpdateListener {

    private val CLICK_OFFSET_PERIOD = 300L
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
    private var userReferralCode: String = EMPTY
    private var video: String? = null
    private var videoDownPath : String? = null
    //lateinit var videoDownPath: String
    private var outputFile : String = ""
    private var downloadID: Long = -1
    lateinit var fileName : String
    private var fileDir: String = ""
    private var flag: Boolean = false
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex(false)

    private var onDownloadCompleteListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID == id) {
                fileDir = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DOWNLOADS)?.absolutePath + File.separator + fileName
                scope.launch {
                    AppObjectController.appDatabase.chatDao().updateReadingTable(currentLessonQuestion!!.questionId.toString(), fileDir, true)
                }
                binding.mergedVideo.setVideoPath(fileDir)
            }
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
        ViewModelProvider(requireActivity()).get(LessonViewModel::class.java)
    }

    private val progressAnimator by lazy {
        ValueAnimator.ofInt(0, 60).apply {
            duration = 1850
            addUpdateListener {
                binding.progressAnimation.progress = it.animatedValue as Int
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    binding.recordingView.scaleX = 0.95f
                    binding.recordingView.scaleY = 0.95f
                    binding.recordingView.backgroundTintList = ContextCompat.getColorStateList(
                        AppObjectController.joshApplication,
                        R.color.highlight_btn_color
                    )
                }

                override fun onAnimationEnd(animation: Animator?) {
                    binding.progressAnimation.progress = 0
                    binding.recordingView.scaleX = 1f
                    binding.recordingView.scaleY = 1f
                    binding.recordingView.backgroundTintList = ContextCompat.getColorStateList(
                        AppObjectController.joshApplication,
                        R.color.button_color
                    )
                }

                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationRepeat(animation: Animator?) {}

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
        scaleAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.scale)
        addObserver()
        // showTooltip()

        binding.mergedVideo.setOnClickListener {
            if(binding.mergedVideo.isPlaying){
                binding.mergedVideo.pause()
                binding.playBtn.visibility = VISIBLE
            }
            if(binding.mergedVideo.isPlaying.not()){
                binding.playBtn.visibility = VISIBLE
            }
//            if(binding.progressDialog.visibility == VISIBLE){
//                binding.progressDialog.visibility = GONE
//            }

//            if(binding.playBtn.visibility == INVISIBLE){
//                binding.playBtn.visibility = VISIBLE
//            }
//            else if(binding.mergedVideo.isPlaying.not()){
//                binding.mergedVideo.start()
//                binding.playBtn.visibility = INVISIBLE
//            }
        }
        binding.mergedVideo.setOnCompletionListener {
            binding.playBtn.visibility = VISIBLE
        }
        return binding.rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (PrefManager.hasKey(HAS_SEEN_READING_PLAY_ANIMATION).not() || PrefManager.getBoolValue(
                HAS_SEEN_READING_PLAY_ANIMATION
            ).not()
        ) {
            binding.playInfoHint.visibility = VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        showRecordHintAnimation()
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
        binding.mergedVideo.pause()
        binding.playBtn.visibility = VISIBLE
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
                override fun onAnimationStart(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {}

                override fun onAnimationCancel(animation: Animator?) {
                    progressAnimator.cancel()
                }

                override fun onAnimationRepeat(animation: Animator?) {
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
        binding.mergedVideo.stopPlayback()
        binding.playBtn.visibility = VISIBLE
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
                                    //binding.yourSubAnswerTv.text = getString(R.string.your_answer)
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
                            binding.videoPlayer.setPlayListener {
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
                    binding.imageView.visibility = VISIBLE
                    this.imageList?.getOrNull(0)?.imageUrl?.let { path ->
                        binding.imageView.setImageAndFitCenter(path, context)
                        binding.imageView.setOnClickListener {
                            //ImageShowFragment.newInstance(path, "", "")
                            //    .show(childFragmentManager, "ImageShow")
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
            viewLifecycleOwner,
            {
                currentLessonQuestion = it.filter { it.chatType == CHAT_TYPE.RP }.getOrNull(0)
                video = currentLessonQuestion?.videoList?.getOrNull(0)?.video_url

                if(video.isNullOrEmpty().not()) {
                    scope.launch {
                        AppObjectController.appDatabase.chatDao().insertReadingVideoDownloadedPath(
                            ReadingVideo(currentLessonQuestion!!.questionId, " ", false)
                        )
                    }
                    if (currentLessonQuestion?.videoList?.getOrNull(0)?.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                        scope.launch {
                            videoDownPath = AppObjectController.appDatabase.chatDao().getCompressedVideo(currentLessonQuestion!!.questionId)
                        }
                    } else {
                        // binding.progressDialog.visibility = VISIBLE
                        // TODO: Prone to Error - If
                        download()
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
//                        if(video.isNullOrEmpty().not()){
//                            binding.progressDialog.visibility = VISIBLE
//                            CoroutineScope(Dispatchers.IO).launch{
//                                delay(10000)
//                            }
//                        }
                        setViewAccordingExpectedAnswer()
                    } else {
                        if(video.isNullOrBlank().not()){
                            binding.videoLayout.visibility = VISIBLE
                            binding.mergedVideo.visibility = VISIBLE
                            binding.ivClose.visibility = GONE
                            binding.ivShare.visibility = VISIBLE
                            if(this.practiceEngagement?.get(0)?.localPath.isNullOrEmpty()){
                                scope.launch {
                                    if(AppObjectController.appDatabase.chatDao().getDownloadedVideoStatus(currentLessonQuestion!!.questionId) != null && AppObjectController.appDatabase.chatDao().getDownloadedVideoStatus(currentLessonQuestion!!.questionId)){
                                        var ansVideoPath = AppObjectController.appDatabase.chatDao().getDownloadedVideoPath(currentLessonQuestion!!.questionId)
                                        binding.mergedVideo.setVideoPath(ansVideoPath)
                                    }else if(AppObjectController.appDatabase.chatDao().getDownloadedVideoStatus(currentLessonQuestion!!.questionId) != null && AppObjectController.appDatabase.chatDao().getDownloadedVideoStatus(currentLessonQuestion!!.questionId) == false){
                                        getPermissionAndDownloadVideo(currentLessonQuestion!!.practiceEngagement?.get(0)?.answerUrl.toString())
                                    }
                                }
                            }else{
                                binding.mergedVideo.setVideoPath(this.practiceEngagement?.get(0)?.localPath)
                            }
                            binding.ivShare.setOnClickListener {
                                scope.launch {
                                    if(currentLessonQuestion?.practiceEngagement?.get(0)?.localPath.isNullOrEmpty().not()){
                                        shareVideoForAudio(currentLessonQuestion?.practiceEngagement?.get(0)?.localPath.toString() )
                                    }else if(AppObjectController.appDatabase.chatDao().getDownloadedVideoStatus(currentLessonQuestion!!.questionId)){
                                        shareVideoForAudio(AppObjectController.appDatabase.chatDao().getDownloadedVideoPath(currentLessonQuestion!!.questionId))
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
                    PrefManager.put(
                        LESSON_COMPLETE_SNACKBAR_TEXT_STRING,
                        it.pointsList!!.last(),
                        false
                    )
                }
            }
        )
        binding.btnNextStep.setOnClickListener {
            showNextTooltip()
        }
    }
    private fun download(){
        Log.e("tocheck", "start download")

        if (PermissionUtils.isStoragePermissionEnabled(requireContext()).not()) {
            askStoragePermission(DOWNLOAD_VIDEO_REQUEST_CODE)
            return
        }
        Log.e("tocheck", "start download $currentLessonQuestion")
        Log.e("tocheck", "start download ${currentLessonQuestion?.videoList}")
        currentLessonQuestion?.videoList?.let {
            if (it.isNotEmpty()) {
                flag = false
                scope.launch { mutex.lock() }
                Log.e("tocheck", "start download2")
                DownloadUtils.downloadFile(
                    it[0].video_url!!,
                    AppDirectory.docsReceivedFile(it[0].video_url!!).absolutePath,
                    currentLessonQuestion!!.id,
                    null,
                    downloadListener,
                    true,
                    currentLessonQuestion
                )
            } else if (BuildConfig.DEBUG) {
            }
        }
    }

    // TODO: Handle Error Case
    private var downloadListener = object : FetchListener {
        override fun onAdded(download: Download) {
        }

        override fun onCancelled(download: Download) {
            DownloadUtils.removeCallbackListener(download.tag)
            currentLessonQuestion?.downloadStatus = DOWNLOAD_STATUS.FAILED
            //showToast("download Failed")
            Log.e("tocheck", "start download8")
        }

        override fun onCompleted(download: Download) {
            DownloadUtils.removeCallbackListener(download.tag)
            currentLessonQuestion?.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
            //showToast("video Downloaded")
            videoDownPath = download.file.toString()
            try{
                Log.e("tocheck", "start download3")
                mutex.unlock()
            }catch (e: IllegalStateException){
                e.printStackTrace()
                Log.e("tocheck", "start download4")
            }
            //mutex.unlock()
            flag = true

            binding.progressDialog.visibility = GONE

            scope.launch {
                if(videoDownPath.isNullOrEmpty().not()){
                    AppObjectController.appDatabase.chatDao().insertCompressedVideo(
                        CompressedVideo(currentLessonQuestion!!.questionId, videoDownPath!!)
                    )
                }
            }

            var uris = mutableListOf<Uri>()
            uris.add(Uri.parse(videoDownPath))
            processVideo(uris)
        }

        override fun onDeleted(download: Download) {
            Log.e("tocheck", "start download9")
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
            //showToast("download error")
            Log.e("tocheck", "start download10")
        }

        override fun onPaused(download: Download) {
            Log.e("tocheck", "start download11")
        }

        override fun onProgress(
            download: Download,
            etaInMilliSeconds: Long,
            downloadedBytesPerSecond: Long
        ) {
            Log.e("tocheck", "start download12")
        }

        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
            Log.e("tocheck", "start download13")
        }

        override fun onRemoved(download: Download) {
            Log.e("tocheck", "start download14")
        }

        override fun onResumed(download: Download) {
        }

        override fun onStarted(
            download: Download,
            downloadBlocks: List<DownloadBlock>,
            totalBlocks: Int
        ) {
            currentLessonQuestion?.downloadStatus = DOWNLOAD_STATUS.DOWNLOADING
            //showToast("downloading")
            Log.e("tocheck", "start download15")
        }

        override fun onWaitingNetwork(download: Download) {
            Log.e("tocheck", "start download16")
        }
    }

    private fun askStoragePermission(requestCode: Int) {

        PermissionUtils.storageReadAndWritePermission1(
            requireActivity(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let {
                        if (report.areAllPermissionsGranted()) {
                            if (requestCode == DOWNLOAD_VIDEO_REQUEST_CODE) {
                                Log.e("tocheck", "start download permission")
                                download()
                            }
                        } else if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(
                                requireActivity(),
                                R.string.grant_storage_permission
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
        if(currentLessonQuestion?.videoList?.getOrNull(0)?.video_url.isNullOrEmpty().not()){
            binding.ivClose.visibility = GONE
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
            if(video.isNullOrEmpty().not()){
                if(currentLessonQuestion?.expectedEngageType ==EXPECTED_ENGAGE_TYPE.VI)
                    filePath = practiseEngagement?.answerUrl
            }else{
                filePath = practiseEngagement?.answerUrl
            }
            //filePath = practiseEngagement?.answerUrl

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
        binding.audioList.enforceSingleScrollDirection()
    }

    private fun addAudioListRV(practiceEngagement: List<PracticeEngagement>?) {
        showPracticeSubmitLayout()
        binding.yourSubAnswerTv.visibility = VISIBLE
        if (practiceEngagement.isNullOrEmpty().not())
            binding.practiseSubmitLayout.visibility = VISIBLE
        binding.subPractiseSubmitLayout.visibility = VISIBLE
        if(video.isNullOrEmpty().not()){
            binding.mergedVideo.visibility = VISIBLE
            binding.ivShare.visibility = VISIBLE
        }else{
            binding.audioList.visibility = VISIBLE
        }
        // binding.audioList.visibility = VISIBLE
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
        if(video.isNullOrEmpty().not()){
            binding.practiseSubmitLayout.visibility = VISIBLE
            binding.videoLayout.visibility = VISIBLE
            binding.mergedVideo.visibility = VISIBLE
            binding.subAnswerLayout.visibility = VISIBLE
            binding.ivClose.visibility = VISIBLE
            // binding.progressDialog.visibility = VISIBLE
        }else{
            binding.subPractiseSubmitLayout.visibility = VISIBLE
            binding.audioList.visibility = VISIBLE
            removePreviousAddedViewHolder()
            binding.audioList.addView(
                PracticeAudioViewHolder(null, context, filePath) {
                    binding.btnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
                }
            )
            initializePractiseSeekBar()
        }
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
                            binding.recordTransparentContainer.setOnClickListener(null)
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
        binding.recordTransparentContainer.setOnTouchListener { _, event ->
            if (isCallOngoing()) {
                return@setOnTouchListener false
            }
            if (PermissionUtils.isAudioAndStoragePermissionEnable(requireContext()).not()) {
                recordPermission()
                return@setOnTouchListener true
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.rootView.requestDisallowInterceptTouchEvent(true)
                    //AppObjectController.uiHandler.postAtFrontOfQueue {
                    binding.counterTv.visibility = VISIBLE
                    //}
                    isAudioRecording = true
                    //binding.recordingViewFrame.layoutTransition?.setAnimateParentHierarchy(false)
                    binding.recordingView.startAnimation(scaleAnimation)
                    //binding.recordingViewFrame.layoutTransition?.setAnimateParentHierarchy(false)
                    binding.videoPlayer.onPause()
                    pauseAllAudioAndUpdateViews()
                    //PrefManager.put(HAS_SEEN_VOCAB_HAND_TOOLTIP,true)
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
                    //AppObjectController.uiHandler.postAtFrontOfQueue {
                    binding.counterTv.visibility = GONE
                    //}
                    binding.audioPractiseHint.visibility = VISIBLE
                    requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    val timeDifference =
                        TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - TimeUnit.MILLISECONDS.toSeconds(
                            startTime
                        )
                    if (timeDifference > 1) {
                        viewModel.recordFile?.let {
                            isAudioRecordDone = true

                            if(File(outputFile).exists()){
                                File(outputFile).delete()
                            }
                            outputFile = (Environment.getExternalStorageDirectory().path + "/Download/MergedVideo_" + System.currentTimeMillis().toString() +".mp4")

                            filePath = AppDirectory.getAudioSentFile(null).absolutePath
                            AppDirectory.copy(it.absolutePath, filePath!!)
                            audioAttachmentInit()
                            scope.launch {
                                Log.e("tocheck", "start download5")
                                mutex.withLock {
                                    Log.e("tocheck", "start download6")
                                    audioVideoMuxer()
                                    Log.e("tocheck", "start download7")
                                    binding.playBtn.visibility = VISIBLE

                                    AppObjectController.uiHandler.postDelayed(
                                        {
                                            binding.submitAnswerBtn.parent.requestChildFocus(
                                                binding.submitAnswerBtn,
                                                binding.submitAnswerBtn
                                            )
                                        },
                                        200
                                    )

                                    val duration = event.eventTime - event.downTime

                                    if (duration < CLICK_OFFSET_PERIOD) {
                                        AppObjectController.uiHandler.removeCallbacks(longPressAnimationCallback)
                                        showRecordHintAnimation()
                                    }
                                }
                            }
//                            if(flag == true){
//                                Log.e("checkFlag11", flag.toString() )
//                                scope.launch {
//                                audioVideoMuxer()
//                            }
//                            }else{
//                                while (flag== true){
//                                    Log.e("checkFlag", flag.toString() )
//                                    scope.launch {
//                                        audioVideoMuxer()
//                                    }
//                                }
//                            }
                        }
                    }
                }
            }
            true
        }
    }

    suspend fun audioVideoMuxer(){
        try {
            val videoExtractor: MediaExtractor  = MediaExtractor()
            val cmpPath: String = AppObjectController.appDatabase.chatDao().getCompressedVideo(currentLessonQuestion!!.questionId)
            //val videoDownPath: String = AppObjectController.appDatabase.chatDao().getCompressedVideo(currentLessonQuestion!!.questionId)


            val audioExtractor: MediaExtractor  = MediaExtractor()
            audioExtractor.setDataSource(filePath!!)
            audioExtractor.selectTrack(0)
            val audioFormat: MediaFormat = audioExtractor.getTrackFormat(0)

           // toCheck(videoExtractor, audioFormat, audioExtractor)
            videoDownPath?.let { videoExtractor.setDataSource(it) }
            //  videoExtractor.setDataSource(cmpPath)
            videoExtractor.selectTrack(0)
            val videoFormat: MediaFormat = videoExtractor.getTrackFormat(0)

//            val audioExtractor: MediaExtractor  = MediaExtractor()
//            audioExtractor.setDataSource(filePath!!)
//            audioExtractor.selectTrack(0)
//            val audioFormat: MediaFormat = audioExtractor.getTrackFormat(0)

            var muxer: MediaMuxer  = MediaMuxer(
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
            while (!sawEOS){
                videoBufferInfo.offset = offset
                videoBufferInfo.size = videoExtractor.readSampleData(videoBuf, offset)

                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0)
                {
                    sawEOS = true
                    videoBufferInfo.size = 0

                }else{
                    videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime()
                    videoBufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME
                    muxer.writeSampleData(videoTrack, videoBuf, videoBufferInfo)
                    videoExtractor.advance()

                    frameCount++
                }
            }

            var sawEOS2 = false
            var frameCount2 = 0
            while (!sawEOS2){
                frameCount2++

                audioBufferInfo.offset = offset
                audioBufferInfo.size = audioExtractor.readSampleData(audioBuf, offset)

                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0){
                    sawEOS2 = true
                    audioBufferInfo.size = 0
                }else{
                    audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime()
                    audioBufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                    muxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo)
                    audioExtractor.advance()
                }

            }

            muxer.stop()

            muxer.release()
            binding.mergedVideo.setVideoPath(outputFile)
            binding.mergedVideo.start()
            binding.playBtn.visibility = INVISIBLE


        }catch (e: IOException) {
            Timber.e(e)
        } catch (e:Exception) {
            Timber.e(e)
        }

    }
    fun toCheck(
        videoExtractor: MediaExtractor,
        audioFormat: MediaFormat,
        audioExtractor: MediaExtractor
    ) {
        try{

            videoDownPath?.let { videoExtractor.setDataSource(it) }
            //  videoExtractor.setDataSource(cmpPath)
            videoExtractor.selectTrack(0)
            val videoFormat: MediaFormat = videoExtractor.getTrackFormat(0)

//            val audioExtractor: MediaExtractor  = MediaExtractor()
//            audioExtractor.setDataSource(filePath!!)
//            audioExtractor.selectTrack(0)
//            val audioFormat: MediaFormat = audioExtractor.getTrackFormat(0)

            var muxer: MediaMuxer  = MediaMuxer(
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
            while (!sawEOS){
                videoBufferInfo.offset = offset
                videoBufferInfo.size = videoExtractor.readSampleData(videoBuf, offset)

                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0)
                {
                    sawEOS = true
                    videoBufferInfo.size = 0

                }else{
                    videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime()
                    videoBufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME
                    muxer.writeSampleData(videoTrack, videoBuf, videoBufferInfo)
                    videoExtractor.advance()

                    frameCount++
                }
            }

            var sawEOS2 = false
            var frameCount2 = 0
            while (!sawEOS2){
                frameCount2++

                audioBufferInfo.offset = offset
                audioBufferInfo.size = audioExtractor.readSampleData(audioBuf, offset)

                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0){
                    sawEOS2 = true
                    audioBufferInfo.size = 0
                }else{
                    audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime()
                    audioBufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                    muxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo)
                    audioExtractor.advance()
                }

            }

            muxer.stop()

            muxer.release()
            binding.mergedVideo.setVideoPath(outputFile)
            binding.mergedVideo.start()
            binding.playBtn.visibility = INVISIBLE

        }catch (e: IOException) {
            Timber.e(e)
        } catch (e:Exception) {
            Timber.e(e)
        }
    }

    fun processVideo(uri: List<Uri>){
        scope.launch {
            VideoCompressor.start(
                context = requireActivity().application,
                uris = uri,
                isStreamable = true,
                saveAt = Environment.DIRECTORY_MOVIES,
                listener = object : CompressionListener {

                    override fun onStart(index: Int) {
                        //showToast("started")
                    }

                    override fun onProgress(index: Int, percent: Float) {
                        //showToast("in progress")
                    }

                    override fun onSuccess(index: Int, size: Long, path: String?) {
                        //showToast("successful")
                        // videoDownPath = path
//                        scope.launch {
//                            AppObjectController.appDatabase.chatDao().insertCompressedVideo(
//                                CompressedVideo(currentLessonQuestion!!.questionId, path!!)
//                            )
//                        }
                    }

                    override fun onFailure(index: Int, failureMessage: String) {
                        //showToast("failed")
                    }

                    override fun onCancelled(index: Int) {
                        //showToast("canceled")
                    }

                },
                configureWith = Configuration(
                    quality = VideoQuality.MEDIUM,
                    isMinBitrateCheckEnabled = false,
                    disableAudio = false,
                    keepOriginalResolution = false,
                )
            )
        }
    }

    fun shareVideoForAudio(path: String){

        userReferralCode = Mentor.getInstance().referralCode
        val branchUniversalObject = BranchUniversalObject()
            .setCanonicalIdentifier(userReferralCode.plus(System.currentTimeMillis()))
            .setTitle("Invite Friend")
            .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
            .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
        val lp = LinkProperties()
            .setChannel(userReferralCode)
            .setFeature("sharing")
            .setCampaign("referral")
            .addControlParameter(Defines.Jsonkey.ReferralCode.key, userReferralCode)
            .addControlParameter(Defines.Jsonkey.UTMCampaign.key, "referral")
            .addControlParameter(
                Defines.Jsonkey.UTMMedium.key,
                userReferralCode.plus(System.currentTimeMillis())
            )
        branchUniversalObject
            .generateShortUrl(requireContext(), lp) { url, error ->
                if (error == null)
                    inviteFriends(
                        dynamicLink = url,
                        path = path
                    )
                else
                    inviteFriends(
                        dynamicLink = (if (PrefManager.hasKey(USER_SHARE_SHORT_URL))
                            PrefManager.getStringValue(USER_SHARE_SHORT_URL)
                        else
                            getAppShareUrl()),
                        path = path
                    )
            }

//            val destination = path!!
//            val waIntent = Intent(Intent.ACTION_SEND)
//            waIntent.type = "*/*"
//            waIntent.putExtra(Intent.EXTRA_TEXT, "")
//            waIntent.putExtra(
//                Intent.EXTRA_STREAM,
//                Uri.parse(destination)
//            )
//            waIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//            startActivity(Intent.createChooser(waIntent, "Share with"))

    }

    fun inviteFriends(dynamicLink: String, path: String) {
        viewModel.getDeepLink(
            dynamicLink,
            userReferralCode.plus(System.currentTimeMillis())
        )
        try {
            val destination = path!!
            val waIntent = Intent(Intent.ACTION_SEND)
            waIntent.type = "*/*"
            waIntent.putExtra(Intent.EXTRA_TEXT, dynamicLink)
            waIntent.putExtra(
                Intent.EXTRA_STREAM,
                Uri.parse(destination)
            )
            waIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(waIntent, "Share with"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getAppShareUrl(): String {
        return "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "&referrer=utm_source%3D$userReferralCode"
    }

    fun closeRecordedView(){
        binding.ivClose.setOnClickListener {
            if(binding.mergedVideo.isPlaying){
                binding.mergedVideo.stopPlayback()
            }
            binding.subAnswerLayout.visibility = GONE
            binding.videoLayout.visibility = GONE
            binding.practiseSubmitLayout.visibility = GONE
            binding.playBtn.visibility = GONE
            disableSubmitButton()
        }
    }

    fun playVideo(){
        binding.playBtn.visibility = INVISIBLE
        binding.mergedVideo.start()
    }

    fun playPracticeAudio() {
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
                } else if(EXPECTED_ENGAGE_TYPE.VI == it && isAudioRecordDone.not()){
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
                    if(it == EXPECTED_ENGAGE_TYPE.VI){
                        requestEngage.localPath = outputFile
                    }else{
                        requestEngage.localPath = filePath
                    }
//                    requestEngage.localPath = filePath
                    requestEngage.duration =
                        Utils.getDurationOfMedia(requireActivity(), filePath)?.toInt()
                    // requestEngage.feedbackRequire = currentLessonQuestion.feedback_require
                    requestEngage.questionId = currentLessonQuestion!!.id
                    requestEngage.mentor = Mentor.getInstance().getId()
                    if (it == EXPECTED_ENGAGE_TYPE.AU ) {
                        requestEngage.answerUrl = filePath
                    }else if(it == EXPECTED_ENGAGE_TYPE.VI){
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
        lessonActivityListener?.onNextTabCall(READING_POSITION)
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

}
