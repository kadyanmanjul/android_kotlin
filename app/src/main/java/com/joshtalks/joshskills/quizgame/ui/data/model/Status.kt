package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

class Status (@SerializedName("user_id") var userId:String?,
              @SerializedName("status") var status:String?
              )