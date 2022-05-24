package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.core.firestore.NotificationAnalyticsRequest
import com.joshtalks.joshskills.repository.local.model.MissedNotification
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@JvmSuppressWildcards
interface UtilsAPIService {

    @GET("$DIR/notification/server_time/")
    suspend fun getServerTime(): Response<Long>

    @GET("$DIR/notification/missed_notification/")
    suspend fun getMissedNotifications(): Response<List<MissedNotification>>

    @POST("$DIR/notification/analytics/")
    suspend fun engageNewNotificationAsync(@Body params: List<NotificationAnalyticsRequest>): Response<Void>
}
