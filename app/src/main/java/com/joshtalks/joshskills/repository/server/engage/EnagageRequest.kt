package com.joshtalks.joshskills.repository.server.engage

import com.google.gson.annotations.SerializedName

data class AudioEngage(
    @SerializedName("graph")
    val graph: List<Graph>,
    @SerializedName("audio_id")
    val audioId: String?,
    @SerializedName("listen_time")
    val listen_time: Long
)


data class PdfEngage(
    @SerializedName("pdf_id")
    val pdfId: String,
    @SerializedName("total_view")
    var totalView: Int = 1
)


data class ImageEngage(
    @SerializedName("image_id")
    val imageId: String,
    @SerializedName("total_view")
    val totalView: Int = 1
)