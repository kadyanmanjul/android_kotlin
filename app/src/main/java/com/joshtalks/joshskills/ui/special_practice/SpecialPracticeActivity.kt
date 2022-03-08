package com.joshtalks.joshskills.ui.special_practice

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Outline
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.ActivityRecordVideoBinding
import com.joshtalks.joshskills.repository.local.dao.ChatDao
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LinkAttribution
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.pdfviewer.CURRENT_VIDEO_PROGRESS_POSITION
import com.joshtalks.joshskills.ui.referral.REFERRAL_SHARE_TEXT_SHARABLE_VIDEO
import com.joshtalks.joshskills.ui.referral.USER_SHARE_SHORT_URL
import com.joshtalks.joshskills.ui.special_practice.model.SpecialPractice
import com.joshtalks.joshskills.ui.special_practice.viewmodel.SpecialPracticeViewModel
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Defines
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import kotlin.collections.HashMap

class SpecialPracticeActivity : CoreJoshActivity() {
    lateinit var binding: ActivityRecordVideoBinding
    var specialPracticeViewModel: SpecialPracticeViewModel? = null
    var specialId = 0

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
       // specialId = intent.getIntExtra(SPECIAL_ID, 0)
        specialPracticeViewModel = ViewModelProvider(this).get(SpecialPracticeViewModel::class.java)

        specialPracticeViewModel?.getSpecialId()

        this.let {
            specialPracticeViewModel?.specialIdData?.observe(it) {
                specialId = it.id?:0
                if (specialId!=0){
                    val map = hashMapOf<String, Any>(
                        Pair("mentor_id", Mentor.getInstance().getId()),
                        Pair("special_practice_id", specialId)
                    )
                    specialPracticeViewModel?.fetchSpecialPracticeData(map)
            }
        }

            this.let {
                specialPracticeViewModel?.specialPracticeData?.observe(it) {
                    setData(it.specialPractice)
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
                binding.card3.visibility = View.GONE
                supportFragmentManager.beginTransaction()
                    .replace(R.id.parent_container, RecordVideoFragment.newInstance(wordInEnglish?:EMPTY,sentenceInEnglish?:EMPTY,wordInHindi?:EMPTY,sentenceInHindi?:EMPTY), "Special").commit()
            }
        }
    }

    private fun setData(specialPractice: SpecialPractice?) {
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
            specialPracticeId: Int?
        ) {
            Intent(context, SpecialPracticeActivity::class.java).apply {
                putExtra(SPECIAL_ID, specialPracticeId)
                putExtra(CONVERSATION_ID, conversationId)
            }.run {
                context.startActivity(this)
            }
        }
    }

    private fun showIntroVideoUi() {
        binding.videoPopup.visibility = View.VISIBLE
        binding.videoView.seekToStart()
        binding.videoView.setUrl(videoUrl)
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
    }

    private fun closeIntroVideoPopUpUi() {
        binding.videoPopup.visibility = View.GONE
        binding.videoView.onStop()
    }

    private fun showRecordedVideoUi() {
        binding.card3.visibility = View.VISIBLE
        binding.videoPlayer.seekToStart()
        binding.videoPlayer.setUrl(recordedUrl)
        binding.videoPlayer.onStart()
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
            binding.videoPlayer.downloadStreamPlay()
        }

        binding.videoPlayer.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 15f)
            }
        }
        binding.videoPlayer.clipToOutline = true
    }
}