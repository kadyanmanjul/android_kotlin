package com.joshtalks.joshskills.repository.server.voip


import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Entity
@Parcelize
data class SpeakingTopic(

    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: Int,

    @ColumnInfo(name = "topic_name")
    @SerializedName("name")
    val topicName: String,

    @ColumnInfo(name = "duration")
    @SerializedName("duration")
    val duration: Int,

    @ColumnInfo(name = "already_talked")
    @SerializedName("already_talked")
    val alreadyTalked: Int

) : Parcelable

@Dao
interface SpeakingTopicDao {

    @Query(value = "SELECT * FROM SpeakingTopic  where id=:topicId")
    suspend fun getTopicById(topicId: String): SpeakingTopic?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTopic(topic: SpeakingTopic)

}
