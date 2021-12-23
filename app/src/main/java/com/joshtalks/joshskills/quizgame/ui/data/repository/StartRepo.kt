package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.AddUserDb
import com.joshtalks.joshskills.quizgame.ui.data.model.Status
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse
import com.joshtalks.joshskills.quizgame.ui.main.view.fragment.ACTIVE
import com.joshtalks.joshskills.quizgame.ui.main.view.fragment.IN_ACTIVE
import com.joshtalks.joshskills.repository.local.model.Mentor

class StartRepo {
    var retrofitInstance = RetrofitInstanse.getRetrofitInstance()
    suspend fun addUserInDb() = retrofitInstance?.addUserToDb(
        AddUserDb(
            Mentor.getInstance().getUserId(),
            Mentor.getInstance().getUser()?.firstName,
            Mentor.getInstance().getUser()?.photo
        )
    )
    suspend fun getHomeInactive() =
        retrofitInstance?.homeInactive(
            Status(Mentor.getInstance().getUserId(), IN_ACTIVE)
        )

    suspend fun getStatus() =
        retrofitInstance?.changeUserStatus(
            Status(Mentor.getInstance().getUserId(), ACTIVE)
        )
}