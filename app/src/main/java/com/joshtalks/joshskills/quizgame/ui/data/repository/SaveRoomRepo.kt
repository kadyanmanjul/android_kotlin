package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse

class SaveRoomRepo {
    suspend fun saveRoomData(saveRoomDetails: SaveRoomDetails) = RetrofitInstanse.getRetrofitInstance()?.saveRoomDetails(saveRoomDetails)
    suspend fun getRoomDataTemp(randomRoomData: RandomRoomData) = RetrofitInstanse.getRetrofitInstance()?.getRoomUserDataTemp(randomRoomData)
    suspend fun clearRoomRadius(saveCallDurationRoomData: SaveCallDurationRoomData) = RetrofitInstanse.getRetrofitInstance()?.clearRadius(saveCallDurationRoomData)
    suspend fun deleteUsersDataFromRoom(saveCallDurationRoomData: SaveCallDurationRoomData) = RetrofitInstanse.getRetrofitInstance()?.getDeleteUserFpp(saveCallDurationRoomData)
    suspend fun addFavouritePartner(addFavouritePartner: AddFavouritePartner) = RetrofitInstanse.getRetrofitInstance()?.addUserAsFpp(addFavouritePartner)
    suspend fun playAgainData(playAgain: PlayAgain) = RetrofitInstanse.getRetrofitInstance()?.playAgain(playAgain)
}