package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.GameApiService
import com.joshtalks.joshskills.repository.local.model.Mentor

class QuestionRepo (val api: GameApiService?){
    suspend fun getQuestion(questionRequest: QuestionRequest) =
        api?.getQuestionList(questionRequest)

    suspend fun getSelectAnswer(
        roomId: String,
        questionId: String,
        choiceId: String,
        teamId: String
    ) = api?.getSelectAnswer(
            SelectOption(
                roomId,
                questionId,
                choiceId,
                teamId,
                Mentor.getInstance().getId()
            )
        )

    suspend fun getDisplayCorrectAnswer(roomId: String, questionId: String) =
        api?.getDisplayData(DisplayAnswer(roomId, questionId, Mentor.getInstance().getUserId()))

    suspend fun getRoomDataTemp(randomRoomData: RandomRoomData) =
        api?.getRoomUserDataTemp(randomRoomData)

    suspend fun clearRoomRadius(saveCallDurationRoomData: SaveCallDurationRoomData) =
        api?.clearRadius(saveCallDurationRoomData)

    suspend fun deleteUsersDataFromRoom(saveCallDurationRoomData: SaveCallDurationRoomData) =
        api?.getDeleteUserFpp(saveCallDurationRoomData)

    suspend fun saveDurationOfCall(saveCallDuration: SaveCallDuration) =
        api?.saveCallDuration(saveCallDuration)

}