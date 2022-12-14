package com.joshtalks.joshskills.leaderboard

import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.repository.service.DIR
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

object LeaderBoardNetwork{
    val leaderboardNetworkService: LeaderBoardNetworkService by lazy {
        AppObjectController.retrofit.create(LeaderBoardNetworkService::class.java)
    }
}
interface LeaderBoardNetworkService {

    @GET("$DIR/leaderboard/get_filtered_leaderboard/")
    suspend fun searchLeaderboardMember(
        @Query("key") word: String,
        @Query("page") page: Int,
        @Query("interval_type") intervalType: LeaderboardType,
        @Query("course_id") courseId: String
    ): Response<List<LeaderboardMentor>>

    @GET("$DIR/leaderboard/get_previous_leaderboard/")
    suspend fun getPreviousLeaderboardData(
        @Query("mentor_id") mentorId: String,
        @Query("interval_type") intervalType: String,
        @Query("course_id") courseId: String
    ): Response<PreviousLeaderboardResponse>


    @GET("$DIR/leaderboard/get_leaderboard/")
    suspend fun getLeaderBoardData(
        @Query("mentor_id") mentorId: String,
        @Query("interval_type") interval: String,
        @Query("course_id") course_id: String?
    ): Response<LeaderboardResponse>

    // not using this
    @GET("$DIR/leaderboard/get_leaderboard/")
    suspend fun getLeaderBoardDataViaPage(
        @Query("mentor_id") mentorId: String,
        @Query("interval_type") interval: String,
        @Query("above_list_page") page: Int
    ): Response<LeaderboardResponse>
}