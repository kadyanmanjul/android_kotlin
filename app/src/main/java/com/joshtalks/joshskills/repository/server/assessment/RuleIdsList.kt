package com.joshtalks.joshskills.repository.server.assessment


import com.google.gson.annotations.SerializedName

data class RuleIdsList(
    @SerializedName("rules_id")
    val totalRulesIds: ArrayList<Int>?,
    @SerializedName("rule_completed_by_user")
    val rulesCompletedIds: ArrayList<Int>?
)