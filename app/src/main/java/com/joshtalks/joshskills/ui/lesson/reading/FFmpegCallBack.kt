package com.joshtalks.joshskills.ui.lesson.reading

import android.media.MediaDrm
import com.arthenica.mobileffmpeg.LogMessage
import com.arthenica.mobileffmpeg.Statistics

interface FFmpegCallBack {
    fun process(logMessage: MediaDrm.LogMessage) {}
    fun statisticsProcess(statistics: Statistics) {}
    fun success() {}
    fun cancel() {}
    fun failed() {}
}