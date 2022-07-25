package com.joshtalks.badebhaiya.recordedRoomPlayer

import android.widget.Toast
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player

class MusicPlayerEventListener(
    private val musicService: AudioPlayerService
) : Player.Listener {

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        super.onPlayerStateChanged(playWhenReady, playbackState)
        if(playbackState == Player.STATE_READY && !playWhenReady) {
            musicService.stopForeground(false)
        }
    }

//    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
//        super.onPlayWhenReadyChanged(playWhenReady, reason)
//        if(reason == Player.STATE_READY && !playWhenReady) {
//            musicService.stopForeground(false)
//        }
//    }
//
//    override fun onPlaybackStateChanged(playbackState: Int) {
//        super.onPlaybackStateChanged(playbackState)
//    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Toast.makeText(musicService, "An unknown error occured", Toast.LENGTH_LONG).show()
    }

//    override fun onPlayerError(error: ExoPlaybackException) {
//        super.onPlayerError(error)
//        Toast.makeText(musicService, "An unknown error occured", Toast.LENGTH_LONG).show()
//    }
}