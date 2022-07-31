package com.joshtalks.joshskills.repository.local.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
@Entity(tableName = "feedback_engage")
data class FeedbackEngageModel(
    @PrimaryKey()
    @Expose
    var id: String,
    @ColumnInfo(name = "created_at")
    @Expose
    var createdAt: Date = Date()

) : Parcelable