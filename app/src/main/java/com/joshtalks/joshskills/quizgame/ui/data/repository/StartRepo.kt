package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.AddUserDb
import com.joshtalks.joshskills.quizgame.ui.data.model.Status
import com.joshtalks.joshskills.quizgame.ui.data.network.GameApiService
import com.joshtalks.joshskills.quizgame.util.ACTIVE
import com.joshtalks.joshskills.quizgame.util.IN_ACTIVE
import com.joshtalks.joshskills.repository.local.model.Mentor

class StartRepo(var api: GameApiService?) {
    suspend fun addUserInDb() = api?.addUserToDb(
        AddUserDb(
            Mentor.getInstance().getId(),
            Mentor.getInstance().getUser()?.firstName,
            Mentor.getInstance().getUser()?.photo
        )
    )

    suspend fun getHomeInactive() =
        api?.homeInactive(
            Status(Mentor.getInstance().getId(), IN_ACTIVE)
        )

    suspend fun getStatus() =
        api?.changeUserStatus(
            Status(Mentor.getInstance().getId(), ACTIVE)
        )
}