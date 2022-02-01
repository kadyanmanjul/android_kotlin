package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.SerializedName

data class KFactor(

    @SerializedName("duration_filter")
    val duration_filter: Boolean,

    @SerializedName("caller")
    val caller: PersonOnTheCall,

    @SerializedName("receiver")
    val receiver: PersonOnTheCall
)

data class PersonOnTheCall(

    @SerializedName("agora_mentor_id")
    val agora_mentor_id: Int,

    @SerializedName("city")
    val city: String?,

    @SerializedName("state")
    val state: String?
)
