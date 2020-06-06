package com.joshtalks.joshskills.repository.server.signup

import com.google.gson.annotations.SerializedName

class RequestSocialSignUp private constructor(
    @SerializedName("instance_id")
    val instanceId: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("photo_url")
    val photoUrl: String?
) {
    data class Builder(
        var id: String,
        var name: String? = null,
        var email: String? = null,
        var instanceId: String,
        var photoUrl: String? = null
    ) {
        fun instanceId(instanceId: String) = apply { this.instanceId = instanceId }
        fun id(id: String) = apply { this.id = id }
        fun email(email: String?) = apply { this.email = email }
        fun name(name: String?) = apply { this.name = name }
        fun photoUrl(photoUrl: String?) = apply { this.photoUrl = photoUrl }
        fun build() = RequestSocialSignUp(instanceId, id, name, email, photoUrl)
    }
}