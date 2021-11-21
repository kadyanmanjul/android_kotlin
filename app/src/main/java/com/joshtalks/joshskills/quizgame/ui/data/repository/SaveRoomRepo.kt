package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.RandomRoomData
import com.joshtalks.joshskills.quizgame.ui.data.model.SaveRoomDetails
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse

class SaveRoomRepo {
    suspend fun saveRoomData(saveRoomDetails: SaveRoomDetails) = RetrofitInstanse.api.saveRoomDetails(saveRoomDetails)
    suspend fun getRoomDataTemp(randomRoomData: RandomRoomData) = RetrofitInstanse.api.getRoomUserDataTemp(randomRoomData)
    suspend fun clearRoomRadius(randomRoomData: RandomRoomData) = RetrofitInstanse.api.clearRadius(randomRoomData)
    suspend fun deleteUsersDataFromRoom(randomRoomData: RandomRoomData) = RetrofitInstanse.api.getDeleteUserFpp(randomRoomData)

}