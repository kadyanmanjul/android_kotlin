package com.joshtalks.joshskills.repository.server.voip


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.local.model.Mentor
import kotlinx.android.parcel.Parcelize

@Parcelize
data class VoipCallDetailModel(
    @SerializedName("name") val name: String? = EMPTY,
    @SerializedName("mentor_id") var mentorId: String = EMPTY,
    @SerializedName("locality") val locality: String? = EMPTY,
    @SerializedName("profile_pic") val profilePic: String? = EMPTY,
    val topic: String? = "aise hi sexy lag rha hai"

) : Parcelable {
    constructor() : this(
        name = EMPTY,
        mentorId = EMPTY,
        locality = EMPTY,
        profilePic = EMPTY,
        topic = "English par"
    )

    fun getOutgoingCallObject(): VoipCallDetailModel {
        val mentorTemp = Mentor.getInstance()
        return VoipCallDetailModel(
            mentorTemp.getUser()?.firstName,
            mentorTemp.getId(),
            mentorTemp.getLocality()?.formattedAddress,
            mentorTemp.getUser()?.photo,
            topic
        )
    }
}