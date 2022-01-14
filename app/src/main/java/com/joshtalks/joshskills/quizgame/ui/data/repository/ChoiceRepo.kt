package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.AddFavouritePartner
import com.joshtalks.joshskills.quizgame.ui.data.model.AgoraToToken
import com.joshtalks.joshskills.quizgame.ui.data.model.Status
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstance
import com.joshtalks.joshskills.repository.local.model.Mentor

class ChoiceRepo {
    var retrofitInstance = RetrofitInstance.getRetrofitInstance()

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
            AgoraToToken(toMentorId, channelName, Mentor.getInstance().getId())
        )

    suspend fun addFavouritePartner(addFavouritePartner: AddFavouritePartner) =
        retrofitInstance?.addUserAsFpp(addFavouritePartner)


}