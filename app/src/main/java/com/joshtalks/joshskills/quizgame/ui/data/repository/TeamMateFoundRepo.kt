package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.DeleteUserData
import com.joshtalks.joshskills.quizgame.ui.data.model.TeamDataDelete
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse

class TeamMateFoundRepo {
    suspend fun getUserDetails(mentorId:String) = RetrofitInstanse.api.getUserDetails(mentorId)

    suspend fun deleteUserData(teamDataDelete: TeamDataDelete) = RetrofitInstanse.api.getDeleteUserAndTeamFpp(teamDataDelete)

}