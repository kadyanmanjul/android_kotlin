package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName


data class RoomUserData(@SerializedName("data") var data :List<TeamData?>? = null)

data class TeamData(@SerializedName("team_id") var teamId:String? = null,
                    @SerializedName("users") var userData : List<User?>? = null)

data class User(@SerializedName("user_id") var userId : String? = null,
                @SerializedName("user_name") var userName :String? = null,
                @SerializedName("image_url") var imageUrl:String? = null)

