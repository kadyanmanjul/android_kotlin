package com.joshtalks.joshskills.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.camera.video.VideoRecordEvent
import java.io.File

fun doesVideoHaveAudio(path: String): Boolean {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(path)
    return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
}

fun getVideoDuration(context: Context, file: File): Long {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(context, Uri.fromFile(file))
    val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    val timeInMillis = time?.toLong() ?: 0
    retriever.release()
    return timeInMillis
}

fun VideoRecordEvent.getNameString() : String {
    return when (this) {
        is VideoRecordEvent.Status -> "Status"
        is VideoRecordEvent.Start -> "Started"
        is VideoRecordEvent.Finalize-> "Finalized"
        is VideoRecordEvent.Pause -> "Paused"
        is VideoRecordEvent.Resume -> "Resumed"
        else -> throw IllegalArgumentException("Unknown VideoRecordEvent: $this")
    }
}