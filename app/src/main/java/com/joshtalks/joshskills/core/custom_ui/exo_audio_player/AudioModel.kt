package com.joshtalks.joshskills.core.custom_ui.exo_audio_player

import com.joshtalks.joshskills.core.EMPTY

data class AudioModel(
    val audioUrl: String,
    val tag: String?,
    val duration: Int,
    var isSilent: Boolean = false,
    val subTag: String = EMPTY

)