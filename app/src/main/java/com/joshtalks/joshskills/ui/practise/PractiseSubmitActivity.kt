package com.joshtalks.joshskills.ui.practise

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.*
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.databinding.ActivityPraticeSubmitBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.eventbus.SeekBarProgressEventBus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.ui.extra.ImageShowFragment
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable
import java.util.concurrent.TimeUnit
import kotlin.random.Random

const val PRACTISE_OBJECT = "practise_object"
const val IMAGE_OR_VIDEO_SELECT_REQUEST_CODE = 1081
const val TEXT_FILE_ATTACHMENT_REQUEST_CODE = 1082

class PractiseSubmitActivity :
    WebRtcMiddlewareActivity(),
    Player.EventListener,
    AudioPlayerEventListener,
    ProgressUpdateListener {
    private var compositeDisposable = CompositeDisposable()

    private lateinit var binding: ActivityPraticeSubmitBinding
    private lateinit var chatModel: ChatModel
    private var sBound = false
    private var mUserIsSeeking = false
    private var isAudioRecordDone = false
    private var isVideoRecordDone = false
    private var isDocumentAttachDone = false
    private var scaleAnimation: Animation? = null
    private var startTime: Long = 0
    private var totalTimeSpend: Long = 0
    private var filePath: String? = null
    private lateinit var appAnalytics: AppAnalytics
    private var audioManager: ExoAudioPlayer? = null

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
        fun startPractiseSubmissionActivity(
            context: Activity,
            requestCode: Int,
            chatModel: ChatModel
        ) {
            val intent = Intent(context, PractiseSubmitActivity::class.java).apply {
                putExtra(PRACTISE_OBJECT, chatModel)
                //      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                //    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            context.startActivityForResult(intent, requestCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        if (intent.hasExtra(PRACTISE_OBJECT).not()) {
            this.finish()
        }
        totalTimeSpend = System.currentTimeMillis()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pratice_submit)
        binding.lifecycleOwner = this
        binding.handler = this
        chatModel = intent.getParcelableExtra(PRACTISE_OBJECT) as ChatModel
        scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale)
        appAnalytics = AppAnalytics.create(AnalyticsEvent.PRACTICE_SCREEN.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam("chatId", chatModel.chatId)
        initToolbarView()
        setPracticeInfoView()
        addObserver()
        chatModel.question?.run {
            if (this.practiceEngagement.isNullOrEmpty()) {
                binding.submitAnswerBtn.visibility = VISIBLE
                appAnalytics.addParam(AnalyticsEvent.PRACTICE_SOLVED.NAME, false)
                appAnalytics.addParam(AnalyticsEvent.PRACTICE_STATUS.NAME, "Not Submitted")
                setViewAccordingExpectedAnswer()
            } else {
                binding.practiseInputLayout.visibility = GONE
                binding.submitAnswerBtn.visibility = GONE
                appAnalytics.addParam(AnalyticsEvent.PRACTICE_SOLVED.NAME, true)
                appAnalytics.addParam(AnalyticsEvent.PRACTICE_STATUS.NAME, "Already Submitted")
                setViewUserSubmitAnswer()
            }
        }
    }

    override fun getConversationId(): String? {
        return chatModel.conversationId
    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
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
        } catch (ex: Exception) {
        }
        try {
            if (filePath.isNullOrEmpty().not()) {
                binding.videoPlayerSubmit.onPause()
            }
        } catch (ex: Exception) {
        }
    }

    override fun onStop() {
        appAnalytics.push()
        super.onStop()
        compositeDisposable.clear()
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
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
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
                    contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        // val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        cursor.moveToFirst()
                        val fileName = cursor.getString(nameIndex)
                        val file = AppDirectory.copy2(
                            it,
                            AppDirectory.getSentFile(fileName)
                        )
                        file?.run {
                            filePath = this.absolutePath
                            isDocumentAttachDone = true
                            binding.practiseInputLayout.visibility = GONE
                            binding.practiseSubmitLayout.visibility = VISIBLE
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
            RxBus2.listen(SeekBarProgressEventBus::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe(
                    {
                        Handler(Looper.getMainLooper()).post {
                            if (filePath.isNullOrEmpty().not() && currentAudio == filePath) {
                                binding.submitPractiseSeekbar.progress = it.progress
                            } else {
                                if (chatModel.question?.practiceEngagement != null && chatModel.question?.practiceEngagement?.getOrNull(
                                        0
                                    ) != null
                                ) {
                                    binding.submitPractiseSeekbar.progress = it.progress
                                } else {
                                    binding.practiseSeekbar.progress = it.progress
                                }
                            }
                        }
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
    }

    private fun initToolbarView() {
        val titleView = findViewById<AppCompatTextView>(R.id.text_message_title)
        chatModel.question?.title?.run {
            titleView.text = "Today's Practice"
            titleView.text = this
        }
        findViewById<View>(R.id.iv_back).visibility = VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            this@PractiseSubmitActivity.finish()
        }
    }

    private fun setPracticeInfoView() {
        chatModel.question?.run {
            appAnalytics.addParam(
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
                                .show(supportFragmentManager, "ImageShow")
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
                                this@PractiseSubmitActivity,
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
                                this@PractiseSubmitActivity,
                                pdfType.id,
                                EMPTY,
                                conversationId = chatModel.conversationId
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
                    binding.infoTv2.text =
                        HtmlCompat.fromHtml(this.qText!!, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    binding.infoTv2.visibility = VISIBLE
                }
            }
        }
    }

    private fun setViewAccordingExpectedAnswer() {
        chatModel.question?.run {
            binding.practiseInputLayout.visibility = VISIBLE
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
                        binding.uploadFileView.visibility = GONE
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
        practiceViewModel.requestStatusLiveData.observe(
            this,
            Observer {
                if (it) {
                    CoroutineScope(Dispatchers.IO).launch {
                        chatModel.question?.interval?.run {
                            WorkManagerAdmin.determineNPAEvent(NPSEvent.PRACTICE_COMPLETED, this)
                        }
                        delay(250)
                        val resultIntent = Intent().apply {
                            putExtra(PRACTISE_OBJECT, chatModel)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finishAndRemoveTask()
                    }
                } else {
                    binding.progressLayout.visibility = GONE
                }
            }
        )
    }

    private fun setViewUserSubmitAnswer() {
        chatModel.question?.run {
            this.expectedEngageType?.let {
                binding.practiseInputLayout.visibility = GONE
                binding.practiseSubmitLayout.visibility = VISIBLE
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
                        binding.submitAudioViewContainer.visibility = VISIBLE
                        filePath = practiseEngagement?.answerUrl
                        if (PermissionUtils.isStoragePermissionEnabled(this@PractiseSubmitActivity) && AppDirectory.isFileExist(
                                practiseEngagement?.localPath
                            )
                        ) {
                            filePath = practiseEngagement?.localPath
                            binding.submitPractiseSeekbar.max =
                                Utils.getDurationOfMedia(this@PractiseSubmitActivity, filePath!!)
                                ?.toInt() ?: 0
                        } else {
                            binding.submitPractiseSeekbar.max = practiseEngagement?.duration!!
                            if (binding.submitPractiseSeekbar.max == 0) {
                                binding.submitPractiseSeekbar.max = 1_00_000
                            }
                        }

                        initializePractiseSeekBar()
                        binding.ivCancel.visibility = GONE
                    }
                    EXPECTED_ENGAGE_TYPE.VI == it -> {
                        filePath = practiseEngagement?.answerUrl
                        binding.videoPlayerSubmit.visibility = VISIBLE

                        if (PermissionUtils.isStoragePermissionEnabled(this@PractiseSubmitActivity) && AppDirectory.isFileExist(
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
                                    this@PractiseSubmitActivity,
                                    null,
                                    null,
                                    filePath,
                                    conversationId = chatModel.conversationId
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

    private fun setImageInImageView(url: String, imageView: ImageView) {
        binding.progressBarImageView.visibility = VISIBLE

        Glide.with(applicationContext)
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
        binding.submitPractiseSeekbar.setOnSeekBarChangeListener(
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
                this,
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
                                PermissionUtils.cameraStoragePermissionPermanentlyDeniedDialog(this@PractiseSubmitActivity)
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
    }

    private fun uploadMedia() {
        PermissionUtils.cameraRecordStorageReadAndWritePermission(
            this,
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
                                this@PractiseSubmitActivity,
                                options
                            )
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.cameraStoragePermissionPermanentlyDeniedDialog(this@PractiseSubmitActivity)
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

    @SuppressLint("CheckResult")
    private fun selectVideoFromStorage() {
        val pickerConfig = MediaPickerConfig()
            .setUriPermanentAccess(true)
            .setAllowMultiSelection(false)
            .setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        MediaPicker.with(this, MediaPicker.MediaTypes.VIDEO)
            .setConfig(pickerConfig)
            .setFileMissingListener(object :
                    MediaPicker.MediaPickerImpl.OnMediaListener {
                    override fun onMissingFileWarning() {
                    }
                })
            .onResult()
            .subscribeOn(Schedulers.io())
            .subscribe(
                {
                    it.let {
                        it[0].path?.let { path ->
                            initVideoPractise(path)
                        }
                    }
                },
                {
                    it.printStackTrace()
                }
            )
    }

    @SuppressLint("CheckResult")
    private fun selectAudioFromStorage() {
        val pickerConfig = MediaPickerConfig()
            .setUriPermanentAccess(true)
            .setAllowMultiSelection(false)
            .setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        MediaPicker.with(this, MediaPicker.MediaTypes.AUDIO)
            .setConfig(pickerConfig)
            .setFileMissingListener(object :
                    MediaPicker.MediaPickerImpl.OnMediaListener {
                    override fun onMissingFileWarning() {
                    }
                })
            .onResult()
            .subscribe(
                {
                    it?.getOrNull(0)?.path?.let { audioFilePath ->
                        isAudioRecordDone = true
                        val tempPath = Utils.getPathFromUri(audioFilePath)
                        val recordUpdatedPath = AppDirectory.getAudioSentFile(tempPath).absolutePath
                        AppDirectory.copy(tempPath, recordUpdatedPath)
                        filePath = recordUpdatedPath
                        audioAttachmentInit()
                    }
                },
                {
                    it.printStackTrace()
                }
            )
    }

    private fun audioAttachmentInit() {
        binding.practiseSubmitLayout.visibility = VISIBLE
        binding.submitAudioViewContainer.visibility = VISIBLE
        initializePractiseSeekBar()
        binding.submitPractiseSeekbar.max = Utils.getDurationOfMedia(this, filePath!!)?.toInt() ?: 0
        enableSubmitButton()
        scrollToEnd()
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
            this,
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
                                this@PractiseSubmitActivity,
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
        binding.uploadPractiseView.setOnTouchListener { _, event ->
            if (PermissionUtils.isAudioAndStoragePermissionEnable(this).not()) {
                recordPermission()
                return@setOnTouchListener true
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.videoPlayer.onPause()

                    binding.rootView.requestDisallowInterceptTouchEvent(true)
                    binding.counterContainer.visibility = VISIBLE
                    binding.uploadPractiseView.startAnimation(scaleAnimation)
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    appAnalytics.addParam(AnalyticsEvent.AUDIO_RECORD.NAME, "Audio Recording")
                    // AppAnalytics.create(AnalyticsEvent.AUDIO_RECORD.NAME).push()
                    binding.counterTv.base = SystemClock.elapsedRealtime()
                    startTime = System.currentTimeMillis()
                    binding.counterTv.start()
                    val params =
                        binding.counterContainer.layoutParams as ViewGroup.MarginLayoutParams
                    params.topMargin = binding.rootView.scrollY
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
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
                        }
                    }
                }
            }

            true
        }
    }

    private fun setAudioPlayerStateDefault() {
        AppObjectController.uiHandler.post {
            binding.practiseSeekbar.progress = 0
            binding.submitPractiseSeekbar.progress = 0
            binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
            binding.btnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
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
        binding.practiseSubmitLayout.visibility = GONE
        binding.submitAudioViewContainer.visibility = GONE
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
        binding.practiseSubmitLayout.visibility = VISIBLE
        binding.videoPlayerSubmit.init()
        binding.videoPlayerSubmit.visibility = VISIBLE
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

    private fun scrollToEnd() {
        binding.rootView.post {
            binding.rootView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun checkIsPlayer(): Boolean {
        return audioManager != null
    }

    private fun isAudioPlaying(): Boolean {
        return this.checkIsPlayer() && this.audioManager!!.isPlaying()
    }

    private fun onPlayAudio(chatModel: ChatModel, audioObject: AudioType) {
        currentAudio = audioObject.audio_url
        val audioList = java.util.ArrayList<AudioType>()
        audioList.add(audioObject)
        audioManager = ExoAudioPlayer.getInstance()
        audioManager?.playerListener = this
        audioManager?.play(currentAudio!!)
        audioManager?.setProgressUpdateListener(this)
        if (filePath.isNullOrEmpty().not() && currentAudio == filePath) {
            binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Pause
        } else {
            binding.btnPlayInfo.state = MaterialPlayPauseDrawable.State.Pause
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        error.printStackTrace()
    }

    fun openAttachmentFile() {
        filePath?.let {
            Utils.openFile(this@PractiseSubmitActivity, it)
        }
    }

    fun removeFileAttachment() {
        filePath = null
        isDocumentAttachDone = false
        binding.practiseInputLayout.visibility = VISIBLE
        binding.practiseSubmitLayout.visibility = GONE
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
                    applicationContext,
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
                    applicationContext,
                    R.color.button_color
                )
            )
        }
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

                appAnalytics.addParam(
                    AnalyticsEvent.PRACTICE_SCREEN_TIME.NAME,
                    System.currentTimeMillis() - totalTimeSpend
                )
                appAnalytics.addParam(AnalyticsEvent.PRACTICE_SOLVED.NAME, true)
                appAnalytics.addParam(AnalyticsEvent.PRACTICE_STATUS.NAME, "Submitted")
                appAnalytics.addParam(
                    AnalyticsEvent.PRACTICE_TYPE_SUBMITTED.NAME,
                    "$it Practice Submitted"
                )
                appAnalytics.addParam(AnalyticsEvent.PRACTICE_SUBMITTED.NAME, "Submit Practice $")

                val requestEngage = RequestEngage()
                requestEngage.text = binding.etPractise.text.toString()
                requestEngage.localPath = filePath
                requestEngage.duration =
                    Utils.getDurationOfMedia(this@PractiseSubmitActivity, filePath)?.toInt()
                requestEngage.feedbackRequire = chatModel.question?.feedback_require
                requestEngage.questionId = chatModel.question?.questionId!!
                requestEngage.mentor = Mentor.getInstance().getId()
                if (it == EXPECTED_ENGAGE_TYPE.AU || it == EXPECTED_ENGAGE_TYPE.VI || it == EXPECTED_ENGAGE_TYPE.DX) {
                    requestEngage.answerUrl = filePath
                }
                binding.progressLayout.visibility = VISIBLE
                practiceViewModel.submitPractise(chatModel, requestEngage, engageType)
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        this@PractiseSubmitActivity.finishAndRemoveTask()
    }

    override fun onPlayerPause() {
        binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
    }

    override fun onPlayerResume() {
        binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Pause
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
        binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
        binding.submitPractiseSeekbar.progress = 0
        audioManager?.seekTo(0)
        audioManager?.onPause()
        audioManager?.setProgressUpdateListener(null)
    }

    override fun onProgressUpdate(progress: Long) {
        binding.submitPractiseSeekbar.progress = progress.toInt()
    }

    override fun onDurationUpdate(duration: Long?) {
        duration?.toInt()?.let { binding.submitPractiseSeekbar.max = it }
    }
}
