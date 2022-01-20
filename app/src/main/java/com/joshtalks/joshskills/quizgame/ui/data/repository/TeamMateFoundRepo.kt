package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.SaveCallDuration
import com.joshtalks.joshskills.quizgame.ui.data.model.TeamDataDelete
import com.joshtalks.joshskills.quizgame.ui.data.network.GameApiService

class TeamMateFoundRepo(val api: GameApiService?) {
    suspend fun getUserDetails(mentorId: String) =
        api?.getUserDetails(mentorId, mentorId)

    suspend fun deleteUserData(teamDataDelete: TeamDataDelete) =
        api?.getDeleteUserAndTeamFpp(teamDataDelete)

    suspend fun saveDurationOfCall(saveCallDuration: SaveCallDuration) =
        api?.saveCallDuration(saveCallDuration)
}