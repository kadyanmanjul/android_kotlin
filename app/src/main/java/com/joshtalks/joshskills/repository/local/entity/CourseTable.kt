package com.joshtalks.joshskills.repository.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*

@Entity(tableName = "course")
data class Course(
    @PrimaryKey()
    @SerializedName("id") val courseId: String,

    @ColumnInfo()
    @SerializedName("duration") val duration: Int,

    @ColumnInfo()
    @SerializedName("is_deleted") val is_deleted: Boolean,

    @ColumnInfo(name = "course_name")
    @SerializedName("name")
    val courseName: String,

    @ColumnInfo(name = "teacher_id")
    @SerializedName("teacher") val teacherId: String,

    @ColumnInfo(name = "conversation_id")
    @SerializedName("conversation_id") val conversationId: String?,

    @ColumnInfo(name = "course_icon")
    @SerializedName("icon") val courseIcon: String?,

    @ColumnInfo(name = "course_created_date")
    @SerializedName("created") val courseCreatedDate: Date?,

    @ColumnInfo(name = "chat_type")
    @SerializedName("chat_type") val chatType: String?,

    @ColumnInfo(name = "report_status")
    @SerializedName("report_status") val reportStatus: Boolean = false,

    @ColumnInfo(name = "batch_started")
    @SerializedName("batch_started") val batchStarted: String,

    @ColumnInfo(name = "voicecall_status")
    @SerializedName("voicecall_status") val voiceCallStatus: Boolean = false,

    @ColumnInfo(name = "is_group_active")
    @SerializedName("is_group_active") val isGroupActive: Boolean = false,

    @ColumnInfo(name = "is_points_active")
    @SerializedName("is_points_active") val isPointsActive: Boolean = false,

    @ColumnInfo(name = "is_course_locked")
    @SerializedName("is_course_locked") val isCourseLocked: Boolean = false,

    @ColumnInfo(name = "is_course_bought")
    @SerializedName("is_course_bought") val isCourseBought: Boolean = false,

    @ColumnInfo(name = "expire_date")
    @SerializedName("expire_date") val expiryDate: Date? = null,

    @ColumnInfo(name="paid_test_id")
    @SerializedName("paid_test_id") val paidTestId: String? = null,

    @ColumnInfo(name = "is_extend_ft_applicable")
    @SerializedName("is_extend_ft_applicable") val isFreeTrialExtended: Boolean = false,

    @ColumnInfo(name = "form_submitted")
    @SerializedName("form_submitted") val formSubmitted: Boolean = false

) : Serializable