package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.RandomRoomData
import com.joshtalks.joshskills.quizgame.ui.data.model.SaveCallDuration
import com.joshtalks.joshskills.quizgame.ui.data.model.SaveCallDurationRoomData
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstance

class BothTeamRepo {
    suspend fun getRoomUserData(randomRoomData: RandomRoomData) =
        RetrofitInstance.getRetrofitInstance()?.getRoomUserDataTemp(randomRoomData)

    suspend fun deleteUsersDataFromRoom(saveCallDurationRoomData: SaveCallDurationRoomData) =
        RetrofitInstance.getRetrofitInstance()?.getDeleteUserFpp(saveCallDurationRoomData)

    suspend fun saveDurationOfCall(saveCallDuration: SaveCallDuration) =
        RetrofitInstance.getRetrofitInstance()?.saveCallDuration(saveCallDuration)

}