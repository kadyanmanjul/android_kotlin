package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse

class SearchOpponentRepo {
    suspend fun addToRoomRepo(teamId: ChannelName) =
        RetrofitInstanse.getRetrofitInstance()?.addUserToRoom(teamId)

    suspend fun getRoomUserData(randomRoomData: RandomRoomData) =
        RetrofitInstanse.getRetrofitInstance()?.getRoomUserDataTemp(randomRoomData)

    suspend fun deleteUserTeamData(teamDataDelete: TeamDataDelete) =
        RetrofitInstanse.getRetrofitInstance()?.getDeleteUserAndTeamFpp(teamDataDelete)

    suspend fun deleteUsersDataFromRoom(saveCallDurationRoomData: SaveCallDurationRoomData) =
        RetrofitInstanse.getRetrofitInstance()?.getDeleteUserFpp(saveCallDurationRoomData)

    suspend fun saveDurationOfCall(saveCallDuration: SaveCallDuration) =
        RetrofitInstanse.getRetrofitInstance()?.saveCallDuration(saveCallDuration)

}