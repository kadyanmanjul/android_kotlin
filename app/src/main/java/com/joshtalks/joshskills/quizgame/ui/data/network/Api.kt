package com.joshtalks.joshskills.quizgame.ui.data.network

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface Api {
     // FPP

     @GET("fpp/favourite_practise_partner/")
     suspend fun getFavourite(@Query("mentor_id") mentorId:String)
             : Response<FavouriteList>

     @POST("fpp/quiz_agora_from_token/")
     suspend fun getAgoraFromToken(@Body params: Map<String, String>)
             : Response<ChannelData>

     @POST("fpp/quiz_agora_to_token/")
     suspend fun getUserChannelId(@Body params: AgoraToToken)
             : Response<AgoraToTokenResponse>

     @POST("fpp/add_to_room/")
     suspend fun addUserToRoom(@Body params:ChannelName) : Response<AddToRoomResponse>

     //this is use when delete data after create room
     @POST("fpp/delete_user_from_firebase_fpp/")
     suspend fun getDeleteUserFpp(@Body params: RandomRoomData) : Response<Success>

     //this is use for when delete team data or user own data
     @POST("fpp/delete_user_from_redis_team_queue/")
     suspend fun getDeleteUserAndTeamFpp(@Body params: TeamDataDelete) : Response<Success>

     //Random

     @POST("random/search_random_user/")
     suspend fun searchRandomUser(@Body params: Map<String, String>) : Response<SearchRandomResponse>

     @POST("random/create_room_for_random/")
     suspend fun createRandomRoom(@Body params: RoomRandom) : Response<RoomData>

     //isko hi question me fpp or save fpp me bhi user karna hai
     //yaha hame phele check karna hai room id bani ya nahi agar ban chuki hai tu hame clear radius karna hai jsi
     // jis se room data or firebase vo user delete ho jaye
     //agar room nahi bana hai tu sirf user ko dlete karna hai
     @POST("random/delete_user_data/")
     suspend fun clearRadius(@Body params : RandomRoomData) : Response<Success>

     @POST("random/delete_random_user_from_redis/")
     suspend fun deleteUserDataFromRadius(@Body params: DeleteUserData) : Response<Success>

     //Quiz Game

     @POST("quiz/change_status/")
     suspend fun changeUserStatus(@Body params: Status):Response<Success>

     @GET("quiz/userdata/")
     suspend fun getUserDetails(@Query("mentor_id") mentorId: String)
     :Response<UserDetails>

     @POST("quiz/questions/")
     suspend fun getQuestionList():Response<QuestionResponse>

     @POST("quiz/select_option/")
     suspend fun getSelectAnswer(@Body params: SelectOption) : Response<SelectOptionResponse>

     @POST("quiz/display_option/")
     suspend fun getDisplayData(@Body params:DisplayAnswer) : Response<DisplayAnswerResponse>

     @POST("quiz/save_game_details/")
     suspend fun saveRoomDetails(@Body params : SaveRoomDetails) :Response<Success>

     @POST("quiz/get_users_from_roomid/")
     suspend fun getRoomUserDataTemp(@Body params :RandomRoomData) : Response<RandomRoomResponse>
}
