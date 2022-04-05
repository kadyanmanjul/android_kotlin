package com.joshtalks.badebhaiya.core.models

import com.google.gson.annotations.SerializedName


data class UpdateUserPersonal(
    @SerializedName("first_name")
    var firstName: String = "",
    @SerializedName("date_of_birth")
    var dateOfBirth: String = "",
    @SerializedName("gender")
    var gender: String = ""
)

data class UpdateUserLocality(
    @SerializedName("locality")
    var locality: SearchLocality = SearchLocality()
)


data class SearchLocality(

    @SerializedName("latitude")
    var latitude: Double = 0.toDouble(),
    @SerializedName("longitude")
    var longitude: Double = 0.toDouble()
)