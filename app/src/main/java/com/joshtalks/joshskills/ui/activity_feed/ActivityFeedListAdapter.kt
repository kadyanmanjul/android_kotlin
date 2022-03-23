package com.joshtalks.joshskills.ui.activity_feed

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.databinding.ActivityFeedRowItemBinding
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedResponseFirebase
import com.joshtalks.joshskills.ui.userprofile.ProfileImageShowFragment
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.joshtalks.joshskills.util.ExoAudioPlayer2
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseButton
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable

class ActivityFeedListAdapter(
    private val items: ArrayList<ActivityFeedResponseFirebase>,
    val context:Context,
    val activity: Activity
) : RecyclerView.Adapter<ActivityFeedListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view=DataBindingUtil.inflate<ActivityFeedRowItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.activity_feed_row_item,
            parent,
            false
        )
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
    override fun getItemCount(): Int = items.size
    inner class ViewHolder(val view:ActivityFeedRowItemBinding) : RecyclerView.ViewHolder(view.root),
        ExoAudioPlayer2.ProgressUpdateListener, AudioPlayerEventListener {

        private lateinit var playPauseButton: MaterialPlayPauseButton
        private lateinit var seekbar: SeekBar
        private lateinit var timestamp: AppCompatTextView

        lateinit var exoAudioManager: ExoAudioPlayer2
        private var id: String = EMPTY
        private var url: String = EMPTY
        private var lastPosition: Long = 0L
        private var duration: Int = 0

        fun bind(activityFeedResponse: ActivityFeedResponseFirebase) {
            view.itemData=activityFeedResponse
            view.handler=this
            seekbar = view.seekBar
            timestamp = view.duration
            playPauseButton = view.btnPlayPause
            seekbar.progress = 0
            exoAudioManager= ExoAudioPlayer2()
            activityFeedResponse.mediaUrl?.let { activityFeedResponse.duration?.let { it1 ->
                if(it1!=0) {
                    initAudioPlayer(
                        it,
                        it1
                    )
                }
            }
            }
        }
        fun openProfileImageShowFragment(imgUrl:String,mentorId:String){
            ProfileImageShowFragment.newInstance(imgUrl, null, null,
                mentorId,false)
                .show((context as ActivityFeedMainActivity).supportFragmentManager.beginTransaction(), "ImageShow")
        }
        fun onPlayPauseBtnClick(){
            if (playPauseButton.state == MaterialPlayPauseDrawable.State.Play) {
                playAudio()
            } else {
                playPauseButton.state = MaterialPlayPauseDrawable.State.Play
                onPausePlayer()
            }
        }
        private fun playAudio() {
                removeSeekbarListener()
                addListener()
                initAndPlay()
        }
        fun initAudioPlayer(url: String, duration: Int) {
            lastPosition = 0
            seekbar.progress = 0
            id = System.currentTimeMillis().toString()
            this.url = url
            this.duration = duration
            seekbar.max = duration
            timestamp.text = Utils.formatDuration(duration)
        }
        private fun removeSeekbarListener() {
            seekbar.setOnSeekBarChangeListener(null)
            exoAudioManager?.playerListener = null
            exoAudioManager?.setProgressUpdateListener(
                null
            )
        }
        fun onPausePlayer() {
            exoAudioManager?.onPause()
            timestamp.text = Utils.formatDuration(duration)
            playPauseButton.state = MaterialPlayPauseDrawable.State.Play

        }
        override fun complete() {
            playPauseButton.state = MaterialPlayPauseDrawable.State.Play
            playPauseButton.jumpToState(MaterialPlayPauseDrawable.State.Play)
            seekbar.progress = 0
            exoAudioManager?.seekTo(0)
            exoAudioManager?.onPause()
            exoAudioManager?.setProgressUpdateListener(null)
        }
        override fun onProgressUpdate(progress: Long) {
            seekbar.progress = progress.toInt()
        }
        private fun addListener() {
            seekbar.setOnSeekBarChangeListener(null)
            seekbar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    var userSelectedPosition = 0
                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                    }
                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser) {
                            userSelectedPosition = progress
                        }
                        timestamp.text = Utils.formatDuration(progress)
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        exoAudioManager?.seekTo(userSelectedPosition.toLong())
                    }
                })
            exoAudioManager?.playerListener = this
            exoAudioManager?.setProgressUpdateListener(this)
        }
        private fun initAndPlay() {
            url.let {
                exoAudioManager?.play(it, id, lastPosition)
                seekbar.progress = lastPosition.toInt()
                playPauseButton.state = MaterialPlayPauseDrawable.State.Pause

            }
        }
        fun openUserProfileActivity(id: String?) {
            if (id != null) {
                UserProfileActivity.startUserProfileActivity(
                    activity,
                    id,
                    arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                    null,
                    "ACTIVITY_FEED",
                    conversationId = null
                )
            }
        }
    }

}

