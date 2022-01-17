package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.SaveCallDuration
import com.joshtalks.joshskills.quizgame.ui.data.model.SaveCallDurationRoomData
import com.joshtalks.joshskills.quizgame.ui.data.network.GameApiService

class RandomTeamMateFoundRepo(val api: GameApiService?) {
    suspend fun getUserDetails(mentorId: String) =
        api?.getUserDetails(mentorId, mentorId)

    suspend fun saveDurationOfCall(saveCallDuration: SaveCallDuration) =
        api?.saveCallDuration(saveCallDuration)

    suspend fun clearRoomRadius(saveCallDurationRoomData: SaveCallDurationRoomData) =
        api?.clearRadius(saveCallDurationRoomData)
}