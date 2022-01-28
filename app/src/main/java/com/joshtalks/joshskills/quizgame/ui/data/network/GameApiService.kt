package com.joshtalks.joshskills.quizgame.ui.data.network

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

const val DIR = "api/skill/v1"
const val GAME_ANALYTICS_MENTOR_ID_API_KEY = "user_id"
const val GAME_ANALYTICS_EVENTS_API_KEY = "game_event_name"

interface GameApiService {
    // FPP
    @GET("$DIR/fpp/favourite_practise_partner/")
    suspend fun getFavourite(@Query("mentor_id") mentorId: String, @Query("user_id") userId: String)
            : Response<FavouriteList>

    @POST("$DIR/fpp/quiz_agora_from_token/")
    suspend fun getAgoraFromToken(@Body params: AgoraFromToken)
            : Response<ChannelData>

    @POST("$DIR/fpp/quiz_agora_to_token/")
    suspend fun getUserChannelId(@Body params: AgoraToToken)
            : Response<AgoraToTokenResponse>

    @POST("$DIR/fpp/add_to_room/")
    suspend fun addUserToRoom(@Body params: ChannelName): Response<AddToRoomResponse>

    //this is use when delete data after create room
    @POST("$DIR/fpp/delete_user_from_firebase_fpp/")
    suspend fun getDeleteUserFpp(@Body params: SaveCallDurationRoomData): Response<Success>

    //this is use for when delete team data or user own data
    @POST("$DIR/fpp/delete_user_from_redis_team_queue/")
    suspend fun getDeleteUserAndTeamFpp(@Body params: TeamDataDelete): Response<Success>

    @POST("$DIR/fpp/addfpp/")
    suspend fun addUserAsFpp(@Body params: AddFavouritePartner): Response<Success>

    //Random

    @POST("$DIR/random/search_random_user/")
    suspend fun searchRandomUser(@Body params: Map<String, String>): Response<SearchRandomResponse>

    @POST("$DIR/random/create_room_for_random/")
    suspend fun createRandomRoom(@Body params: RoomRandom): Response<RoomData>

    //isko hi question me fpp or save fpp me bhi user karna hai
    //yaha hame phele check karna hai room id bani ya nahi agar ban chuki hai tu hame clear radius karna hai jsi
    // jis se room data or firebase vo user delete ho jaye
    //agar room nahi bana hai tu sirf user ko dlete karna hai

    @POST("$DIR/random/get_random_users_from_roomid/")
    suspend fun getRandomRoomUserData(@Body prams: RandomRoomData): Response<RandomRoomDataResponse>

    @POST("$DIR/random/delete_user_data/")
    suspend fun clearRadius(@Body params: SaveCallDurationRoomData): Response<Success>

    @POST("$DIR/random/delete_random_user_from_redis/")
    suspend fun deleteUserDataFromRadius(@Body params: DeleteUserData): Response<Success>

    //Quiz Game

    @POST("$DIR/quiz/change_status/")
    suspend fun changeUserStatus(@Body params: Status): Response<Success>

    @GET("$DIR/quiz/userdata/")
    suspend fun getUserDetails(
        @Query("mentor_id") mentorId: String,
        @Query("user_id") userId: String
    )
            : Response<UserDetails>

    @POST("$DIR/quiz/questions/")
    suspend fun getQuestionList(@Body params: QuestionRequest): Response<QuestionResponse>

    @POST("$DIR/quiz/select_option/")
    suspend fun getSelectAnswer(@Body params: SelectOption): Response<SelectOptionResponse>

    @POST("$DIR/quiz/display_option/")
    suspend fun getDisplayData(@Body params: DisplayAnswer): Response<DisplayAnswerResponse>

    @POST("$DIR/quiz/save_game_details/")
    suspend fun saveRoomDetails(@Body params: SaveRoomDetails): Response<SaveRoomDetailsResponse>

    @POST("$DIR/quiz/get_users_from_roomid/")
    suspend fun getRoomUserDataTemp(@Body params: RandomRoomData): Response<RandomRoomResponse>

    @POST("$DIR/quiz/play_again/")
    suspend fun playAgain(@Body params: PlayAgain): Response<Success>

    @POST("$DIR/quiz/adduser/")
    suspend fun addUserToDb(@Body params: AddUserDb): Response<Success>

    @POST("$DIR/quiz/back_from_main_screen/")
    suspend fun homeInactive(@Body params: Status): Response<Success>

    @POST("$DIR/quiz/save_call_duration/")
    suspend fun saveCallDuration(@Body prams: SaveCallDuration): Response<CallDurationResponse>

    @POST("$DIR/quiz/impression/track_game_impressions/")
    @JvmSuppressWildcards
    suspend fun gameImpressionDetails(@Body params: Map<String, Any?>): Response<Unit>
}
