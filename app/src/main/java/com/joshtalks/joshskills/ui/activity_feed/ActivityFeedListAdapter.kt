package com.joshtalks.joshskills.ui.activity_feed

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.databinding.ActivityFeedRowItemBinding
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedResponse
import com.joshtalks.joshskills.ui.activity_feed.utils.OPEN_FEED_USER_PROFILE
import com.joshtalks.joshskills.ui.activity_feed.utils.OPEN_PROFILE_IMAGE_FRAGMENT
import com.joshtalks.joshskills.util.ExoAudioPlayer2
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseButton
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable

class ActivityFeedListAdapter :
    RecyclerView.Adapter<ActivityFeedListAdapter.ViewHolder>() {
    var itemClick: ((ActivityFeedResponse, Int) -> Unit)? = null
    var items: ArrayList<ActivityFeedResponse> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = DataBindingUtil.inflate<ActivityFeedRowItemBinding>(
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

    fun setListener(function: ((ActivityFeedResponse, Int) -> Unit)?) {
        itemClick = function
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val view: ActivityFeedRowItemBinding) :
        RecyclerView.ViewHolder(view.root),
        ExoAudioPlayer2.ProgressUpdateListener, AudioPlayerEventListener {

        private lateinit var playPauseButton: MaterialPlayPauseButton
        private lateinit var seekbar: SeekBar
        lateinit var exoAudioManager: ExoAudioPlayer2
        private var id: String = EMPTY
        private var url: String = EMPTY
        private var lastPosition: Long = 0L
        private var duration: Int = 0

        fun bind(activityFeedResponse: ActivityFeedResponse) {
            view.itemData = activityFeedResponse
            view.handler = this
            seekbar = view.seekBar
                        playPauseButton = view.btnPlayPause
                        seekbar.progress = 0
                        exoAudioManager = ExoAudioPlayer2()
                        id = System.currentTimeMillis().toString()
                        this.url = activityFeedResponse.mediaUrl.toString()
                        this.duration = activityFeedResponse.duration
                        seekbar.max = duration

            view.feedText.setOnClickListener {
                    itemClick?.invoke(activityFeedResponse, OPEN_FEED_USER_PROFILE)
                }
                view.updatedPic.setOnClickListener {
                    itemClick?.invoke(activityFeedResponse, OPEN_PROFILE_IMAGE_FRAGMENT)
                }

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
        fun onPlayPauseBtnClick() {
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
        private fun removeSeekbarListener() {
            seekbar.setOnSeekBarChangeListener(null)
            exoAudioManager?.playerListener = null
            exoAudioManager?.setProgressUpdateListener(
                null
            )
        }

        fun onPausePlayer() {
            exoAudioManager?.onPause()
            playPauseButton.state = MaterialPlayPauseDrawable.State.Play

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
    }
}