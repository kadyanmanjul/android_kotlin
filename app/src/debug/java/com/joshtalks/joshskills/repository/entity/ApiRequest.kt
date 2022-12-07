package com.joshtalks.joshskills.repository.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity
@Parcelize
data class ApiRequest(
    val status: Int,
    val message: String,
    val method: String?,
    val url: String?,
    val request: String?,
    val response: String?,
    val time: Long,
    val duration: Long,
    val headers: String,
    val curl: String
) : Parcelable {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
