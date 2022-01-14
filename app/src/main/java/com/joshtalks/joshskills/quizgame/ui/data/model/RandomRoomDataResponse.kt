package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

data class RandomRoomDataResponse(
    @SerializedName("room_id") var roomId: String,
    @SerializedName("teams") var teamData: TeamsDataRandom
)


data class TeamsDataRandom(
    @SerializedName("team1_id") var team1Id: String,
    @SerializedName("users_in_team1") var usersInTeam1: UsersInTeam1Random,
    @SerializedName("team2_id") var team2Id: String,
    @SerializedName("users_in_team2") var usersInTeam2: UsersInTeam2Random
)

data class UsersInTeam1Random(
    @SerializedName("user1") var user1: User1Random,
    @SerializedName("user2") var user2: User2Random
)

data class UsersInTeam2Random(
    @SerializedName("user3") var user3: User3Random,
    @SerializedName("user4") var user4: User4Random
)

data class User1Random(
    @SerializedName("token") var token: String,
    @SerializedName("channel_name") var channelName: String,
    @SerializedName("user_id") var userId: String,
    @SerializedName("image_url") var imageUrl: String,
    @SerializedName("username") var userName: String
)

data class User2Random(
    @SerializedName("token") var token: String,
    @SerializedName("channel_name") var channelName: String,
    @SerializedName("user_id") var userId: String,
    @SerializedName("image_url") var imageUrl: String,
    @SerializedName("username") var userName: String
)

data class User3Random(
    @SerializedName("token") var token: String,
    @SerializedName("channel_name") var channelName: String,
    @SerializedName("user_id") var userId: String,
    @SerializedName("image_url") var imageUrl: String,
    @SerializedName("username") var userName: String
)

data class User4Random(
    @SerializedName("token") var token: String,
    @SerializedName("channel_name") var channelName: String,
    @SerializedName("user_id") var userId: String,
    @SerializedName("image_url") var imageUrl: String,
    @SerializedName("username") var userName: String
)