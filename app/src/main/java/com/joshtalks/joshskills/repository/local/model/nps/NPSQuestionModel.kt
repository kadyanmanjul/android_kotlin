package com.joshtalks.joshskills.repository.local.model.nps


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.AppObjectController
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NPSQuestionModel(
    @SerializedName("end_rating")
    val endRating: Int,
    @SerializedName("event_name")
    val eventName: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("question")
    val question: String,
    @SerializedName("start_rating")
    val startRating: Int,
    @SerializedName("sub_question")
    val subQuestion: String
) : Parcelable {

    companion object {
        fun getNPSQuestionModelList(string: String): List<NPSQuestionModel>? {
            return AppObjectController.gsonMapperForLocal.fromJson<List<NPSQuestionModel>>(
                string, object : TypeToken<List<NPSQuestionModel>>() {}.type
            )

        }
    }
}