package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.AddFavouritePartner
import com.joshtalks.joshskills.quizgame.ui.data.model.AgoraToToken
import com.joshtalks.joshskills.quizgame.ui.data.model.Status
import com.joshtalks.joshskills.quizgame.ui.data.network.GameApiService
import com.joshtalks.joshskills.repository.local.model.Mentor

class ChoiceRepo(val api: GameApiService?) {

    suspend fun getStatus(userIdMentor: String?, status: String?) =
        api?.changeUserStatus(
            Status(userIdMentor, status)
        )

    suspend fun getHomeInactive(userIdMentor: String?, status: String?) =
        api?.homeInactive(
            Status(userIdMentor, status)
        )

    suspend fun getChannelData(toMentorId: String?, channelName: String?) =
        api?.getUserChannelId(
            AgoraToToken(toMentorId, channelName, Mentor.getInstance().getId())
        )

    suspend fun addFavouritePartner(addFavouritePartner: AddFavouritePartner) =
        api?.addUserAsFpp(addFavouritePartner)


}