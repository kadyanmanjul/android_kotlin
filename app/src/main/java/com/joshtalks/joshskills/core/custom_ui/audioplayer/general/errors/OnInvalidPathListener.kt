package com.joshtalks.joshskills.core.custom_ui.audioplayer.general.errors

import com.joshtalks.joshskills.core.custom_ui.audioplayer.model.JcAudio

interface OnInvalidPathListener {

    /**
     * Audio path error jcPlayerManagerListener.
     * @param jcAudio The wrong audio.
     */
    fun onPathError(jcAudio: JcAudio)
}