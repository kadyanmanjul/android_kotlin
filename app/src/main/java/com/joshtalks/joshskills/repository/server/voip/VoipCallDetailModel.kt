package com.joshtalks.joshskills.repository.server.voip


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import kotlinx.android.parcel.Parcelize

@Parcelize
data class VoipCallDetailModel(
    @SerializedName("name") val name: String? = EMPTY,
    @SerializedName("plivo_username") val plivoUserName: String? = EMPTY,
    @SerializedName("locality") val locality: String? = EMPTY,
    @SerializedName("profile_pic") val profilePic: String? = EMPTY,
    @SerializedName("topicName") var topic: String? = EMPTY,
    @SerializedName("topicId") var topicName: String? = EMPTY,
    @SerializedName("callieName") var callieName: String? = EMPTY,
) : Parcelable {
    constructor() : this(
        name = EMPTY,
        plivoUserName = EMPTY,
        locality = EMPTY,
        profilePic = EMPTY,
        topic = EMPTY
    )

    fun string(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }

    companion object {
        fun getVoipCallDetailModel(data: String?): VoipCallDetailModel? {
            return try {
                return AppObjectController.gsonMapper.fromJson(
                    data, VoipCallDetailModel::class.java
                )
            } catch (ex: Exception) {
                null
            }
        }
    }
}