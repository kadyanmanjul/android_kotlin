package com.joshtalks.joshskills.repository.server.reminder

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "reminder_table", indices = [Index(value = ["reminder_id"])])
data class ReminderResponse(
    @PrimaryKey
    @ColumnInfo(name = "reminder_id")
    @SerializedName("id")
    var id: Int,

    @ColumnInfo(name = "mentor_id")
    @SerializedName("mentor")
    var mentor: String,

    @ColumnInfo(name = "reminder_frequency")
    @SerializedName("reminder_frequency")
    var reminderFrequency: String,

    @ColumnInfo(name = "status")
    @SerializedName("status")
    var status: String,

    @ColumnInfo(name = "reminder_time")
    @SerializedName("reminder_time")
    var reminderTime: String,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    var createdAt: String,

    @ColumnInfo(name = "modified_at")
    @SerializedName("modified_at")
    var modifiedAt: String
)