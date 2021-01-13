package com.joshtalks.joshskills.repository.local.minimalentity

import android.os.Parcelable
import androidx.room.ColumnInfo
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_DELIVER_STATUS
import com.joshtalks.joshskills.repository.local.entity.User
import kotlinx.android.parcel.Parcelize
import java.util.Date

@Parcelize
data class InboxEntity constructor(
    val course_name: String = "",
    val conversation_id: String = "",
    val course_icon: String? = "",
    val courseId: String = "",
    val duration: Int? = 0,
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
    val isPointsActive: Boolean = false

    ) : Parcelable
