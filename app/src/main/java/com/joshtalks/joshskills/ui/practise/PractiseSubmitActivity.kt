package com.joshtalks.joshskills.ui.practise

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.crashlytics.android.Crashlytics
import com.google.android.exoplayer2.util.Log
import com.greentoad.turtlebody.mediapicker.MediaPicker
import com.greentoad.turtlebody.mediapicker.core.MediaPickerConfig
import com.joshtalks.appcamera.VideoTrimmerActivity.Companion.startTrimmerActivity
import com.joshtalks.appcamera.pix.JoshCameraActivity
import com.joshtalks.appcamera.pix.Options
import com.joshtalks.appcamera.utility.ImageQuality
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.playback.MusicNotificationManager
import com.joshtalks.joshskills.core.playback.MusicService
import com.joshtalks.joshskills.core.playback.PlaybackInfoListener
import com.joshtalks.joshskills.core.playback.PlayerInterface
import com.joshtalks.joshskills.databinding.ActivityPraticeSubmitBinding
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.EXPECTED_ENGAGE_TYPE
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.ui.extra.ImageShowFragment
import com.joshtalks.joshskills.ui.video_player.FullScreenVideoFragment
import com.joshtalks.joshskills.ui.view_holders.IMAGE_SIZE
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.muddzdev.styleabletoast.StyleableToast
import io.reactivex.Observer
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.random.Random


const val PRACTISE_OBJECT = "practise_object"

const val IMAGE_OR_VIDEO_SELECT_REQUEST_CODE = 1081
const val TEXT_FILE_ATTACHMENT_REQUEST_CODE = 1082
private const val VIDEO_COMPRESS = 125


class PractiseSubmitActivity : CoreJoshActivity(), FullScreenVideoFragment.OnDismissListener {
    private var compositeDisposable = CompositeDisposable()

    private lateinit var binding: ActivityPraticeSubmitBinding
    private lateinit var chatModel: ChatModel
    private var mPlayerInterface: PlayerInterface? = null
    private var mMusicService: MusicService? = null
    private var mPlaybackListener: PlaybackListener? = null
    private var sBound = false
    private var mUserIsSeeking = false
    private var mMusicNotificationManager: MusicNotificationManager? = null
    private var isAudioRecordDone = false
    private var isVideoRecordDone = false
    private var isImageAttachmentDone = false
    private var isDocumentAttachDone = false
    private var scaleAnimation: Animation? = null
    private var startTime: Long = 0
    private var filePath: String? = null
    private var currentAudio: String? = null


    val DOCX_FILE_MIME_TYPE = arrayOf(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/msword", "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )

    private val practiseViewModel: PractiseViewModel by lazy {
        ViewModelProviders.of(this).get(PractiseViewModel::class.java)
    }


    companion object {
        fun startPractiseSubmissionActivity(
            context: Activity,
            requestCode: Int,
            chatModel: ChatModel
        ) {
            val intent = Intent(context, PractiseSubmitActivity::class.java).apply {
                putExtra(PRACTISE_OBJECT, chatModel)
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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pratice_submit)
        binding.lifecycleOwner = this
        binding.handler = this
        chatModel = intent.getSerializableExtra(PRACTISE_OBJECT) as ChatModel
        scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale)

        initToolbarView()
        setPracticeInfoView()
        setViewAccordingExpectedAnswer()
        doBindService()

    }


    override fun onResume() {
        super.onResume()
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        if (mPlayerInterface != null && mPlayerInterface!!.isMediaPlayer) {
            mPlayerInterface?.onResumeActivity()
        }
        try {
            binding.videoPlayer.onResume()
        } catch (ex: Exception) {

        }
    }

    override fun onPause() {
        super.onPause()
        if (mPlayerInterface != null && mPlayerInterface!!.isMediaPlayer) {
            mPlayerInterface?.onPauseActivity()
        }
        try {
            binding.videoPlayer.onPause()
        } catch (ex: Exception) {

        }
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
        try {
            binding.videoPlayer.onStop()
        } catch (ex: Exception) {
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        mPlaybackListener = null
        doUnbindService()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, returnIntent: Intent?) {

        try {
            if (requestCode == IMAGE_OR_VIDEO_SELECT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                returnIntent?.let { intent ->
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
                            filePath = videoPath
                        }
                        else -> return
                    }
                }
            } else if (requestCode == TEXT_FILE_ATTACHMENT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                returnIntent?.data?.let {
                    val path = Utils.getRealPathFromURI(it)
                    filePath = path
                    isDocumentAttachDone = true
                    Log.e("path", path)
                }

            } else if (VIDEO_COMPRESS == TEXT_FILE_ATTACHMENT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                Utils.printAllIntent(intent)
                returnIntent?.data?.let {
                    val path = Utils.getRealPathFromURI(it)
                    filePath = path
                    isVideoRecordDone = true
                    Log.e("path", path)
                }

            }

        } catch (ex: Exception) {
            Crashlytics.logException(ex)
            ex.printStackTrace()
        }

        super.onActivityResult(requestCode, resultCode, returnIntent)

    }

    override fun onDismiss() {
        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (ex: Exception) {
        }
    }

    private fun initToolbarView() {
        val titleView = findViewById<AppCompatTextView>(R.id.text_message_title)
        chatModel.question?.title?.run {
            titleView.text = this
        }
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            this@PractiseSubmitActivity.finish()
        }
    }

    private fun setPracticeInfoView() {
        chatModel.question?.run {
            when (this.material_type) {
                BASE_MESSAGE_TYPE.AU -> {
                    binding.audioViewContainer.visibility = VISIBLE
                    this.audioList?.getOrNull(0)?.audio_url?.let {
                        binding.btnPlayInfo.tag = it
                        binding.practiseSeekbar.max = this.audioList?.getOrNull(0)?.duration!!
                        initializePractiseSeekBar()
                    }
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
                        binding.videoPlayer.downloadStreamPlay()
                        binding.videoPlayer.fitToScreen()

                    }
                    binding.videoPlayer.setOnClickListener {
                        FullScreenVideoFragment.newInstance(this.videoList?.getOrNull(0)?.video_url!!)
                            .show(supportFragmentManager, "Payment Process")
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
                this.qText?.let {
                    binding.practiseTextInfoLayout.visibility = VISIBLE
                    binding.infoTv2.text = HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    binding.infoTv2.visibility = VISIBLE
                }
            }

        }
    }


    private fun setViewAccordingExpectedAnswer() {
        chatModel.question?.run {
            binding.practiseInputLayout.visibility = VISIBLE
            this.expectedEngageType?.let {
                when {
                    EXPECTED_ENGAGE_TYPE.TX == it -> {
                        binding.etPractise.visibility = VISIBLE
                    }
                    EXPECTED_ENGAGE_TYPE.AU == it -> {
                        binding.uploadPractiseView.setImageResource(R.drawable.recv_ic_mic_white)
                        setupRecordView()
                    }
                    EXPECTED_ENGAGE_TYPE.VI == it -> {
                        binding.uploadPractiseView.setImageResource(R.drawable.ic_videocam)
                        setupFileUploadListener(it)
                    }
                    EXPECTED_ENGAGE_TYPE.IM == it -> {
                        binding.uploadPractiseView.setImageResource(R.drawable.ic_camera)
                        setupFileUploadListener(it)
                    }
                    EXPECTED_ENGAGE_TYPE.DX == it -> {
                        binding.uploadPractiseView.setImageResource(R.drawable.ic_file_upload)
                        setupFileUploadListener(it)
                    }
                }

                if ((it == EXPECTED_ENGAGE_TYPE.TX).not()) {
                    binding.uploadPractiseView.visibility = VISIBLE
                    binding.uploadFileView.visibility = VISIBLE
                }
            }
        }
    }


    private fun setImageInImageView(url: String, imageView: ImageView) {
        val multi = MultiTransformation<Bitmap>(
            CropTransformation(
                Utils.dpToPx(IMAGE_SIZE),
                Utils.dpToPx(IMAGE_SIZE),
                CropTransformation.CropType.CENTER
            ),
            RoundedCornersTransformation(
                Utils.dpToPx(ROUND_CORNER),
                0,
                RoundedCornersTransformation.CornerType.ALL
            )
        )

        Glide.with(applicationContext)
            .load(url)
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(RequestOptions.bitmapTransform(multi))
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
                    //mSongPosition.text = Utils.formatDuration(progress)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    if (mUserIsSeeking) {
                        //  mSongPosition.setTextColor(currentPositionColor)
                    }
                    mUserIsSeeking = false
                    mPlayerInterface?.seekTo(userSelectedPosition)
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
                    //mSongPosition.text = Utils.formatDuration(progress)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    if (mUserIsSeeking) {
                        //  mSongPosition.setTextColor(currentPositionColor)
                    }
                    mUserIsSeeking = false
                    mPlayerInterface?.seekTo(userSelectedPosition)
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
            PermissionUtils.cameraRecordStorageReadAndWritePermission(this,
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                if (EXPECTED_ENGAGE_TYPE.VI == expectedEngageType) {
                                    selectVideoFromStorage()
                                } else if (EXPECTED_ENGAGE_TYPE.AU == expectedEngageType) {
                                    selectAudioFromStorage()
                                }/*else if (EXPECTED_ENGAGE_TYPE.IM == expectedEngageType) {
                                    selectImageFromStorage()
                                }*/
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
                })


        }

    }


    private fun uploadMedia() {
        PermissionUtils.cameraRecordStorageReadAndWritePermission(this,
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

                            JoshCameraActivity.start(this@PractiseSubmitActivity, options)
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
            })
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
            .subscribe(object : Observer<ArrayList<Uri>> {
                override fun onSubscribe(@NonNull d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(@NonNull e: Throwable) {
                    e.printStackTrace()
                }

                override fun onComplete() {
                }

                override fun onNext(it: ArrayList<Uri>) {
                    it.let {
                        it[0].path?.let { path ->
                            filePath = path
                            startTrimmerActivity(
                                this@PractiseSubmitActivity,
                                VIDEO_COMPRESS,
                                Uri.fromFile(File(path)),
                                File(path),
                                File(path).absolutePath
                            )
                        }
                    }
                }
            })
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

            .subscribe(object : Observer<ArrayList<Uri>> {
                override fun onSubscribe(@NonNull d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onError(@NonNull e: Throwable) {
                    e.printStackTrace()
                }

                override fun onComplete() {
                }

                override fun onNext(it: ArrayList<Uri>) {
                    it.let {
                        it[0].path?.let { path ->

                            isAudioRecordDone = true
                            filePath = AppDirectory.getAudioSentFile(null).absolutePath
                            AppDirectory.copy(path, filePath!!)
                            binding.practiseSubmitLayout.visibility = VISIBLE
                            binding.submitAudioViewContainer.visibility = VISIBLE
                            Log.e("audio path", filePath!!)
                        }
                    }
                }
            })
    }


    private fun uploadTextFileChooser() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, DOCX_FILE_MIME_TYPE)
        startActivityForResult(intent, TEXT_FILE_ATTACHMENT_REQUEST_CODE)
    }


    private fun setupRecordView() {

        if (PermissionUtils.isAudioAndStoragePermissionEnable(this)) {
            audioRecordTouchListener()
        } else {
            binding.uploadPractiseView.setOnClickListener {
                PermissionUtils.audioRecordStorageReadAndWritePermission(this,
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
                    })
            }
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun audioRecordTouchListener() {
        binding.uploadPractiseView.setOnTouchListener { _, event ->
            Log.e("event", "" + event.action)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.uploadPractiseView.startAnimation(scaleAnimation)
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    AppAnalytics.create(AnalyticsEvent.AUDIO_BUTTON_CLICKED.NAME)
                        .push()
                    binding.counterTv.base = SystemClock.elapsedRealtime()
                    startTime = System.currentTimeMillis()
                    binding.counterTv.start()
                    binding.counterTv.visibility = VISIBLE
                    practiseViewModel.startRecord()
                }

                MotionEvent.ACTION_MOVE -> {

                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    binding.counterTv.stop()
                    practiseViewModel.stopRecording()
                    binding.uploadPractiseView.clearAnimation()
                    binding.counterTv.visibility = GONE
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                    val timeDifference =
                        TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - TimeUnit.MILLISECONDS.toSeconds(
                            startTime
                        )
                    if (timeDifference > 1) {
                        practiseViewModel.recordFile.let {
                            isAudioRecordDone = true
                            filePath = AppDirectory.getAudioSentFile(null).absolutePath
                            AppDirectory.copy(it.absolutePath, filePath!!)
                            Log.e("comple", filePath!!)
                            binding.practiseSubmitLayout.visibility = VISIBLE
                            binding.submitAudioViewContainer.visibility = VISIBLE
                        }
                    }
                }
            }

            true
        }
    }


    private fun doBindService() {
        bindService(
            Intent(this, MusicService::class.java),
            mConnection,
            Context.BIND_AUTO_CREATE
        )
        sBound = true
        val startNotStickyIntent = Intent(this, MusicService::class.java)
        startService(startNotStickyIntent)
    }

    private val mConnection: ServiceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
                mMusicService = (iBinder as MusicService.LocalBinder).instance
                mPlayerInterface = mMusicService?.mediaPlayerHolder
                mMusicNotificationManager = mMusicService?.musicNotificationManager
                mMusicNotificationManager?.setAccentColor(R.color.colorAccent)
                if (mPlaybackListener == null) {
                    mPlaybackListener = PlaybackListener()
                    mPlayerInterface?.setPlaybackInfoListener(mPlaybackListener)
                }
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                mMusicService = null
            }
        }

    private fun doUnbindService() {
        if (sBound) {
            unbindService(mConnection)
            sBound = false
        }
    }


    private fun updatePlayingInfo(restore: Boolean, startPlay: Boolean) {
        try {
            if (startPlay) {
                mPlayerInterface?.mediaPlayer?.start()
                Handler().postDelayed({
                    mMusicService!!.startForeground(
                        MusicNotificationManager.NOTIFICATION_ID,
                        mMusicNotificationManager!!.createNotification()

                    )
                }, 250)
            }
            if (restore) {
                if (filePath.isNullOrEmpty().not() && currentAudio == filePath) {
                    binding.submitPractiseSeekbar.progress = mPlayerInterface!!.playerPosition
                } else {
                    binding.practiseSeekbar.progress = mPlayerInterface!!.playerPosition
                }

                updateResetStatus(false)
                Handler().postDelayed({
                    //stop foreground if coming from pause state
                    if (mMusicService!!.isRestoredFromPause) {
                        mMusicService!!.stopForeground(false)
                        mMusicService!!.musicNotificationManager.notificationManager.notify(
                            MusicNotificationManager.NOTIFICATION_ID,
                            mMusicService!!.musicNotificationManager.notificationBuilder.build()
                        )
                        mMusicService!!.isRestoredFromPause = false
                    }
                }, 250)
            }
        } catch (ex: Exception) {
        }
    }

    private fun updateResetStatus(onPlaybackCompletion: Boolean) {
        if (onPlaybackCompletion) {
            if (mPlayerInterface!!.state != PlaybackInfoListener.State.COMPLETED && mPlayerInterface!!.isPlaying) {
                mPlayerInterface?.resumeOrPause()
                mPlayerInterface?.reset()
            }
        }
    }

    fun playPracticeAudio() {

        if (Utils.getCurrentMediaVolume(applicationContext) <= 0) {
            StyleableToast.Builder(applicationContext).gravity(Gravity.BOTTOM)
                .text(getString(R.string.volume_up_message)).cornerRadius(16)
                .length(Toast.LENGTH_LONG)
                .solidBackground().show()
        }

        if (currentAudio == null) {
            onPlayAudio(chatModel, chatModel.question?.audioList?.getOrNull(0)!!)
        } else {
            if (currentAudio == chatModel.question?.audioList?.getOrNull(0)?.audio_url) {
                if (checkIsPlayer()) {
                    mPlayerInterface?.resumeOrPause()
                } else {
                    onPlayAudio(chatModel, chatModel.question?.audioList?.getOrNull(0)!!)
                }
            } else {
                onPlayAudio(chatModel, chatModel.question?.audioList?.getOrNull(0)!!)
            }
        }
    }

    fun playSubmitPracticeAudio() {
        val audioType = AudioType()
        audioType.audio_url = filePath!!
        audioType.downloadedLocalPath = filePath!!
        audioType.duration = Utils.getDurationOfMedia(this, filePath!!)!!.toInt()
        audioType.id = Random.nextInt().toString()

        if (currentAudio == null) {
            onPlayAudio(chatModel, audioType)
        } else {
            if (currentAudio == audioType.audio_url) {
                if (checkIsPlayer()) {
                    mPlayerInterface?.resumeOrPause()
                } else {
                    onPlayAudio(chatModel, audioType)
                }
            } else {
                onPlayAudio(chatModel, audioType)

            }

        }


    }


    private fun checkIsPlayer(): Boolean {
        return this.mPlayerInterface != null && mPlayerInterface!!.isMediaPlayer
    }

    fun isAudioPlaying(): Boolean {
        return this.checkIsPlayer() && this.mPlayerInterface!!.isPlaying
    }

    private fun onPlayAudio(chatModel: ChatModel, audioObject: AudioType) {
        currentAudio = audioObject.audio_url
        val audioList = java.util.ArrayList<AudioType>()
        audioList.add(audioObject)
        mPlayerInterface?.setCurrentSong(null, chatModel, audioObject, audioList)
        mPlayerInterface?.initMediaPlayer(chatModel, audioObject)

        if (filePath.isNullOrEmpty().not() && currentAudio == filePath) {

        } else {
            binding.btnPlayInfo.state = MaterialPlayPauseDrawable.State.Pause
        }

    }

    fun submitPractise() {
        if (Utils.isInternetAvailable().not()) {
            Toast.makeText(
                AppObjectController.joshApplication,
                getString(R.string.internet_not_available_msz),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (chatModel.question != null && chatModel.question!!.expectedEngageType != null) {
            chatModel.question?.expectedEngageType?.let {
                if (EXPECTED_ENGAGE_TYPE.TX == it && binding.etPractise.text.isNullOrEmpty()) {
                    return
                } else if (EXPECTED_ENGAGE_TYPE.AU == it && isAudioRecordDone.not()) {
                    return
                } else if (EXPECTED_ENGAGE_TYPE.VI == it && isVideoRecordDone.not()) {
                    return
                } else if (EXPECTED_ENGAGE_TYPE.DX == it && isDocumentAttachDone.not()) {
                    return
                }

                val requestEngage = RequestEngage()
                requestEngage.text = binding.etPractise.text.toString()
                requestEngage.localPath = filePath
                requestEngage.feedbackRequire = chatModel.question?.feedback_require
                requestEngage.question = chatModel.question?.questionId!!
                requestEngage.mentor = Mentor.getInstance().getId()
                if (it == EXPECTED_ENGAGE_TYPE.AU || it == EXPECTED_ENGAGE_TYPE.VI || it == EXPECTED_ENGAGE_TYPE.DX) {
                    requestEngage.answerUrl = filePath
                }
                practiseViewModel.submitPractise(requestEngage)
            }
        }
    }

    inner class PlaybackListener : PlaybackInfoListener() {
        override fun onPositionChanged(position: Int) {
        }

        override fun onStateChanged(@State state: Int) {
            try {
                if (mPlayerInterface!!.state != State.RESUMED && mPlayerInterface!!.state != State.PAUSED) {
                    updatePlayingInfo(restore = false, startPlay = true)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        override fun onPlaybackCompleted() {
            updateResetStatus(true)
        }

        override fun onPlaybackStop() {

        }
    }

}
