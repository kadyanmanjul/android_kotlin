package com.joshtalks.joshskills.core.custom_ui.audioplayer.service

import com.joshtalks.joshskills.core.custom_ui.audioplayer.general.JcStatus


interface JcPlayerServiceListener {

    /**
     * Notifies on prepared audio for the service listeners
     */
    fun onPreparedListener(status: JcStatus)

    /**
     * Notifies on time changed for the service listeners
     */
    fun onTimeChangedListener(status: JcStatus)

    /**
     * Notifies on continue for the service listeners
     */
    fun onContinueListener(status: JcStatus)

    /**
     * Notifies on completed audio for the service listeners
     */
    fun onCompletedListener()

    /**
     * Notifies on paused for the service listeners
     */
    fun onPausedListener(status: JcStatus)

    /**
     * Notifies on stopped for the service listeners
     */
    fun onStoppedListener(status: JcStatus)

    /**
     * Notifies an error for the service listeners
     */
    fun onError(exception: Exception)
}