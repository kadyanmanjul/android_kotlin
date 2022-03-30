package com.joshtalks.joshskills.ui.group.analytics.data.network

import com.joshtalks.joshskills.base.constants.DIR
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

const val GROUPS_ANALYTICS_MENTOR_ID_API_KEY = "mentor_id"
const val GROUPS_ANALYTICS_EVENTS_API_KEY = "group_event_name"
const val GROUPS_ANALYTICS_GROUP_ID_API_KEY = "group_id"

interface GroupsAnalyticsService {

    @POST("$DIR/impression/track_group_impressions/")
    @JvmSuppressWildcards
    suspend fun groupImpressionDetails(
        @Body params: Map<String, Any?>
    ): Response<Unit>
}