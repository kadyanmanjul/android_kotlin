package com.joshtalks.joshskills.repository.server.certification_exam


import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.RESUME_CERTIFICATION_EXAM
import java.lang.reflect.Type
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CertificationQuestionModel(
    @SerializedName("id")
    val id: Int = -1,
    @SerializedName("total_marks")
    val totalMarks: Int = 0,
    @SerializedName("total_min")
    val totalMinutes: Int = 0,
    @SerializedName("total_question")
    val totalQuestion: Int = 0,
    @SerializedName("attempted_count")
    val attemptCount: Int = 0,
    @SerializedName("instructions")
    val instruction: List<String>? = emptyList(),
    @SerializedName("type")
    val type: String? = EMPTY,
    @SerializedName("questions")
    val questions: List<CertificationQuestion> = emptyList(),
    var timerTime: Long? = null,
    var certificateExamId: Int = -1,
    var lastQuestionOfExit: Int = -1,
) : Parcelable {
    constructor() : this(
        id = -1,
        totalMarks = 0,
        totalMinutes = 0,
        totalQuestion = 0,
        attemptCount = 0,
        instruction = emptyList(),
        type = EMPTY,
        questions = emptyList(),
        timerTime = 0,
        certificateExamId = -1,
        lastQuestionOfExit = -1
    )

    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }

    fun saveForLaterUse() {
        val string: String = toString()
        PrefManager.put(getKey(certificateExamId), string)
    }

    companion object {
        fun getResumeExam(certificateExamId: Int): CertificationQuestionModel? {
            return try {
                val gsonBuilder = GsonBuilder()
                gsonBuilder.registerTypeAdapter(
                    CertificationQuestionModel::class.java,
                    CertificationQuestionInstanceCreator()
                )
                val customGson: Gson = gsonBuilder.create()
                val matType = object : TypeToken<CertificationQuestionModel>() {}.type
                customGson.fromJson<CertificationQuestionModel>(
                    PrefManager.getStringValue(getKey(certificateExamId)),
                    matType
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }

        fun removeResumeExam(certificateExamId: Int) {
            PrefManager.removeKey(getKey(certificateExamId))
        }

        fun getKey(certificateExamId: Int): String {
            return RESUME_CERTIFICATION_EXAM + certificateExamId
        }
    }
}

@Parcelize
data class CertificationQuestion(
    @SerializedName("id")
    val questionId: Int = -1,
    @SerializedName("sort_order")
    val sortOrder: Int = -1,
    @SerializedName("text")
    val questionText: String = EMPTY,
    @SerializedName("explanation")
    val explanation: String? = null,
    @SerializedName("answers")
    val answers: List<Answer> = emptyList(),
    @SerializedName("user_submitted_answer")
    val userSubmittedAnswer: List<UserSubmittedAnswer>? = null,
    var userSelectedOption: Int? = null,
    var isAttempted: Boolean = false,
    var isBookmarked: Boolean = false,
    var isViewed: Boolean = false,
) : Parcelable {
    constructor() : this(
        questionId = -1,
        sortOrder = -1,
        questionText = EMPTY,
        explanation = EMPTY,
        answers = emptyList(),
        userSubmittedAnswer = emptyList(),
        userSelectedOption = null,
        isAttempted = false,
        isBookmarked = false,
        isViewed = false
    )
}

@Parcelize
data class Answer(
    @SerializedName("id")
    val id: Int = -1,
    @SerializedName("is_correct")
    val isCorrect: Boolean = false,
    @SerializedName("sort_order")
    val sortOrder: Int = -1,
    @SerializedName("text")
    val text: String = EMPTY
) : Parcelable {
    constructor() : this(
        id = -1,
        isCorrect = false,
        sortOrder = 0,
        text = EMPTY
    )
}

@Parcelize
data class UserSubmittedAnswer(
    @SerializedName("answer_id")
    val answerId: Int = -1,
    @SerializedName("attempt_seq")
    val attemptSeq: Int = -1
) : Parcelable {
    constructor() : this(
        answerId = -1,
        attemptSeq = -1
    )
}


enum class CertificationExamView {
    EXAM_VIEW, RESULT_VIEW

}

private class CertificationQuestionInstanceCreator : InstanceCreator<CertificationQuestionModel> {
    override fun createInstance(type: Type): CertificationQuestionModel {
        return CertificationQuestionModel()
    }
}
