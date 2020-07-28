package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ReviseConceptResponse(

    @SerializedName("heading")
    val heading: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("media_url")
    val mediaUrl: String,

    @SerializedName("media_type")
    val mediaType: AssessmentMediaType,

    @SerializedName("video_thumbnail_url")
    val videoThumbnailUrl: String?

) : Parcelable {
    constructor(reviseConcept: ReviseConcept) : this(
        heading = reviseConcept.heading,
        title = reviseConcept.title,
        description = reviseConcept.description,
        mediaUrl = reviseConcept.mediaUrl,
        mediaType = reviseConcept.mediaType,
        videoThumbnailUrl = reviseConcept.videoThumbnailUrl
    )
}
