package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse
import retrofit2.http.Body

class SearchRandomRepo {
    suspend fun getSearchRandomData(userId: String) = RetrofitInstanse.api.searchRandomUser(mapOf("user_id" to userId))

    suspend fun getStatus(userIdMentor: String?,status:String?) = RetrofitInstanse.api.changeUserStatus(
        Status(userIdMentor,status)
    )

    suspend fun createRandomUserRoom(roomRandom: RoomRandom) = RetrofitInstanse.api.createRandomRoom(roomRandom)

    suspend fun getRandomUserData(randomRoomData: RandomRoomData) = RetrofitInstanse.api.getRandomRoomUserData(randomRoomData)

    suspend fun deleteUserData(deleteUserData: DeleteUserData) = RetrofitInstanse.api.deleteUserDataFromRadius(deleteUserData)
    suspend fun clearRoomRadius(randomRoomData: RandomRoomData) = RetrofitInstanse.api.clearRadius(randomRoomData)
}