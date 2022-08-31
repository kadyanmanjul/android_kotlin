package com.joshtalks.joshskills.repository.server.voip


import android.os.Parcelable
import androidx.room.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Entity
@Parcelize
data class SpeakingTopic(

    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: Int,

    @ColumnInfo(name = "topic_name")
    @SerializedName("name")
    val topicName: String,

    @ColumnInfo(name = "duration")
    @SerializedName("duration")
    val duration: Int,

    @ColumnInfo(name = "already_talked")
    @SerializedName("already_talked")
    val alreadyTalked: Int,

    @ColumnInfo(name = "total_new_student_calls")
    @SerializedName("total_new_student_calls")
    val totalNewStudentCalls: Int = 0,

    @ColumnInfo(name = "required_new_student_calls")
    @SerializedName("required_new_student_calls")
    val requiredNewStudentCalls: Int = 7,

    @ColumnInfo(name = "is_new_student_calls_activated")
    @SerializedName("call_new_student")
    val isNewStudentCallsActivated: Boolean = false,

    @ColumnInfo(name = "call_duration_status")
    @SerializedName("call_duration_status")
    val callDurationStatus: String = "NFT",

    @IgnoredOnParcel
    @Expose
    var isFromDb: Boolean = false,

    @SerializedName("is_ft_caller_blocked")
    @ColumnInfo(name = "is_ft_caller_blocked")
    val isFtCallerBlocked: String?,

    @SerializedName("call_target_type")
    @ColumnInfo(name = "call_target_type")
    val type: String,

    @SerializedName("last_call")
    @ColumnInfo(name = "last_call")
    val lastCall : Int

) : Parcelable

@Dao
interface SpeakingTopicDao {

    @Query(value = "SELECT * FROM SpeakingTopic where id=:topicId")
    suspend fun getTopicById(topicId: String): SpeakingTopic?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTopic(topic: SpeakingTopic)

}
