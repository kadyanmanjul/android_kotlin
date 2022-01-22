package com.joshtalks.joshskills.repository.local.model

data class KFactor(
    var duration_filter: Boolean,
    var caller: PersonOnTheCall,
    var receiver: PersonOnTheCall

)

data class Caller(
    var photo_url: String = "",
    var name: String,
    var district: String,
    var state: String
)

data class PersonOnTheCall(
    var agora_mentor_id: Int,
    var city: String?,
    var state: String?
)
