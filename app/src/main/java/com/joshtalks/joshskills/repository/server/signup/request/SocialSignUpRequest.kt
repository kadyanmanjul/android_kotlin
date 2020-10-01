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
    val createdSource: String,
    @SerializedName("user_id")
    val userId: String?,
    @SerializedName("social_id")
    val socialId: String?,
    @SerializedName("payload")
    val payload: String?,
    @SerializedName("signature")
    val signature: String?,
    @SerializedName("signature_algo")
    val signatureAlgo: String?,
    @SerializedName("mobile")
    var mobile: String?,
    @SerializedName("country_code")
    var countryCode: String?,
    @SerializedName("otp")
    val otp: String?,
    @SerializedName("date_of_birth")
    var dateOfBirth: String?,
    @SerializedName("gender")
    var gender: String?
) {
    data class Builder(
        var mentorId: String,
        var instanceId: String,
        var createdSource: String = "",
        var userId: String? = null,
    ) {
        var name: String? = null
        var email: String? = null
        var photoUrl: String? = null
        var socialId: String? = null
        var payload: String? = null
        var signature: String? = null
        var signatureAlgo: String? = null
        var mobile: String? = null
        var otp: String? = null
        var countryCode: String? = null
        var dateOfBirth: String? = null
        var gender: String? = null

        fun instanceId(instanceId: String) = apply { this.instanceId = instanceId }
        fun mentorId(mentorId: String) = apply { this.mentorId = mentorId }
        fun email(email: String?) = apply { this.email = email }
        fun name(name: String?) = apply { this.name = name }
        fun photoUrl(photoUrl: String?) = apply { this.photoUrl = photoUrl }
        fun createdSource(createdSource: String) = apply { this.createdSource = createdSource }
        fun userId(userId: String) = apply { this.userId = userId }
        fun socialId(socialId: String) = apply { this.socialId = socialId }
        fun payload(payload: String) = apply { this.payload = payload }
        fun signature(signature: String) = apply { this.signature = signature }
        fun signatureAlgo(signatureAlgo: String) = apply { this.signatureAlgo = signatureAlgo }
        fun mobile(mobile: String) = apply { this.mobile = mobile }
        fun otp(otp: String) = apply { this.otp = otp }
        fun countryCode(countryCode: String) = apply { this.countryCode = countryCode }
        fun dateOfBirth(dateOfBirth: String) = apply { this.dateOfBirth = dateOfBirth }
        fun gender(gender: String) = apply { this.gender = gender }
        fun build() =
            SocialSignUpRequest(
                instanceId,
                mentorId,
                name,
                email,
                photoUrl,
                createdSource,
                userId,
                socialId,
                payload,
                signature,
                signatureAlgo, mobile, countryCode, otp,
                dateOfBirth,
                gender
            )
    }
}