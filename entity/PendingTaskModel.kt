package com.joshtalks.joshskills.base.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.joshtalks.joshskills.repository.server.RequestEngage

@Entity(tableName = "pending_task_table")
data class PendingTaskModel(

    @ColumnInfo(name = "request_object")
    var requestObject: RequestEngage,
    @ColumnInfo(name = "type")
    var type: PendingTask,
    @ColumnInfo(name = "retry_count")
    var numberOfRetries: Int = 0


) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0
}

enum class PendingTask {
    VOCABULARY_PRACTICE,
    READING_PRACTICE_NEW,
    READING_PRACTICE_OLD,

}