package com.joshtalks.badebhaiya.mediaPlayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.ExoPlayer
import com.joshtalks.badebhaiya.liveroom.LiveRoomState
import com.joshtalks.badebhaiya.recordedRoomPlayer.*
import com.joshtalks.badebhaiya.recordedRoomPlayer.AudioPlayerService.Companion.MEDIA_ROOT_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class RecordedRoomViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection,
) : ViewModel() {
//    private val _mediaItems = MutableLiveData<Resource<List<Song>>>()
//    val mediaItems: LiveData<Resource<List<Song>>> = _mediaItems

    companion object {
        const val FORWARD_BACKWARD_TIME = 15000
    }

    val isConnected = musicServiceConnection.isConnected
    val networkError = musicServiceConnection.networkError
    val curPlayingSong = musicServiceConnection.curPlayingSong
    val playbackState = musicServiceConnection.playbackState

    private val _curSongDuration = MutableLiveData<Long>()
    val curSongDuration: LiveData<Long> = _curSongDuration

    private val _curPlayerPosition = MutableLiveData<Long>()
    val curPlayerPosition: LiveData<Long> = _curPlayerPosition


    var lvRoomState= MutableLiveData<LiveRoomState>()

    init {

        updateCurrentPlayerPosition()

//        _mediaItems.postValue(Resource.loading(null))
        musicServiceConnection.subscribe(MEDIA_ROOT_ID, object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                super.onChildrenLoaded(parentId, children)
//                val items = children.map {
//                    Song(
//                        it.mediaId!!,
//                        it.description.title.toString(),
//                        it.description.subtitle.toString(),
//                        it.description.mediaUri.toString(),
//                        it.description.iconUri.toString()
//                    )
//                }
//                _mediaItems.postValue(Resource.success(items))
            }
        })
    }




    private fun updateCurrentPlayerPosition() {
        viewModelScope.launch {
            while(true) {
                val pos = playbackState.value?.currentPlaybackPosition
                if(curPlayerPosition.value != pos) {
                    _curPlayerPosition.postValue(pos ?: 0)
                    _curSongDuration.postValue(AudioPlayerService.curSongDuration)
                }
                delay(AudioPlayerService.UPDATE_PLAYER_POSITION_INTERVAL)
            }
        }
    }

    fun skipToNextSong() {
        musicServiceConnection.transportControls.skipToNext()
    }

    fun skipToPreviousSong() {
        musicServiceConnection.transportControls.skipToPrevious()
    }

    fun seekTo(pos: Long) {
        musicServiceConnection.transportControls.seekTo(pos)
    }

    fun forward(){
        curPlayerPosition.value?.let {
            seekTo(it + FORWARD_BACKWARD_TIME)
        }
    }

    fun backward(){
        curPlayerPosition.value?.let {
            seekTo(it - FORWARD_BACKWARD_TIME)
        }
    }

    fun increaseSpeed(speed: Float){
        Timber.tag("speedincrease").d("SPEED INCREASE TO => $speed")
        musicServiceConnection.setPlaybackSpeed(speed)
//        exoPlayer.setPlaybackSpeed(speed)
//        musicServiceConnection.transportControls.setPlaybackSpeed(10f)
    }

    fun playOrToggleSong() {
//        musicServiceConnection.transportControls.play()
        val isPrepared = playbackState.value?.isPrepared ?: false

//        if(isPrepared && mediaItem.mediaId ==
//            curPlayingSong.value?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)) {
            playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying -> {
                        Timber.tag("recordedroom").d("under is playing state")
                        musicServiceConnection.transportControls.pause()
                    }
                    playbackState.isPlayEnabled -> {
                        Timber.tag("recordedroom").d("under is play enabled state")

                        musicServiceConnection.transportControls.play()
                    }
                    else -> {
                        Timber.tag("recordedroom").d("playback state unknown")
                    }
                }
            }
//        }
//        else {
//            musicServiceConnection.transportControls.playFromMediaId(mediaItem.mediaId, null)
//        }
    }

    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unsubscribe(MEDIA_ROOT_ID, object : MediaBrowserCompat.SubscriptionCallback() {})
    }
}