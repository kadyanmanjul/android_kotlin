package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.SaveCallDuration
import com.joshtalks.joshskills.quizgame.ui.data.model.TeamDataDelete
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstance

class TeamMateFoundRepo {
    suspend fun getUserDetails(mentorId: String) =
        RetrofitInstance.getRetrofitInstance()?.getUserDetails(mentorId, mentorId)

    suspend fun deleteUserData(teamDataDelete: TeamDataDelete) =
        RetrofitInstance.getRetrofitInstance()?.getDeleteUserAndTeamFpp(teamDataDelete)

    suspend fun saveDurationOfCall(saveCallDuration: SaveCallDuration) =
        RetrofitInstance.getRetrofitInstance()?.saveCallDuration(saveCallDuration)
}