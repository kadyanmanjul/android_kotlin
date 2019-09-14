package com.joshtalks.joshskills.ui.video_player

import android.content.Context

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.custom_ui.PlayerListener
import com.joshtalks.joshskills.databinding.ActivityVideoPlayer1Binding
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.VideoType

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

        supportActionBar?.hide()
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

    }


    companion object {
        fun startConversionActivity(context: Context, chatModel: ChatModel) {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            intent.putExtra(VIDEO_OBJECT, chatModel)
            context.startActivity(intent)
        }

    }


    override fun onStart() {
        super.onStart()
        binding.pvPlayer.onStart()
    }

    override fun onStop() {
        super.onStop()
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