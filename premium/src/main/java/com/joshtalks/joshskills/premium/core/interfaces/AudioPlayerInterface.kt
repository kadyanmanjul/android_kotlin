package com.joshtalks.joshskills.premium.core.interfaces

interface AudioPlayerInterface {
    fun downloadInQueue()
    fun downloadStart(url: String)
    fun downloadStop()


}