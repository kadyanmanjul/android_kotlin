package com.joshtalks.badebhaiya.repository.service

import com.joshtalks.badebhaiya.profile.request.FollowRequest
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.profile.response.ProfileResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ProfileNetworkService {

    @GET("$DIR/user/personal_profile/{id}/")
    suspend fun getProfileForUser(@Path("id") userId: String): Response<ProfileResponse>

    @POST("$DIR/user/follow/")
    suspend fun updateFollowStatus(@Body followRequest: FollowRequest): Response<Void>

    @POST("$DIR/reminder/set_reminder/")
    suspend fun setReminderForRoom(@Body reminderRequest: ReminderRequest): Response<Any>
}