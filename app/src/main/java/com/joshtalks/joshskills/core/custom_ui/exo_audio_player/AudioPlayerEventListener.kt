package com.joshtalks.joshskills.core.custom_ui.exo_audio_player

interface AudioPlayerEventListener {
    fun onPlayerPause()
    fun onPlayerResume()
    fun onCurrentTimeUpdated(lastPosition: Long)
    fun onTrackChange(tag: String?)
    fun onPositionDiscontinuity(lastPos: Long, reason: Int = 1)
    fun onPlayerReleased()
    fun onPlayerEmptyTrack()
    fun onPositionDiscontinuity(reason: Int)
    fun complete()
}
