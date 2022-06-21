package com.joshtalks.badebhaiya.repository.service

import com.joshtalks.badebhaiya.feed.model.Users
import com.joshtalks.badebhaiya.impressions.Impression
import com.joshtalks.badebhaiya.profile.request.FollowRequest
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.profile.response.ProfileResponse
import retrofit2.Response
import retrofit2.http.*

interface ProfileNetworkService {

    @GET("$DIR/user/personal_profile/{id}/")
    suspend fun getProfileForUser(
        @Path("id") userId: String,
        @Query("from_page") page: String
    ): Response<ProfileResponse>

    @GET("$DIR/user/deeplink/profile/{id}/")
    suspend fun getProfileWithoutToken(
        @Path("id") userId: String,
        source: String
    ): Response<ProfileResponse>

    @POST("$DIR/user/follow/")
    suspend fun updateFollowStatus(@Body followRequest: FollowRequest): Response<Void>

    @POST("$DIR/user/unfollow/")
    suspend fun updateUnfollowStatus(@Body followRequest: FollowRequest): Response<Void>

    @POST("$DIR/reminder/set_reminder/")
    suspend fun setReminderForRoom(@Body reminderRequest: ReminderRequest): Response<Any>

    @POST("$DIR/impressions/track_impressions/")
    suspend fun sendEvent(@Body event:Impression):Response<Void>

}