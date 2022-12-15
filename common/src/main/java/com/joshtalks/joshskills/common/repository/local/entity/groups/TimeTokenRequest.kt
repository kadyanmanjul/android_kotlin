package com.joshtalks.joshskills.common.repository.local.entity.groups

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

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
