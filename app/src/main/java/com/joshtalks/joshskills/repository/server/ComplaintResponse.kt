package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName

data class ComplaintResponse(
    @SerializedName("category")
    val category: Int,
    @SerializedName("created")
    val created: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("image_url")
    val imageUrl: Any,
    @SerializedName("mobile")
    val mobile: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("problem")
    val problem: String,
    @SerializedName("ticket_id")
    val ticketId: String
)