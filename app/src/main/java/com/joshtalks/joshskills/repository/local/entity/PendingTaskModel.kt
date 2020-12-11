package com.joshtalks.joshskills.repository.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.joshtalks.joshskills.repository.server.RequestEngage

@Entity(tableName = "pending_task_table")
data class PendingTaskModel(
    @PrimaryKey
    @ColumnInfo(name = "id")
    var id: Int,
    @ColumnInfo(name = "request_object")
    var requestObject: RequestEngage,
    @ColumnInfo(name = "type")
    var type: PendingTask

)

enum class PendingTask {
    VOCABULARY_PRACTICE,
    READING_PRACTICE
}