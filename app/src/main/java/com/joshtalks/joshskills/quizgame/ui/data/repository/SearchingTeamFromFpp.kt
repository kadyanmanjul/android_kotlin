package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.GameApiService

class SearchingTeamFromFpp(val api: GameApiService?) {
    suspend fun addToRoomRepo(teamId: ChannelName) = api?.addUserToRoom(teamId)

    suspend fun getRoomUserData(randomRoomData: RandomRoomData) =
        api?.getRoomUserDataTemp(randomRoomData)

    suspend fun deleteUserTeamData(teamDataDelete: TeamDataDelete) =
        api?.getDeleteUserAndTeamFpp(teamDataDelete)

    suspend fun deleteUsersDataFromRoom(saveCallDurationRoomData: SaveCallDurationRoomData) =
        api?.getDeleteUserFpp(saveCallDurationRoomData)

    suspend fun saveDurationOfCall(saveCallDuration: SaveCallDuration) =
        api?.saveCallDuration(saveCallDuration)

}