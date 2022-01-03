package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.SaveCallDuration
import com.joshtalks.joshskills.quizgame.ui.data.model.SaveCallDurationRoomData
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse

class RandomTeamMateFoundRepo {
    suspend fun getUserDetails(mentorId: String) =
        RetrofitInstanse.getRetrofitInstance()?.getUserDetails(mentorId)

    suspend fun saveDurationOfCall(saveCallDuration: SaveCallDuration) =
        RetrofitInstanse.getRetrofitInstance()?.saveCallDuration(saveCallDuration)

    suspend fun clearRoomRadius(saveCallDurationRoomData: SaveCallDurationRoomData) =
        RetrofitInstanse.getRetrofitInstance()?.clearRadius(saveCallDurationRoomData)
}