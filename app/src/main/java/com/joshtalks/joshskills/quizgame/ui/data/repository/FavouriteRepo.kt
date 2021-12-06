package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.AddFavouritePartner
import com.joshtalks.joshskills.quizgame.ui.data.model.AgoraCallResponse
import com.joshtalks.joshskills.quizgame.ui.data.model.AgoraToToken
import com.joshtalks.joshskills.quizgame.ui.data.model.Status
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse

class FavouriteRepo {

    suspend fun getFavourite(mentorId:String) = RetrofitInstanse.getRetrofitInstance()?.getFavourite(mentorId)

    suspend fun getAgoraFromToken(mentorId: String) = RetrofitInstanse.getRetrofitInstance()?.getAgoraFromToken(mapOf("from_mentor_id" to mentorId))

    suspend fun getChannelData(toMentorId: String?,channelName:String?) = RetrofitInstanse.getRetrofitInstance()?.getUserChannelId(AgoraToToken(toMentorId,channelName))

    suspend fun getStatus(userIdMentor: String?,status:String?) = RetrofitInstanse.getRetrofitInstance()?.changeUserStatus(
        Status(userIdMentor,status)
    )

    suspend fun addFavouritePartner(addFavouritePartner: AddFavouritePartner) = RetrofitInstanse.getRetrofitInstance()?.addUserAsFpp(addFavouritePartner)

}
