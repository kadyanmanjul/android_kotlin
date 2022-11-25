package com.joshtalks.joshskills.common.repository.server.signup.request

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.common.core.Utils

class SocialSignUpRequest private constructor(
    @SerializedName("gaid")
    val gaid: String,
    @SerializedName("mentor_id")
    val mentorId: String,
    @SerializedName("name")
    val name: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("photo_url")
    val photoUrl: String?,
    @SerializedName("created_source")
    val createdSource: String,
    @SerializedName("device_id")
    val deviceId: String = Utils.getDeviceId()
) {
    data class Builder(
        var mentorId: String,
        var name: String? = null,
        var email: String? = null,
        var gaid: String,
        var photoUrl: String? = null,
        var createdSource: String = ""
    ) {
        fun gaid(gaid: String) = apply { this.gaid = gaid }
        fun mentorId(mentorId: String) = apply { this.mentorId = mentorId }
        fun email(email: String?) = apply { this.email = email }
        fun name(name: String?) = apply { this.name = name }
        fun photoUrl(photoUrl: String?) = apply { this.photoUrl = photoUrl }
        fun createdSource(createdSource: String) = apply { this.createdSource = createdSource }
        fun build() =
            SocialSignUpRequest(gaid, mentorId, name, email, photoUrl, createdSource)
    }
}
