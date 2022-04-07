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
import com.joshtalks.joshskills.ui.activity_feed.utils.ON_AUDIO_COMPLETE
import com.joshtalks.joshskills.ui.activity_feed.utils.OPEN_FEED_USER_PROFILE
import com.joshtalks.joshskills.ui.activity_feed.utils.OPEN_PROFILE_IMAGE_FRAGMENT
import com.joshtalks.joshskills.ui.activity_feed.utils.PLAY_AUDIO
import com.joshtalks.joshskills.util.ExoAudioPlayer2
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseButton
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable

class ActivityFeedListAdapter :
    RecyclerView.Adapter<ActivityFeedListAdapter.ViewHolder>() {
    var itemClick: ((ActivityFeedResponse, Int,SeekBar?,MaterialPlayPauseButton?,ViewHolder?) -> Unit)? = null
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

    fun setListener(function: ((ActivityFeedResponse, Int,SeekBar?,MaterialPlayPauseButton?,ViewHolder?) -> Unit)?) {
        itemClick = function
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val view: ActivityFeedRowItemBinding) :
        RecyclerView.ViewHolder(view.root),
        ExoAudioPlayer2.ProgressUpdateListener, AudioPlayerEventListener {

        fun bind(activityFeedResponse: ActivityFeedResponse) {
            view.itemData = activityFeedResponse

            view.feedText.setOnClickListener {
                    itemClick?.invoke(activityFeedResponse, OPEN_FEED_USER_PROFILE,null,null,null)
                }
            view.updatedPic.setOnClickListener {
                    itemClick?.invoke(activityFeedResponse, OPEN_PROFILE_IMAGE_FRAGMENT,null,null,null)
                }
            view.btnPlayPause.setOnClickListener{
                    itemClick?.invoke(activityFeedResponse, PLAY_AUDIO,view.seekBar,view.btnPlayPause,this)
            }

        }
        override fun complete() {
            itemClick?.invoke(view.itemData!!, ON_AUDIO_COMPLETE,null,null,null)
        }
        override fun onProgressUpdate(progress: Long) {
            view.seekBar.progress = progress.toInt()
        }

    }
}