package com.joshtalks.badebhaiya.mediaPlayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.joshtalks.badebhaiya.liveroom.LiveRoomState
import com.joshtalks.badebhaiya.recordedRoomPlayer.AudioPlayerService.Companion.MEDIA_ROOT_ID
import com.joshtalks.badebhaiya.recordedRoomPlayer.MusicServiceConnection
import com.joshtalks.badebhaiya.recordedRoomPlayer.Resource
import com.joshtalks.badebhaiya.recordedRoomPlayer.isPrepared
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class RecordedRoomViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {
//    private val _mediaItems = MutableLiveData<Resource<List<Song>>>()
//    val mediaItems: LiveData<Resource<List<Song>>> = _mediaItems

    val isConnected = musicServiceConnection.isConnected
    val networkError = musicServiceConnection.networkError
    val curPlayingSong = musicServiceConnection.curPlayingSong
    val playbackState = musicServiceConnection.playbackState


    var lvRoomState= MutableLiveData<LiveRoomState>()

    init {
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

    fun skipToNextSong() {
        musicServiceConnection.transportControls.skipToNext()
    }

    fun skipToPreviousSong() {
        musicServiceConnection.transportControls.skipToPrevious()
    }

    fun seekTo(pos: Long) {
        musicServiceConnection.transportControls.seekTo(pos)
    }

    fun playOrToggleSong(toggle: Boolean = false) {
        musicServiceConnection.transportControls.play()
        val isPrepared = playbackState.value?.isPrepared ?: false

//        if(isPrepared && mediaItem.mediaId ==
//            curPlayingSong.value?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)) {
//            playbackState.value?.let { playbackState ->
//                when {
//                    playbackState.isPlaying -> if(toggle) musicServiceConnection.transportControls.pause()
//                    playbackState.isPlayEnabled -> musicServiceConnection.transportControls.play()
//                    else -> Unit
//                }
//            }
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