package com.joshtalks.joshskills.premium.repository.local.model


import com.google.gson.annotations.SerializedName

data class CountryDetail(
    @SerializedName("code")
    val code: String,
    @SerializedName("dial_code")
    val dialCode: String,
    @SerializedName("name")
    val name: String
)