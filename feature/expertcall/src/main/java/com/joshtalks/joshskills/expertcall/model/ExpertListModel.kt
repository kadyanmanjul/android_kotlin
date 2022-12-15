package com.joshtalks.joshskills.expertcall.model

import com.google.gson.annotations.SerializedName

data class ExpertListResponse(
    @SerializedName("bb_tip_expert")
    val bbTipText: String,

    @SerializedName("expert_list")
    var arrayList: ArrayList<ExpertListModel>
)

data class ExpertListModel(
    @SerializedName("full_name") val expertName: String,
    @SerializedName("photo_url") val expertImage: String,
    @SerializedName("spoken_languages") val expertLanguageSpeak: String,
    @SerializedName("experience") val expertExperience: Int,
    @SerializedName("price") val expertPricePerMinute: Int,
    @SerializedName("bio") val expertBio: String,
    @SerializedName("rating") val expertRating: Float,
    @SerializedName("agora_user_id") val agoraId: Int,
    @SerializedName("mentor") val mentorId: String
)