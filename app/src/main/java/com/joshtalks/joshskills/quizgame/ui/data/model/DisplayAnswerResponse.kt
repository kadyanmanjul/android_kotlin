package com.joshtalks.joshskills.quizgame.ui.data.model

import com.google.gson.annotations.SerializedName

//data class DataAnswer(
//    @SerializedName("choice_id") var choiceId:String,
//    @SerializedName("is_correct") var isCorrect:String,
//    @SerializedName("marked_by_team") var markedByTeam : String)

data class DisplayAnswerResponse (
    @SerializedName("message")var message:String,
    @SerializedName("correct_choice_value")var correctChoiceValue:String,
    //@SerializedName("data") var answerData:List<DataAnswer>,
    @SerializedName("correct_choice_no") var correctChoiceNumber:String,
    @SerializedName("correct_choice_id") var correctChoiceId:String)