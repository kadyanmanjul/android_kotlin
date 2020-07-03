package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName

data class RequestCertificateGenerate(
    @SerializedName("mentor")
    val mentor: String,
    @SerializedName("conversation")
    val conversation: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String
)