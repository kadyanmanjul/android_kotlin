package com.joshtalks.joshskills.ui.group.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

open class GroupRequest(
    @field:SerializedName("mentor_id")
    val mentorId: String,

    @PrimaryKey
    @field:SerializedName("group_id")
    val groupId: String,
)

@Entity(tableName = "time_token_db")
class TimeTokenRequest(
    mentorId: String,
    groupId: String,

    @field:SerializedName("time_token")
    val timeToken: Long,
) : GroupRequest(mentorId, groupId)
