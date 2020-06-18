package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName

data class FreshChatRestoreIDResponse(
    @SerializedName("id")
    var id: String,
    @SerializedName("restore_id")
    var restoreId: String? = null

)