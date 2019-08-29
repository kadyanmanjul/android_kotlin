package com.joshtalks.joshskills.repository.local.model.googlelocation

import com.google.gson.annotations.SerializedName


data class Locality(

    @SerializedName("id")
    var id: Int = 0,
    @SerializedName("name")
    var name: String="",
    @SerializedName("state")
    var state: State= State(),
    @SerializedName("district")
    var district: District=District(),
    @SerializedName("latitude")
    var latitude: Double = 0.toDouble(),
    @SerializedName("longitude")
    var longitude: Double = 0.toDouble(),
    @SerializedName("formatted_address")
    var formattedAddress: String="",
    @SerializedName("pin_code")
    var pinCode: Int = 0
)


data class State(
    @SerializedName("id")
    var id: Int = 0,
    @SerializedName("name")
    var name: String=""
)


data class District(

    @SerializedName("id")
    var id: Int = 0,
    @SerializedName("name")
    var name: String="",
    @SerializedName("state_id")
    var state: Int = 0
)
