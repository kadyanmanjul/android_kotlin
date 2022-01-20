package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.GameApiService
import com.joshtalks.joshskills.repository.local.model.Mentor

class FavouriteRepo(val api: GameApiService?) {

    suspend fun getFavourite(mentorId: String) =
        api?.getFavourite(mentorId, mentorId)

    suspend fun getAgoraFromToken(fromToken: AgoraFromToken) =
        api?.getAgoraFromToken(fromToken)


    suspend fun getChannelData(toMentorId: String?, channelName: String?) =
        api?.getUserChannelId(AgoraToToken(toMentorId, channelName, Mentor.getInstance().getId()))

    suspend fun addFavouritePartner(addFavouritePartner: AddFavouritePartner) =
        api?.addUserAsFpp(addFavouritePartner)

}
