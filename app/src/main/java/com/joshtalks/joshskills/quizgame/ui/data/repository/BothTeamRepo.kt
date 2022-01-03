package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.RandomRoomData
import com.joshtalks.joshskills.quizgame.ui.data.model.RoomData
import com.joshtalks.joshskills.quizgame.ui.data.model.SaveCallDuration
import com.joshtalks.joshskills.quizgame.ui.data.model.SaveCallDurationRoomData
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse

class BothTeamRepo {
    suspend fun getRoomUserData(randomRoomData: RandomRoomData) =
        RetrofitInstanse.getRetrofitInstance()?.getRoomUserDataTemp(randomRoomData)

    suspend fun deleteUsersDataFromRoom(saveCallDurationRoomData: SaveCallDurationRoomData) =
        RetrofitInstanse.getRetrofitInstance()?.getDeleteUserFpp(saveCallDurationRoomData)

    suspend fun saveDurationOfCall(saveCallDuration: SaveCallDuration) =
        RetrofitInstanse.getRetrofitInstance()?.saveCallDuration(saveCallDuration)

}