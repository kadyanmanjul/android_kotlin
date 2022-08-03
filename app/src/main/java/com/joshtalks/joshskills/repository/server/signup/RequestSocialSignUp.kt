package com.joshtalks.joshskills.repository.server.signup

import com.google.gson.annotations.SerializedName

class RequestSocialSignUp private constructor(
    @SerializedName("gaid")
    val gaid: String,
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
        var gaid: String,
        var photoUrl: String? = null
    ) {
        fun gaid(gaid: String) = apply { this.gaid = gaid }
        fun id(id: String) = apply { this.id = id }
        fun email(email: String?) = apply { this.email = email }
        fun name(name: String?) = apply { this.name = name }
        fun photoUrl(photoUrl: String?) = apply { this.photoUrl = photoUrl }
        fun build() = RequestSocialSignUp(gaid, id, name, email, photoUrl)
    }
}