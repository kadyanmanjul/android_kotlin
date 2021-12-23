package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.AddFavouritePartner
import com.joshtalks.joshskills.quizgame.ui.data.model.AgoraToToken
import com.joshtalks.joshskills.quizgame.ui.data.model.Status
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse

class ChoiceRepo {
    var retrofitInstance = RetrofitInstanse.getRetrofitInstance()

    suspend fun getStatus(userIdMentor: String?, status: String?) =
        retrofitInstance?.changeUserStatus(
            Status(userIdMentor, status)
        )

    suspend fun getHomeInactive(userIdMentor: String?, status: String?) =
        retrofitInstance?.homeInactive(
            Status(userIdMentor, status)
        )

    suspend fun getChannelData(toMentorId: String?, channelName: String?) =
        retrofitInstance?.getUserChannelId(
            AgoraToToken(toMentorId, channelName)
        )

    suspend fun addFavouritePartner(addFavouritePartner: AddFavouritePartner) =
        retrofitInstance?.addUserAsFpp(addFavouritePartner)


}