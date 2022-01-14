package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstance
import com.joshtalks.joshskills.repository.local.model.Mentor

class FavouriteRepo {

    suspend fun getFavourite(mentorId: String) =
        RetrofitInstance.getRetrofitInstance()?.getFavourite(mentorId, mentorId)

    suspend fun getAgoraFromToken(fromToken: AgoraFromToken) =
        RetrofitInstance.getRetrofitInstance()
            ?.getAgoraFromToken(fromToken)


    suspend fun getChannelData(toMentorId: String?, channelName: String?) =
        RetrofitInstance.getRetrofitInstance()
            ?.getUserChannelId(AgoraToToken(toMentorId, channelName, Mentor.getInstance().getId()))

    suspend fun addFavouritePartner(addFavouritePartner: AddFavouritePartner) =
        RetrofitInstance.getRetrofitInstance()?.addUserAsFpp(addFavouritePartner)

}
