package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

data class RandomRoomResponseTemp(
    @SerializedName("room_id") var roomId :String,
    @SerializedName("teams") var teamData :TeamsData
)

data class TeamsDataFpp(
    @SerializedName("team1_id") var team1Id:String,
    @SerializedName("users_in_team1") var usersInTeam1 :UsersInTeam1,
    @SerializedName("team2_id") var team2Id :String,
    @SerializedName("users_in_team2") var usersInTeam2: UsersInTeam2
    )

data class UsersInTeam1Fpp(
    @SerializedName("user1") var user1: User1,
    @SerializedName("user2") var user2: User2
)

data class UsersInTeam2Fpp(
    @SerializedName("user3") var user3: User3,
    @SerializedName("user4") var user4: User4
)

data class User1Fpp(
    @SerializedName("token") var token : String,
    @SerializedName("channel_name") var channelName : String,
    @SerializedName("user_id") var userId : String,
    @SerializedName("image_url") var imageUrl : String,
    @SerializedName("username") var userName : String
)

data class User2Fpp(
    @SerializedName("token") var token : String,
    @SerializedName("channel_name") var channelName : String,
    @SerializedName("user_id") var userId : String,
    @SerializedName("image_url") var imageUrl : String,
    @SerializedName("username") var userName : String
)

data class User3Fpp(
    @SerializedName("token") var token : String,
    @SerializedName("channel_name") var channelName : String,
    @SerializedName("user_id") var userId : String,
    @SerializedName("image_url") var imageUrl : String,
    @SerializedName("username") var userName : String
)

data class User4Fpp(
    @SerializedName("token") var token : String,
    @SerializedName("channel_name") var channelName : String,
    @SerializedName("user_id") var userId : String,
    @SerializedName("image_url") var imageUrl : String,
    @SerializedName("username") var userName : String
)