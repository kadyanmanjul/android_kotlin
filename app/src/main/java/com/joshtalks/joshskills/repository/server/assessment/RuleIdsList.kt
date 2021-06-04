package com.joshtalks.joshskills.repository.server.assessment


import com.google.gson.annotations.SerializedName

data class RuleIdsList(
    @SerializedName("rules_id")
    val rulesId: List<Int>?
)