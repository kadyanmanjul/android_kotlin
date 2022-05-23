package com.joshtalks.joshskills.repository.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "broadcast_events")
data class BroadCastEvent(

    @SerializedName("mentor_id")
    val mentorId: String,
    @SerializedName("event_name")
    val eventName: String?
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}