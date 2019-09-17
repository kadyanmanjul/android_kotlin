package com.joshtalks.joshskills.ui.video_player

import android.content.Context

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.custom_ui.PlayerListener
import com.joshtalks.joshskills.databinding.ActivityVideoPlayer1Binding
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.server.engage.VideoEngage
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.pdfviewer.COURSE_NAME

const val VIDEO_OBJECT = "video_"

class VideoPlayerActivity : BaseActivity(), PlayerListener {

    override fun onPlayerReady() {
    }

    override fun onBufferingUpdated(isBuffering: Boolean) {
    }

    override fun onCurrentTimeUpdated(time: Long) {
    }

    override fun onPlayerReleased() {
    }

    override fun onPositionDiscontinuity(reason: Int) {
    }

    private lateinit var binding: ActivityVideoPlayer1Binding
    private lateinit var chatObject: ChatModel
    private lateinit var exoProgress: DefaultTimeBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding = DataBindingUtil.setContentView(this, R.layout.activity_video_player_1)
        binding.handler = this

        chatObject = intent.getSerializableExtra(VIDEO_OBJECT) as ChatModel


        if (chatObject.url!=null){
            binding.pvPlayer.setUrl(chatObject.url)

        }else {
            binding.pvPlayer.setUrl(chatObject.question?.videoList?.get(0)?.video_url)
        }

        binding.pvPlayer.fitToScreen()
        binding.pvPlayer.play()
        binding.pvPlayer.setActivity(this)
        exoProgress = findViewById(R.id.exo_progress)
        setToolbar()

    }

    private fun setToolbar() {

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.overlay)

        intent.getStringExtra(COURSE_NAME)?.let {
            binding.textMessageTitle.text = it
        }
        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    companion object {
        fun startConversionActivity(
            context: Context,
            chatModel: ChatModel,
            courseName: String
        ) {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            intent.putExtra(VIDEO_OBJECT, chatModel)
            intent.putExtra(COURSE_NAME, courseName)
            context.startActivity(intent)
        }

    }


    override fun onStart() {
        super.onStart()
        binding.pvPlayer.onStart()
    }

    override fun onStop() {
        super.onStop()
        chatObject.question?.videoList?.get(0)?.id?.let {
            EngagementNetworkHelper.engageVideoApi(VideoEngage(emptyList(),it,binding.pvPlayer.lastPosition))
        }
        binding.pvPlayer.onStop()
    }

    override fun onPause() {
        super.onPause()

        binding.pvPlayer.onPause()


    }

    override fun onResume() {
        super.onResume()
        binding.pvPlayer.onResume()
    }


}