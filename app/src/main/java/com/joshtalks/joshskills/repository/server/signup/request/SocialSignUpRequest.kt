package com.joshtalks.joshskills.repository.server.signup.request

import com.google.gson.annotations.SerializedName

class SocialSignUpRequest private constructor(
    @SerializedName("instance_id")
    val instanceId: String,
    @SerializedName("mentor_id")
    val mentorId: String,
    @SerializedName("name")
    val name: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("photo_url")
    val photoUrl: String?,
    @SerializedName("created_source")
    val createdSource: String
) {
    data class Builder(
        var mentorId: String,
        var name: String? = null,
        var email: String? = null,
        var instanceId: String,
        var photoUrl: String? = null,
        var createdSource: String=""
    ) {
        fun instanceId(instanceId: String) = apply { this.instanceId = instanceId }
        fun mentorId(mentorId: String) = apply { this.mentorId = mentorId }
        fun email(email: String?) = apply { this.email = email }
        fun name(name: String?) = apply { this.name = name }
        fun photoUrl(photoUrl: String?) = apply { this.photoUrl = photoUrl }
        fun createdSource(createdSource: String) = apply { this.createdSource = createdSource }
        fun build() = SocialSignUpRequest(instanceId, mentorId, name, email, photoUrl,createdSource)
    }
}