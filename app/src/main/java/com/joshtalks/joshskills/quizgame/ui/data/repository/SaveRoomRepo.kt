package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstance

class SaveRoomRepo {
    suspend fun saveRoomData(saveRoomDetails: SaveRoomDetails) =
        RetrofitInstance.getRetrofitInstance()?.saveRoomDetails(saveRoomDetails)

    suspend fun getRoomDataTemp(randomRoomData: RandomRoomData) =
        RetrofitInstance.getRetrofitInstance()?.getRoomUserDataTemp(randomRoomData)

    suspend fun clearRoomRadius(saveCallDurationRoomData: SaveCallDurationRoomData) =
        RetrofitInstance.getRetrofitInstance()?.clearRadius(saveCallDurationRoomData)

    suspend fun deleteUsersDataFromRoom(saveCallDurationRoomData: SaveCallDurationRoomData) =
        RetrofitInstance.getRetrofitInstance()?.getDeleteUserFpp(saveCallDurationRoomData)

    suspend fun addFavouritePartner(addFavouritePartner: AddFavouritePartner) =
        RetrofitInstance.getRetrofitInstance()?.addUserAsFpp(addFavouritePartner)

    suspend fun playAgainData(playAgain: PlayAgain) =
        RetrofitInstance.getRetrofitInstance()?.playAgain(playAgain)

    suspend fun saveDurationOfCall(saveCallDuration: SaveCallDuration) =
        RetrofitInstance.getRetrofitInstance()?.saveCallDuration(saveCallDuration)

}