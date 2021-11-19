package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.ChannelName
import com.joshtalks.joshskills.quizgame.ui.data.model.RandomRoomData
import com.joshtalks.joshskills.quizgame.ui.data.model.RoomData
import com.joshtalks.joshskills.quizgame.ui.data.model.TeamDataDelete
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse

class SearchOpponentRepo {
    suspend fun addToRoomRepo(teamId:ChannelName) = RetrofitInstanse.api.addUserToRoom(teamId)

    suspend fun getRoomUserData(randomRoomData: RandomRoomData) = RetrofitInstanse.api.getRoomUserDataTemp(randomRoomData)

    suspend fun deleteUserTeamData(teamDataDelete: TeamDataDelete) = RetrofitInstanse.api.getDeleteUserAndTeamFpp(teamDataDelete)

    suspend fun deleteUsersDataFromRoom(randomRoomData: RandomRoomData) = RetrofitInstanse.api.getDeleteUserFpp(randomRoomData)

}