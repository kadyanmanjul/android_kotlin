package com.joshtalks.joshskills.repository.service

import retrofit2.Response
import retrofit2.http.GET

@JvmSuppressWildcards
interface UtilsAPIService {

    @GET("$DIR/notification/server_time/")
    suspend fun getServerTime(): Response<Long>
}
