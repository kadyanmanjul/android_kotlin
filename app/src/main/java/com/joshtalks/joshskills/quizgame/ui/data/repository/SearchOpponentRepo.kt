package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstance

class SearchOpponentRepo {
    suspend fun addToRoomRepo(teamId: ChannelName) =
        RetrofitInstance.getRetrofitInstance()?.addUserToRoom(teamId)

    suspend fun getRoomUserData(randomRoomData: RandomRoomData) =
        RetrofitInstance.getRetrofitInstance()?.getRoomUserDataTemp(randomRoomData)

    suspend fun deleteUserTeamData(teamDataDelete: TeamDataDelete) =
        RetrofitInstance.getRetrofitInstance()?.getDeleteUserAndTeamFpp(teamDataDelete)

    suspend fun deleteUsersDataFromRoom(saveCallDurationRoomData: SaveCallDurationRoomData) =
        RetrofitInstance.getRetrofitInstance()?.getDeleteUserFpp(saveCallDurationRoomData)

    suspend fun saveDurationOfCall(saveCallDuration: SaveCallDuration) =
        RetrofitInstance.getRetrofitInstance()?.saveCallDuration(saveCallDuration)

}