package com.joshtalks.joshskills.base.model.groups

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

class GroupRequest(
    @field:SerializedName("mentor_id")
    val mentorId: String,

    @PrimaryKey
    @field:SerializedName("group_id")
    val groupId: String,

    @field:SerializedName("allow")
    val allow: Boolean = true,

    @field:SerializedName("type")
    val groupType: String = OPENED_GROUP
)

@Entity(tableName = "time_token_db")
class TimeTokenRequest(
    @field:SerializedName("mentor_id")
    val mentorId: String,

    @PrimaryKey
    @field:SerializedName("group_id")
    val groupId: String,

    @field:SerializedName("time_token")
    val timeToken: Long
)
