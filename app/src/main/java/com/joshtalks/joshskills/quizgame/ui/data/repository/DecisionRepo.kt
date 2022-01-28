package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.GameApiService

class DecisionRepo(val api: GameApiService?) {
    suspend fun saveRoomData(saveRoomDetails: SaveRoomDetails) =
    api?.saveRoomDetails(saveRoomDetails)

    suspend fun getRoomDataTemp(randomRoomData: RandomRoomData) =
        api?.getRoomUserDataTemp(randomRoomData)

    suspend fun clearRoomRadius(saveCallDurationRoomData: SaveCallDurationRoomData) =
        api?.clearRadius(saveCallDurationRoomData)

    suspend fun deleteUsersDataFromRoom(saveCallDurationRoomData: SaveCallDurationRoomData) =
        api?.getDeleteUserFpp(saveCallDurationRoomData)

    suspend fun addFavouritePartner(addFavouritePartner: AddFavouritePartner) =
        api?.addUserAsFpp(addFavouritePartner)

    suspend fun playAgainData(playAgain: PlayAgain) =
        api?.playAgain(playAgain)

    suspend fun saveDurationOfCall(saveCallDuration: SaveCallDuration) =
        api?.saveCallDuration(saveCallDuration)

}