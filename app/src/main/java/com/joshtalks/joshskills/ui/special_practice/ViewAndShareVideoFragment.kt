package com.joshtalks.joshskills.ui.special_practice

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.Player
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.FragmentViewShareVideoBinding
import com.joshtalks.joshskills.ui.pdfviewer.CURRENT_VIDEO_PROGRESS_POSITION
import com.joshtalks.joshskills.ui.special_practice.base.BaseKFactorFragment
import com.joshtalks.joshskills.ui.special_practice.utils.CALL_INVITE_FRIENDS_METHOD
import com.joshtalks.joshskills.ui.special_practice.utils.PLAY_RECORDED_VIDEO
import com.joshtalks.joshskills.ui.special_practice.utils.convertImageFilePathIntoBitmap
import com.joshtalks.joshskills.ui.special_practice.viewmodel.SpecialPracticeViewModel
import com.joshtalks.joshskills.ui.special_practice.viewmodel.ViewAndShareViewModel
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity

class ViewAndShareVideoFragment : BaseKFactorFragment(), Player.EventListener {

    private val videoShareViewModel by lazy {
        ViewModelProvider(this).get(ViewAndShareViewModel::class.java)
    }

    val spViewModel by lazy {
        ViewModelProvider(requireActivity())[SpecialPracticeViewModel::class.java]
    }

    private lateinit var binding: FragmentViewShareVideoBinding

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentViewShareVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initViewBinding() {
        init()
        binding.vm = videoShareViewModel
        binding.executePendingBindings()
    }

    override fun initViewState() {
        liveData.observe(viewLifecycleOwner) {
            when (it.what) {
                CALL_INVITE_FRIENDS_METHOD -> inviteFriends(it.obj as Intent)
                PLAY_RECORDED_VIDEO -> playRecordedVideo()
            }
        }
    }

    override fun setArguments() {}
    override fun onBackPressed() {
        spViewModel.moveToActivity()
    }

    fun init() {
        if (isAdded)
            videoShareViewModel.addOverLayOnVideo(
                convertImageFilePathIntoBitmap(
                    spViewModel.imagePathForSetOnVideo.get() ?: EMPTY
                ),
                spViewModel,
                binding.videoView
            )
    }

    fun inviteFriends(waIntent: Intent) {
        try {
            startActivity(Intent.createChooser(waIntent, "Share with"))
        } catch (e: PackageManager.NameNotFoundException) {
            showToast(getString(R.string.whatsApp_not_installed))
        }
    }

    override fun onPause() {
        binding.videoView.onPause()
        super.onPause()
    }

    private fun playRecordedVideo() {
        binding.videoView.setPlayListener {
            val currentVideoProgressPosition = binding.videoView.progress
            openVideoPlayerActivity.launch(
                VideoPlayerActivity.getActivityIntent(
                    requireContext(),
                    EMPTY,
                    null,
                    videoShareViewModel.sharableVideoUrl.get(),
                    currentVideoProgressPosition,
                    getConversationId()
                )
            )
        }
    }
}