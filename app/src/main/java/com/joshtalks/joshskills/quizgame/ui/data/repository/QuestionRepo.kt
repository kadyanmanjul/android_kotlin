package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.DisplayAnswer
import com.joshtalks.joshskills.quizgame.ui.data.model.RandomRoomData
import com.joshtalks.joshskills.quizgame.ui.data.model.RoomData
import com.joshtalks.joshskills.quizgame.ui.data.model.SelectOption
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse

class QuestionRepo {
    suspend fun getQuestion() = RetrofitInstanse.api.getQuestionList()
    suspend fun getSelectAnswer(roomId:String,questionId:String,choiceId:String,teamId:String) = RetrofitInstanse.api.getSelectAnswer(SelectOption(roomId,questionId,choiceId,teamId))
    suspend fun getDisplayCorrectAnswer(roomId:String,questionId:String) = RetrofitInstanse.api.getDisplayData(DisplayAnswer(roomId,questionId))

    suspend fun getRoomDataTemp(randomRoomData: RandomRoomData) = RetrofitInstanse.api.getRoomUserDataTemp(randomRoomData)
    suspend fun clearRoomRadius(randomRoomData: RandomRoomData) = RetrofitInstanse.api.clearRadius(randomRoomData)
}