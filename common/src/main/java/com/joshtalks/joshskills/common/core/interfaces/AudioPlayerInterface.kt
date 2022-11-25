package com.joshtalks.joshskills.common.core.interfaces

interface AudioPlayerInterface {
    fun downloadInQueue()
    fun downloadStart(url: String)
    fun downloadStop()


}