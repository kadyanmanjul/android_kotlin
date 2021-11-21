package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.RandomRoomData
import com.joshtalks.joshskills.quizgame.ui.data.model.RoomData
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse

class BothTeamRepo {
    suspend fun getRoomUserData(randomRoomData: RandomRoomData) = RetrofitInstanse.api.getRoomUserDataTemp(randomRoomData)

    suspend fun deleteUsersDataFromRoom(randomRoomData: RandomRoomData) = RetrofitInstanse.api.getDeleteUserFpp(randomRoomData)

}