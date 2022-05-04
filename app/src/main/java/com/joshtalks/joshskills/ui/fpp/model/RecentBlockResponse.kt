package com.joshtalks.joshskills.ui.fpp.model

import com.google.gson.annotations.SerializedName

data class RecentBlockResponse(@SerializedName("data") var recentBlock: RecentBlock)

data class RecentBlock(
    @SerializedName("group_id") var groupId: String?,
    @SerializedName("agora_uid") var uId: Int?
)
