package com.joshtalks.joshskills.repository.local.minimalentity

import android.os.Parcelable
import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_DELIVER_STATUS
import com.joshtalks.joshskills.repository.local.entity.User
import java.util.Date
import kotlinx.android.parcel.Parcelize

@Parcelize
data class InboxEntity constructor(
    val course_name: String = "",
    val conversation_id: String = "",
    val course_icon: String? = "",
    val courseId: String = "",
    val duration: Int = 0,
    val is_deleted: Boolean? = false,
    val teacher_id: String? = "",
    var chat_id: String? = "",
    @ColumnInfo(name = "created")
    var created: Long? = 0,
    @ColumnInfo(name = "course_created_date")
    var courseCreatedDate: Date? = null,
    var isSeen: Boolean? = false,
    var text: String? = "",
    var type: BASE_MESSAGE_TYPE? = BASE_MESSAGE_TYPE.TX,
    var url: String? = "",
    var localUrlPath: String? = "",
    var title: String? = "",
    var question_type: String? = "",
    var chat_type: String? = null,
    var qText: String? = "",
    var user: User? = null,
    var material_type: BASE_MESSAGE_TYPE?,
    var message_deliver_status: MESSAGE_DELIVER_STATUS? = MESSAGE_DELIVER_STATUS.READ,
    var report_status: Boolean,
    @ColumnInfo(name = "batch_started")
    val batchStarted: String?,
    @ColumnInfo(name = "voicecall_status")
    val voiceCallStatus: Boolean = false,
    @ColumnInfo(name = "is_group_active")
    val isGroupActive: Boolean = false,
    @ColumnInfo(name = "is_points_active")
    val isCapsuleCourse: Boolean = false,
    @ColumnInfo(name = "lesson_no")
    val lessonNo: Int? = 0,
    @ColumnInfo(name = "is_course_locked")
    @SerializedName("is_course_locked") val isCourseLocked: Boolean = false,
    @ColumnInfo(name = "is_course_bought")
    @SerializedName("is_course_bought") val isCourseBought: Boolean = false,
    @ColumnInfo(name = "expire_date")
    @SerializedName("expire_date") val expiryDate: Date? = null,
    @ColumnInfo(name="paid_test_id")
    @SerializedName("paid_test_id") val paidTestId: String? = null,
    @ColumnInfo(name = "speaking_status")
    @SerializedName("speaking_status") val speakingStatus: String = " "

) : Parcelable {
    override fun hashCode(): Int {
        return conversation_id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other?.javaClass != javaClass) return false
        if (conversation_id == (other as InboxEntity).conversation_id) {
            return true
        }
        return false
    }
}
