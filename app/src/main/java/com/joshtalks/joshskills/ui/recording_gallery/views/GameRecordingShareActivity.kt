package com.joshtalks.joshskills.ui.recording_gallery.views

import android.app.Activity
import android.content.Intent
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.ActivityGameRecordingShareBinding
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.pdfviewer.CURRENT_VIDEO_PROGRESS_POSITION
import com.joshtalks.joshskills.ui.recording_gallery.RecordingModel
import com.joshtalks.joshskills.ui.recording_gallery.viewmodels.RecordingGalleryViewModel
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import java.text.SimpleDateFormat
import java.util.*

class GameRecordingShareActivity : BaseActivity() {
    var recordModel:RecordingModel? = null
    var conversationId:String = ""
    private val binding by lazy<ActivityGameRecordingShareBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_game_recording_share)
    }

    val viewModel by lazy {
        ViewModelProvider(this)[RecordingGalleryViewModel::class.java]
    }

    var openVideoPlayerActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getLongExtra(
                CURRENT_VIDEO_PROGRESS_POSITION,
                0
            )?.let { progress ->
                binding.videoPayer.progress = progress
                binding.videoPayer.onResume()
            }
        }
    }

    override fun initViewBinding() {
    }

    override fun onCreated() {
        binding.vm = viewModel
        recordModel = intent.getSerializableExtra("record") as RecordingModel
    }

    override fun initViewState() {
        binding.duration.text = timeConversion(recordModel?.duration?.toLong())
        binding.time.text = getFormattedTime()
        binding.username.text = User.getInstance().firstName
        playRecordedVideo()
    }

    fun playRecordedVideo() {
        binding.videoPayer.visibility = View.VISIBLE
        binding.videoPayer.seekToStart()
        binding.videoPayer.setUrl(recordModel?.videoUrl)
        binding.videoPayer.onStart()
        playRecordedVideo111()
        binding.videoPayer.downloadStreamPlay()

        binding.videoPayer.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 15f)
            }
        }
        binding.videoPayer.clipToOutline = true
        binding.videoPayer.setFullScreenListener {
            val currentVideoProgressPosition = binding.videoPayer.progress
            openVideoPlayerActivity.launch(
                VideoPlayerActivity.getActivityIntent(
                    this,
                    EMPTY,
                    null,
                    recordModel?.videoUrl,
                    currentVideoProgressPosition,
                    conversationId
                )
            )
        }
    }

    private fun playRecordedVideo111() {
        binding.videoPayer.setFullScreenListener {
            val currentVideoProgressPosition = binding.videoPayer.progress
            openVideoPlayerActivity.launch(
                VideoPlayerActivity.getActivityIntent(
                    this,
                    EMPTY,
                    null,
                    recordModel?.videoUrl,
                    currentVideoProgressPosition,
                    conversationId
                )
            )
        }
    }

    private fun timeConversion(millie: Long?): String? {
        return if (millie != null) {
            val seconds = millie / 1000
            val sec = seconds % 60
            val min = seconds / 60 % 60
            val hrs = seconds / (60 * 60) % 24
            if (hrs > 0) {
                String.format("%02d:%02d:%02d", hrs, min, sec)
            } else {
                String.format("%02d:%02d", min, sec)
            }
        } else {
            null
        }
    }

    private fun getFormattedTime(): String? {
        val formatter =  SimpleDateFormat("h:mm a")
        return formatter.format( Date(recordModel?.timestamp?.time?:0))
    }

}