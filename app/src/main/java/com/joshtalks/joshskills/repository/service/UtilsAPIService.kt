package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.repository.local.model.MissedNotification
import retrofit2.Response
import retrofit2.http.GET

@JvmSuppressWildcards
interface UtilsAPIService {

    @GET("$DIR/notification/server_time/")
    suspend fun getServerTime(): Response<Long>

    @GET("$DIR/notification/missed_notification/")
    suspend fun getMissedNotifications(): Response<List<MissedNotification>>
}
