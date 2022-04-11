package com.joshtalks.joshskills.ui.activity_feed.repository

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedList
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Path

class ActivityFeedRepository {
    private val commonNetworkService by lazy { AppObjectController.commonNetworkService }

    suspend fun getActivityFeedData(timestamp: String): Response<ActivityFeedList> =
        commonNetworkService.getActivityFeedData(timestamp)
    suspend fun engageActivityFeedTime(userProfileImpressionId: String,params: Map<String, Long>): Any=
        commonNetworkService.engageActivityFeedTime(
            userProfileImpressionId,
            params
        )

}