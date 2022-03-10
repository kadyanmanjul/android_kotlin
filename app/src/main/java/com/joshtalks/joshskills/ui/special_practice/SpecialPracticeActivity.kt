package com.joshtalks.joshskills.ui.special_practice

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Outline
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewOutlineProvider
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.ActivityRecordVideoBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.pdfviewer.CURRENT_VIDEO_PROGRESS_POSITION
import com.joshtalks.joshskills.ui.special_practice.model.SpecialPractice
import com.joshtalks.joshskills.ui.special_practice.viewmodel.SpecialPracticeViewModel
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class SpecialPracticeActivity : CoreJoshActivity() {
    lateinit var binding: ActivityRecordVideoBinding
    var specialPracticeViewModel: SpecialPracticeViewModel? = null
    var specialId: String? = null
    var videoDownloadPath: String? = null
    private var downloadID: Long = -1
    private var isVideoDownloadingStarted: Boolean = false
    private var isVideoDownloaded: MutableLiveData<Boolean> = MutableLiveData(false)

    var videoUrl = EMPTY
    var recordedUrl = EMPTY
    var wordInEnglish: String? = null
    var sentenceInEnglish: String? = null
    var wordInHindi: String? = null
    var sentenceInHindi: String? = null

    var openVideoPlayerActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getLongExtra(
                CURRENT_VIDEO_PROGRESS_POSITION,
                0
            )?.let { progress ->
                binding.videoView.progress = progress
                binding.videoView.onResume()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_record_video)
        binding.lifecycleOwner = this
        binding.handler = this
        specialId = intent.getStringExtra(SPECIAL_ID)
        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
        isVideoDownloadingStarted = true
        specialPracticeViewModel = ViewModelProvider(this).get(SpecialPracticeViewModel::class.java)

        if (specialId != null) {
            val map = hashMapOf<String, String>(
                Pair("mentor_id", Mentor.getInstance().getId()),
                Pair("special_practice_id", specialId ?: EMPTY)
            )
            specialPracticeViewModel?.fetchSpecialPracticeData(map)
        }

        this.let {
            specialPracticeViewModel?.specialPracticeData?.observe(it) {
                setData(it.specialPractice)
                binding.textMessageTitle.text =
                    "Special Practice - ${it.specialPractice?.practiceNo}"
                videoUrl = it.specialPractice?.sampleVideoUrl ?: EMPTY
                recordedUrl = it.recordedVideoUrl ?: EMPTY
                if (recordedUrl != EMPTY) {
                    showRecordedVideoUi()
                }
            }
        }

        binding.cardSampleVideoPlay.setOnClickListener {
            showIntroVideoUi()
        }

        binding.btnRecord.setOnClickListener {
            startVideoRecording()
        }
    }

    private fun addObserver() {
        isVideoDownloaded.observe(this, Observer {
            if (it) {

            }
        })
    }
    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount
        if (count == 0) {
            super.onBackPressed()
            // additional code
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    private fun openRecordingScreen() {
        try {
            binding.videoView.onPause()
            binding.videoPlayer.onPause()
            binding.card3.visibility = View.GONE
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.parent_container,
                    RecordVideoFragment.newInstance(
                        wordInEnglish ?: EMPTY,
                        sentenceInEnglish ?: EMPTY,
                        wordInHindi ?: EMPTY,
                        sentenceInHindi ?: EMPTY,
                        specialId ?: EMPTY
                    ),
                    "Special"
                ).commit()
        } catch (ex: Exception) {
        }
    }

    private fun setData(specialPractice: SpecialPractice?) {
       // getPermissionAndDownloadFile(recordedUrl)
        binding.wordText.text = specialPractice?.wordText
        binding.instructionText.text = specialPractice?.instructionText
        wordInEnglish = specialPractice?.wordEnglish
        sentenceInEnglish = specialPractice?.sentenceEnglish
        wordInHindi = specialPractice?.wordHindi
        sentenceInHindi = specialPractice?.sentenceHindi
    }

    override fun onPause() {
        binding.videoView.onPause()
        binding.videoPlayer.onPause()
        super.onPause()
    }

    companion object {
        private const val SPECIAL_ID = "SPECIAL_ID"

        @JvmStatic
        fun start(
            context: Context,
            conversationId: String? = null,
            specialId: String?
        ) {
            Intent(context, SpecialPracticeActivity::class.java).apply {
                putExtra(SPECIAL_ID, specialId)
                putExtra(CONVERSATION_ID, conversationId)
            }.run {
                context.startActivity(this)
            }
        }
    }

    private fun showIntroVideoUi() {
        try {
            binding.btnRecord.isClickable = false
            binding.btnRecord.isEnabled = false
            binding.videoPopup.visibility = View.VISIBLE
            binding.videoView.seekToStart()
            binding.videoView.setUrl(videoUrl)
            binding.videoPlayer.fitToScreen()
            binding.videoView.onStart()
            binding.videoView.setPlayListener {
                val currentVideoProgressPosition = binding.videoView.progress
                openVideoPlayerActivity.launch(
                    VideoPlayerActivity.getActivityIntent(
                        this,
                        EMPTY,
                        null,
                        videoUrl,
                        currentVideoProgressPosition,
                        conversationId = getConversationId()
                    )
                )
            }

            lifecycleScope.launchWhenStarted {
                binding.videoView.downloadStreamPlay()
            }

            binding.imageViewClose.setOnClickListener {
                closeIntroVideoPopUpUi()
            }

            binding.videoView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 15f)
                }
            }
            binding.videoView.clipToOutline = true
        } catch (ex: Exception) {
        }
    }

    private fun closeIntroVideoPopUpUi() {
        binding.btnRecord.isClickable = true
        binding.btnRecord.isEnabled = true
        binding.videoPopup.visibility = View.GONE
        binding.videoView.onStop()
    }

    private fun showRecordedVideoUi() {
        try {
            binding.card3.visibility = View.VISIBLE
            binding.videoPlayer.seekToStart()
            binding.videoPlayer.setUrl(recordedUrl)
            binding.videoPlayer.onStart()
            binding.videoPlayer.fitToScreen()
            binding.videoPlayer.setPlayListener {
                val currentVideoProgressPosition = binding.videoPlayer.progress
                openVideoPlayerActivity.launch(
                    VideoPlayerActivity.getActivityIntent(
                        this,
                        EMPTY,
                        null,
                        recordedUrl,
                        currentVideoProgressPosition,
                        conversationId = getConversationId()
                    )
                )
            }

            lifecycleScope.launchWhenStarted {
                binding.videoPlayer.downloadStreamButNotPlay()
            }

            binding.videoPlayer.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 15f)
                }
            }
            binding.videoPlayer.clipToOutline = true
        } catch (ex: Exception) {
        }
    }

    private fun startVideoRecording() {
        if (PermissionUtils.isCameraPermissionEnabled(this)) {
            if (Utils.isInternetAvailable()) {
                openRecordingScreen()
                return
            } else {
                showToast(getString(R.string.internet_not_available_msz))
            }
        }
        PermissionUtils.cameraRecordStorageReadAndWritePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.cameraStoragePermissionPermanentlyDeniedDialog(
                                this@SpecialPracticeActivity,
                                message = R.string.call_start_permission_message
                            )
                            return
                        }
                        if (flag) {
                            if (Utils.isInternetAvailable()) {
                                openRecordingScreen()
                                return
                            } else {
                                showToast(getString(R.string.internet_not_available_msz))
                            }
                        } else {
                            MaterialDialog(this@SpecialPracticeActivity).show {
                                message(R.string.call_start_permission_message)
                                positiveButton(R.string.ok)
                            }
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

//    private fun getPermissionAndDownloadFile(videoUrl: String) {
//        if (PermissionUtils.isStoragePermissionEnabled(this)) {
//            downloadFile(videoUrl)
//        } else {
//            PermissionUtils.storageReadAndWritePermission(this,
//                object : MultiplePermissionsListener {
//                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
//                        report?.areAllPermissionsGranted()?.let { flag ->
//                            if (flag) {
//                                downloadFile(videoUrl)
//                                return
//
//                            }
//                            if (report.isAnyPermissionPermanentlyDenied) {
//                                PermissionUtils.permissionPermanentlyDeniedDialog(this@SpecialPracticeActivity)
//                                return
//                            }
//                        }
//                    }
//
//                    override fun onPermissionRationaleShouldBeShown(
//                        permissions: MutableList<PermissionRequest>?,
//                        token: PermissionToken?
//                    ) {
//                        token?.continuePermissionRequest()
//                    }
//                })
//            return
//        }
//    }
//
//    fun getVideoFilePath(): String {
//        return getAndroidMoviesFolder()?.absolutePath + "/" + "Recorded_video" + "filter_apply.mp4"
//    }
//
//    fun getAndroidMoviesFolder(): File? {
//        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//    }
//    protected fun downloadFile(
//        url: String,
//        message: String = "Downloading file",
//        title: String = "Josh Skills"
//    ) {
//        lifecycleScope.launch(Dispatchers.IO) {
//            var fileName = getVideoFilePath()
////            if (fileName.isEmpty()) {
////                url.let {
////                    fileName = it + Random(5).nextInt().toString().plus(it.getExtension())
////                }
////            }
//            videoDownloadPath = fileName
//            registerDownloadReceiver(fileName)
//
//            val env = Environment.DIRECTORY_DOWNLOADS
//
//            val request: DownloadManager.Request =
//                DownloadManager.Request(Uri.parse(url))
//                    .setTitle(title)
//                    .setDescription(message)
//                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
//                    .setAllowedOverMetered(true)
//                    .setAllowedOverRoaming(true)
//                    .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
//                    .setDestinationInExternalPublicDir(env, fileName)
//
//            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                request.setRequiresCharging(false).setRequiresDeviceIdle(false)
//            }
//
//            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as (DownloadManager)
//            downloadID = downloadManager.enqueue(request)
//        }
//    }
//
//    private fun registerDownloadReceiver(fileName: String) {
//        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
//    }
//
//    protected var onDownloadComplete = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
//            if (downloadID == id) {
//                try {
//                    videoDownloadPath
//                    CoroutineScope(Dispatchers.IO).launch {
//                        AppObjectController.appDatabase.specialDao().updateRecordedTable(specialId?: EMPTY,videoDownloadPath?: EMPTY)
//                    }
//
//                    if (isVideoDownloadingStarted.not()) {
//                        showToast(getString(R.string.downloading_complete))
//                    } else {
//                        isVideoDownloaded.postValue(true)
//                    }
//                    isVideoDownloadingStarted = false
//                } catch (Ex: Exception) {
//                    showToast(getString(R.string.something_went_wrong))
//                }
//
//            }
//        }
//    }
}