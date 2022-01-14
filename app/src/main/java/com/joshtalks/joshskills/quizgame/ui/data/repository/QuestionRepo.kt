package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstance
import com.joshtalks.joshskills.repository.local.model.Mentor

class QuestionRepo {
    suspend fun getQuestion(questionRequest: QuestionRequest) =
        RetrofitInstance.getRetrofitInstance()?.getQuestionList(questionRequest)

    suspend fun getSelectAnswer(
        roomId: String,
        questionId: String,
        choiceId: String,
        teamId: String
    ) = RetrofitInstance.getRetrofitInstance()
        ?.getSelectAnswer(
            SelectOption(
                roomId,
                questionId,
                choiceId,
                teamId,
                Mentor.getInstance().getId()
            )
        )

    suspend fun getDisplayCorrectAnswer(roomId: String, questionId: String) =
        RetrofitInstance.getRetrofitInstance()
            ?.getDisplayData(DisplayAnswer(roomId, questionId, Mentor.getInstance().getUserId()))

    suspend fun getRoomDataTemp(randomRoomData: RandomRoomData) =
        RetrofitInstance.getRetrofitInstance()?.getRoomUserDataTemp(randomRoomData)

    suspend fun clearRoomRadius(saveCallDurationRoomData: SaveCallDurationRoomData) =
        RetrofitInstance.getRetrofitInstance()?.clearRadius(saveCallDurationRoomData)

    suspend fun deleteUsersDataFromRoom(saveCallDurationRoomData: SaveCallDurationRoomData) =
        RetrofitInstance.getRetrofitInstance()?.getDeleteUserFpp(saveCallDurationRoomData)

    suspend fun saveDurationOfCall(saveCallDuration: SaveCallDuration) =
        RetrofitInstance.getRetrofitInstance()?.saveCallDuration(saveCallDuration)

}