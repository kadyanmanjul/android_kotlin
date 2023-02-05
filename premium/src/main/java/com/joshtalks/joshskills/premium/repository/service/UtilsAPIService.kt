package com.joshtalks.joshskills.premium.repository.service

import com.joshtalks.joshskills.premium.core.firestore.NotificationAnalyticsRequest
import com.joshtalks.joshskills.premium.core.notification.model.ScheduleNotification
import com.joshtalks.joshskills.premium.repository.local.model.MissedNotification
import retrofit2.Response
import retrofit2.http.*

@JvmSuppressWildcards
interface UtilsAPIService {

    @GET("$DIR/notification/server_time/")
    suspend fun getServerTime(): Response<Long>

    @GET("$DIR/notification/missed_notification/")
    suspend fun getMissedNotifications(@Query("mentor_id") mentorId: String): Response<List<MissedNotification>>

    @POST("$DIR/notification/analytics_v3/")
    suspend fun engageNewNotificationAsync(@Body params: List<NotificationAnalyticsRequest>): Response<Unit>

    @GET("$DIR/notification/client_side/")
    suspend fun getFTScheduledNotifications(@Query("test_id") course: String = "None", @Query("daily") daily: String = "None"): List<ScheduleNotification>

    @PATCH("$DIR/notification/update_status/")
    suspend fun updateNotificationStatus(@Body mapOf: Map<String, Any>): Map<String, Any>
}
