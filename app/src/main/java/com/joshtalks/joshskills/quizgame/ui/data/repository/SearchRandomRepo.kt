package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse

class SearchRandomRepo {
    suspend fun getSearchRandomData(userId: String) =
        RetrofitInstanse.getRetrofitInstance()?.searchRandomUser(mapOf("user_id" to userId))

    suspend fun createRandomUserRoom(roomRandom: RoomRandom) =
        RetrofitInstanse.getRetrofitInstance()?.createRandomRoom(roomRandom)

    suspend fun getRandomUserData(randomRoomData: RandomRoomData) =
        RetrofitInstanse.getRetrofitInstance()?.getRandomRoomUserData(randomRoomData)

    suspend fun deleteUserData(deleteUserData: DeleteUserData) =
        RetrofitInstanse.getRetrofitInstance()?.deleteUserDataFromRadius(deleteUserData)

    suspend fun clearRoomRadius(saveCallDurationRoomData: SaveCallDurationRoomData) =
        RetrofitInstanse.getRetrofitInstance()?.clearRadius(saveCallDurationRoomData)

}