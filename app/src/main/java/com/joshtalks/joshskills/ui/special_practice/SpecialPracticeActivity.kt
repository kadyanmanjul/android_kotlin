package com.joshtalks.joshskills.ui.special_practice

import android.app.Activity
import android.app.DownloadManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.ActivityRecordVideoBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.BaseGroupActivity
import com.joshtalks.joshskills.ui.pdfviewer.CURRENT_VIDEO_PROGRESS_POSITION
import com.joshtalks.joshskills.ui.special_practice.utils.*
import com.joshtalks.joshskills.ui.special_practice.viewmodel.SpecialPracticeViewModel
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import timber.log.Timber

class SpecialPracticeActivity : BaseGroupActivity() {

    private val binding by lazy<ActivityRecordVideoBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_record_video)
    }

    private val spviewModel by lazy {
        ViewModelProvider(this).get(SpecialPracticeViewModel::class.java)
    }

    var openSampleVideoPlayerActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
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

    var openRecordVideoPlayerActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
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

    override fun setIntentExtras() {
        spviewModel.specialId.set(intent.getStringExtra(SPECIAL_ID))
    }

    override fun initViewBinding() {
        binding.vm = spviewModel
        binding.executePendingBindings()
    }

    override fun onCreated() {
        spviewModel.isVideoDownloadingStarted.set(true)
        spviewModel.getSpecialIdData(spviewModel.specialId.get() ?: EMPTY)

        if (spviewModel.specialId.get() != null) {
            val map = hashMapOf(
                Pair("mentor_id", Mentor.getInstance().getId()),
                Pair("special_practice_id", spviewModel.specialId.get() ?: EMPTY)
            )
            spviewModel.fetchSpecialPracticeData(map)
        }
    }

    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                K_FACTOR_ON_BACK_PRESSED -> popBackState()
                CALL_INVITE_FRIENDS_METHOD -> inviteFriends(it.obj as Intent)
                SHOW_RECORD_VIDEO -> spviewModel.showRecordedVideoUi(
                    it.obj as Boolean,
                    binding.videoPlayer,
                    spviewModel.recordedUrl
                )
                DOWNLOAD_VIDEO -> getPermissionAndDownloadFile(spviewModel.recordedUrl)
                SHOW_SAMPLE_VIDEO -> spviewModel.showRecordedVideoUi(
                    it.obj as Boolean,
                    binding.videoView,
                    spviewModel.videoUrl
                )
                SHOW_RECORDED_SPECIAL_VIDEO -> playRecordedVideo()
                SHOW_SAMPLE_SPECIAL_VIDEO -> playSampleVideo()
                CLOSE_SAMPLE_VIDEO -> closeIntroVideoPopUpUi()
                START_VIDEO_RECORDING -> startVideoRecording()
                DOWNLOAD_ID_DATA -> setDownloadId(it.obj as DownloadManager.Request)
                OPEN_VIEW_AND_SHARE -> openViewAndShare()
                MOVE_TO_ACTIVITY -> moveToNewActivity()
               // CHECK_DOWNLOAD_PERMISSION_EXIST -> checkDownloadPermissionExist()
            }
        }
    }

    private fun popBackState() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            try {
                supportFragmentManager.popBackStackImmediate()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        } else
            onBackPressed()
    }

    fun inviteFriends(waIntent: Intent) {
        try {
            startActivity(Intent.createChooser(waIntent, "Share with"))
        } catch (e: PackageManager.NameNotFoundException) {
            showToast(getString(R.string.whatsApp_not_installed))
        }
    }

    override fun onPause() {
        pauseVideo()
        super.onPause()
    }

    private fun closeIntroVideoPopUpUi() {
        spviewModel.isRecordButtonClick.set(true)
        spviewModel.isVideoPopUpShow.set(false)
        binding.videoView.onStop()
    }

    fun startVideoRecording() {
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
                                message = R.string.recording_start_permission_message
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
                                message(R.string.recording_start_permission_message)
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

    private fun getPermissionAndDownloadFile(videoUrl: String) {
        if (PermissionUtils.isStoragePermissionEnabled(this)) {
            spviewModel.downloadFile(videoUrl)
        } else {
            PermissionUtils.storageReadAndWritePermission(this,
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                spviewModel.downloadFile(videoUrl)
                                return

                            }
                            if (report.isAnyPermissionPermanentlyDenied) {
                                PermissionUtils.permissionPermanentlyDeniedDialog(this@SpecialPracticeActivity)
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
            return
        }
    }

    fun setDownloadId(request: DownloadManager.Request) {
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as (DownloadManager)
        spviewModel.downloadID.set(downloadManager.enqueue(request))
        registerDownloadReceiver()
    }

    fun registerDownloadReceiver() {
        registerReceiver(
            spviewModel.onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    private fun pauseVideo() {
        binding.videoView.onPause()
        binding.videoPlayer.onPause()
    }

    private fun openRecordingScreen() {
        try {
            spviewModel.isVideoPlay.set(false)
            pauseVideo()
            spviewModel.isRecordButtonClick.set(false)
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                val fragment = RecordVideoFragment()
                replace(R.id.parent_container, fragment, RECORD_VIEW_FRAGMENT)
                addToBackStack(K_FACTOR_STACK)
            }
        } catch (ex: Exception) {
            Timber.d(ex)
        }
    }

    fun openViewAndShare() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val fragment = ViewAndShareVideoFragment()
            replace(R.id.parent_container, fragment, VIEW_AND_SHARE_FRAGMENT)
            addToBackStack(K_FACTOR_STACK)
        }
    }

    fun playRecordedVideo() {
        val currentVideoProgressPosition = binding.videoPlayer.progress
        openRecordVideoPlayerActivity.launch(
            VideoPlayerActivity.getActivityIntent(
                this,
                EMPTY,
                null,
                spviewModel.recordedUrl,
                currentVideoProgressPosition,
                conversationId = getConversationId()
            )
        )
    }

    fun playSampleVideo() {
        val currentVideoProgressPosition = binding.videoView.progress
        openSampleVideoPlayerActivity.launch(
            VideoPlayerActivity.getActivityIntent(
                this,
                EMPTY,
                null,
                spviewModel.videoUrl,
                currentVideoProgressPosition,
                conversationId = getConversationId()
            )
        )
    }

    fun moveToNewActivity() {
        try {
            val i = Intent(this, SpecialPracticeActivity::class.java)
            i.putExtra(SPECIAL_ID, spviewModel.specialId.get())
            startActivity(i)
            overridePendingTransition(0, 0)
            finish()
        } catch (ex: Exception) {
        }
    }
}