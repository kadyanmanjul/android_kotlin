package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse

class QuestionRepo {
    suspend fun getQuestion(questionRequest: QuestionRequest) =
        RetrofitInstanse.getRetrofitInstance()?.getQuestionList(questionRequest)

    suspend fun getSelectAnswer(
        roomId: String,
        questionId: String,
        choiceId: String,
        teamId: String
    ) = RetrofitInstanse.getRetrofitInstance()
        ?.getSelectAnswer(SelectOption(roomId, questionId, choiceId, teamId))

    suspend fun getDisplayCorrectAnswer(roomId: String, questionId: String) =
        RetrofitInstanse.getRetrofitInstance()?.getDisplayData(DisplayAnswer(roomId, questionId))

    suspend fun getRoomDataTemp(randomRoomData: RandomRoomData) =
        RetrofitInstanse.getRetrofitInstance()?.getRoomUserDataTemp(randomRoomData)

    suspend fun clearRoomRadius(saveCallDurationRoomData: SaveCallDurationRoomData) =
        RetrofitInstanse.getRetrofitInstance()?.clearRadius(saveCallDurationRoomData)

    suspend fun deleteUsersDataFromRoom(saveCallDurationRoomData: SaveCallDurationRoomData) =
        RetrofitInstanse.getRetrofitInstance()?.getDeleteUserFpp(saveCallDurationRoomData)

    suspend fun saveDurationOfCall(saveCallDuration: SaveCallDuration) =
        RetrofitInstanse.getRetrofitInstance()?.saveCallDuration(saveCallDuration)

}