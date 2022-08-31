package com.joshtalks.joshskills.ui.recording_gallery

import android.os.Parcelable
import java.io.Serializable
import java.sql.Timestamp

data class RecordingModel(
    val duration : Int? = null,
    var imgUrl  : String? = null,
    val videoUrl : String? = null,
    val timestamp: Timestamp? = null
):Serializable