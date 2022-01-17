package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.GameApiService

class SearchRandomRepo(val api: GameApiService?) {
    suspend fun getSearchRandomData(userId: String) =
        api?.searchRandomUser(mapOf("user_id" to userId))

    suspend fun createRandomUserRoom(roomRandom: RoomRandom) =
        api?.createRandomRoom(roomRandom)

    suspend fun getRandomUserData(randomRoomData: RandomRoomData) =
        api?.getRandomRoomUserData(randomRoomData)

    suspend fun deleteUserData(deleteUserData: DeleteUserData) =
        api?.deleteUserDataFromRadius(deleteUserData)

    suspend fun clearRoomRadius(saveCallDurationRoomData: SaveCallDurationRoomData) =
        api?.clearRadius(saveCallDurationRoomData)

}