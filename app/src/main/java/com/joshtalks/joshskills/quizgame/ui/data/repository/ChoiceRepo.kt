package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.AgoraToToken
import com.joshtalks.joshskills.quizgame.ui.data.model.Status
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse

class ChoiceRepo {
    suspend fun getStatus(userIdMentor: String?,status:String?) = RetrofitInstanse.getRetrofitInstance()?.changeUserStatus(
        Status(userIdMentor,status)
    )

    suspend fun getHomeInactive(userIdMentor: String?,status:String?) = RetrofitInstanse.getRetrofitInstance()?.homeInactive(
        Status(userIdMentor,status)
    )

    suspend fun getChannelData(toMentorId: String?,channelName:String?) = RetrofitInstanse.getRetrofitInstance()?.getUserChannelId(
        AgoraToToken(toMentorId,channelName)
    )


}