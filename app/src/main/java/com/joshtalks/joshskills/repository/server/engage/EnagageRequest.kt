package com.joshtalks.joshskills.repository.server.engage

import com.google.gson.annotations.SerializedName


data class VideoEngage(
    @SerializedName("graph")
    val graph: List<Graph>,
    @SerializedName("video_id")
    val videoId: String,
    @SerializedName("watch_time")
    val watchTime: Long
)


data class AudioEngage(
   @SerializedName("graph")
    val graph: List<Graph>,
    @SerializedName("audio_id")
    val audioId: String?,
    @SerializedName("watch_time")
    val listen_time: Long
)


data class PdfEngage(
    @SerializedName("pdf_id")
    val pdfId: String,
    @SerializedName("total_view")
    var totalView: Int
)


data class ImageEngage(
    @SerializedName("image_id")
    val imageId: String,
    @SerializedName("total_view")
    val totalView: Int
)