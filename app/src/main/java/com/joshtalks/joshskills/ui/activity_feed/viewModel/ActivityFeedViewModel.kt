package com.joshtalks.joshskills.ui.activity_feed.viewModel

import android.annotation.SuppressLint
import android.os.Handler
import android.provider.Contacts
import android.view.View
import android.widget.SeekBar
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.viewModelScope
import com.clevertap.android.sdk.Utils.runOnUiThread
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.ui.activity_feed.ActivityFeedListAdapter
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedResponse
import com.joshtalks.joshskills.ui.activity_feed.repository.ActivityFeedRepository
import com.joshtalks.joshskills.ui.activity_feed.utils.*
import com.joshtalks.joshskills.util.ExoAudioPlayer2
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseButton
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable
import java.sql.Timestamp

class ActivityFeedViewModel : BaseViewModel(), LifecycleObserver {
    val activityFeedRepository by lazy{ ActivityFeedRepository() }

    val adapter = ActivityFeedListAdapter()
    val isScrollToEndButtonVisible = ObservableBoolean(false)
    val updateProfilePicOrBorder = ObservableBoolean(false)
    val fetchingAllFeed = ObservableBoolean(false)
    var startTime = System.currentTimeMillis()
    var impressionId = ObservableField(EMPTY)

    private lateinit var playPauseButton: MaterialPlayPauseButton
    private lateinit var seekbar: SeekBar
    val exoAudioManager by lazy { ExoAudioPlayer2() }
    private lateinit var prevPlayPauseButton: MaterialPlayPauseButton
    private var id: String = EMPTY
    private var url: String = EMPTY
    private var lastPosition: Long = 0L
    private var duration: Int = 0

    fun getActivityFeed(timestamp: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if(timestamp.isEmpty()) {
                    fetchingAllFeed.set(true)
                }
                val response =
                    activityFeedRepository.getActivityFeedData(timestamp)
                if (response.isSuccessful) {
                    response.body()?.let {
                        fetchingAllFeed.set(false)
                        impressionId.set(response.body()?.impressionId)
                        it.activityList?.forEach {currentFeed->
                            addItem(currentFeed)
                            delay(60000L.div(it.activityList.size))
                        }
                        it.activityList?.let{
                            if(it.size==0){
                                delay(60000L)
                            }
                        }
                        response.body()?.timestamp?.let { getActivityFeed(it) }
                    }

                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                fetchingAllFeed.set(false)
            }
        }
    }
    @SuppressLint("RestrictedApi")
    private fun addItem(activityFeedResponse: ActivityFeedResponse) {
        runOnUiThread {
            adapter.items.add(0, activityFeedResponse)
            adapter.notifyItemInserted(0)
            message.what= ON_ITEM_ADDED
            singleLiveEvent.value=message
        }
    }

    fun engageActivityFeedTime(impressionId: String, startTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (impressionId.isBlank())
                    return@launch

                activityFeedRepository.engageActivityFeedTime(
                    impressionId,
                    mapOf("time_spent" to startTime)
                )

            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    val onItemClick: (ActivityFeedResponse, Int, SeekBar?, MaterialPlayPauseButton?, ActivityFeedListAdapter.ViewHolder?) -> Unit = { activityFeedResponse, type,seekBar,playPauseBtn,viewHolder ->
        when (type) {
            OPEN_FEED_USER_PROFILE -> {
                message.what = OPEN_FEED_USER_PROFILE
                message.obj = activityFeedResponse
                singleLiveEvent.value = message
            }
            OPEN_PROFILE_IMAGE_FRAGMENT -> {
                message.what = OPEN_PROFILE_IMAGE_FRAGMENT
                message.obj = activityFeedResponse
                singleLiveEvent.value = message
            }
            PLAY_AUDIO->{
                seekbar = seekBar!!
                playPauseButton = playPauseBtn!!
                seekbar.progress = 0
                id = System.currentTimeMillis().toString()
                this.url = activityFeedResponse.mediaUrl.toString()
                this.duration = activityFeedResponse.duration
                seekbar.max = duration
                if (playPauseButton.state == MaterialPlayPauseDrawable.State.Play) {
                    if(exoAudioManager.isPlaying()){
                        prevPlayPauseButton.state = MaterialPlayPauseDrawable.State.Play
                    }
                    prevPlayPauseButton=playPauseButton
                    removeSeekbarListener()
                    if (viewHolder != null) {
                        addListener(viewHolder)
                    }
                    initAndPlay()
                } else {
                    playPauseButton.state = MaterialPlayPauseDrawable.State.Play
                    onPausePlayer()
                }
            }
            ON_AUDIO_COMPLETE->{
                playPauseButton.state = MaterialPlayPauseDrawable.State.Play
                playPauseButton.jumpToState(MaterialPlayPauseDrawable.State.Play)
                seekbar.progress = 0
                exoAudioManager?.seekTo(0)
                exoAudioManager?.onPause()
                exoAudioManager?.setProgressUpdateListener(null)
            }
        }
    }
    private fun removeSeekbarListener() {
        seekbar.setOnSeekBarChangeListener(null)
        exoAudioManager?.playerListener = null
        exoAudioManager?.setProgressUpdateListener(
            null
        )
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPausePlayer() {
        if(exoAudioManager.isPlaying()) {
            exoAudioManager?.onPause()
            playPauseButton.state = MaterialPlayPauseDrawable.State.Play
        }

    }

    private fun addListener(viewHolder: ActivityFeedListAdapter.ViewHolder) {
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
        exoAudioManager?.playerListener = viewHolder
        exoAudioManager?.setProgressUpdateListener(viewHolder)
    }

    private fun initAndPlay() {
        url.let {
            exoAudioManager?.play(it, id, lastPosition)
            seekbar.progress = lastPosition.toInt()
            playPauseButton.state = MaterialPlayPauseDrawable.State.Pause

        }
    }
    fun onBackPress(view: View) {
        saveEngageTime()
    }

    fun onScrollToEnd(view: View) {
        message.what = FEED_SCROLL_TO_END
        singleLiveEvent.value = message
    }

    fun saveEngageTime() {
        startTime = System.currentTimeMillis().minus(startTime).div(1000)
        if (startTime > 0 && impressionId.get()?.isNotBlank() == true) {
            engageActivityFeedTime(impressionId.get() ?: EMPTY, startTime)
        }
        message.what = ON_FEED_BACK_PRESSED
        singleLiveEvent.value = message
    }

}