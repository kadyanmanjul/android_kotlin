package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstance

class SearchRandomRepo {
    suspend fun getSearchRandomData(userId: String) =
        RetrofitInstance.getRetrofitInstance()?.searchRandomUser(mapOf("user_id" to userId))

    suspend fun createRandomUserRoom(roomRandom: RoomRandom) =
        RetrofitInstance.getRetrofitInstance()?.createRandomRoom(roomRandom)

    suspend fun getRandomUserData(randomRoomData: RandomRoomData) =
        RetrofitInstance.getRetrofitInstance()?.getRandomRoomUserData(randomRoomData)

    suspend fun deleteUserData(deleteUserData: DeleteUserData) =
        RetrofitInstance.getRetrofitInstance()?.deleteUserDataFromRadius(deleteUserData)

    suspend fun clearRoomRadius(saveCallDurationRoomData: SaveCallDurationRoomData) =
        RetrofitInstance.getRetrofitInstance()?.clearRadius(saveCallDurationRoomData)

}