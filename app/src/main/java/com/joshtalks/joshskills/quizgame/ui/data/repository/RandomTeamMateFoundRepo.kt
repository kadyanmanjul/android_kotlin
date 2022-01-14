package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.SaveCallDuration
import com.joshtalks.joshskills.quizgame.ui.data.model.SaveCallDurationRoomData
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstance

class RandomTeamMateFoundRepo {
    suspend fun getUserDetails(mentorId: String) =
        RetrofitInstance.getRetrofitInstance()?.getUserDetails(mentorId, mentorId)

    suspend fun saveDurationOfCall(saveCallDuration: SaveCallDuration) =
        RetrofitInstance.getRetrofitInstance()?.saveCallDuration(saveCallDuration)

    suspend fun clearRoomRadius(saveCallDurationRoomData: SaveCallDurationRoomData) =
        RetrofitInstance.getRetrofitInstance()?.clearRadius(saveCallDurationRoomData)
}