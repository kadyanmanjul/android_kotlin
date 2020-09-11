package com.joshtalks.joshskills.repository.server.voip


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SinchNotificationObject(
    @SerializedName("name") val name: String, // Hello Worlds
    @SerializedName("sinch") val sinch: String
) : Parcelable

@Parcelize
data class Sinch(
    @SerializedName("public_headers") val publicHeaders: PublicHeaders,
    @SerializedName("session_id") val sessionId: String, // dab77e6a-6d20-4ecf-86d6-4c551e6416ad
    @SerializedName("timestamp") val timestamp: Int, // 1599468824
    @SerializedName("type") val type: Int, // 1
    @SerializedName("user_id") val userId: String, // 07a823d9-aeb8-42ef-95f4-58475ef5e1e9
    @SerializedName("version") val version: Int // 4
) : Parcelable

@Parcelize
class PublicHeaders : Parcelable