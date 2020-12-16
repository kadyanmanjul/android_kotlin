package com.joshtalks.joshskills.repository.server.recommendation

import com.google.gson.annotations.SerializedName

class RecommendationPostRequest(
    @SerializedName("gaid")
    var gaid: String,
    @SerializedName("user_segment")
    val test_ids: List<UserSegmentIDRequest>
)

class UserSegmentIDRequest(
    @SerializedName("id")
    var id: Int
)
