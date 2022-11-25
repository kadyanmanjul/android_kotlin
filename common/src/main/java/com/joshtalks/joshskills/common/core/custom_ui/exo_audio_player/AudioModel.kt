package com.joshtalks.joshskills.common.core.custom_ui.exo_audio_player

import com.joshtalks.joshskills.common.core.EMPTY

data class AudioModel(
    val audioUrl: String,
    val tag: String?,
    val duration: Int,
    var isSilent: Boolean = false,
    val subTag: String = EMPTY

)