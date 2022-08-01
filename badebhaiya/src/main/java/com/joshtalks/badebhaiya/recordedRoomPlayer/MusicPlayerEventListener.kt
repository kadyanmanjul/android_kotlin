package com.joshtalks.badebhaiya.recordedRoomPlayer

import android.widget.Toast
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class MusicPlayerEventListener(
    private val musicService: AudioPlayerService,
    private val scope: CoroutineScope
) : Player.Listener {

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Timber.tag("loadingplayer").d("PLAYER STATE => $playbackState")

        super.onPlayerStateChanged(playWhenReady, playbackState)

        if(playbackState == Player.STATE_READY && !playWhenReady) {
            musicService.stopForeground(false)
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        Timber.tag("loadingplayer").d("PLAYBACK STATE => $playbackState")

        super.onPlaybackStateChanged(playbackState)

    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        Timber.tag("loadingplayer").d("LOADING STATE => $isLoading")
        scope.launch {
            PlayerData.isLoading.emit(isLoading)
        }
        super.onIsLoadingChanged(isLoading)
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        error.printStackTrace()
        Timber.tag("playererror").d("Player error is => $error")
        Toast.makeText(musicService, "An unknown error occured", Toast.LENGTH_LONG).show()
    }

//    override fun onPlayerError(error: ExoPlaybackException) {
//        super.onPlayerError(error)
//        Toast.makeText(musicService, "An unknown error occured", Toast.LENGTH_LONG).show()
//    }
}