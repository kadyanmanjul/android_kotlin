package com.joshtalks.joshskills.repository.local.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Entity
@Parcelize
data class AwardMentorModel(

    @PrimaryKey
    @SerializedName("id")
    @Expose
    @ColumnInfo(name = "id")
    var id: Int,

    @ColumnInfo(name = "award_image_url")
    @SerializedName("award_image_url")
    @Expose
    var awardImageUrl: String?,

    @ColumnInfo(name = "award_text")
    @SerializedName("award_text")
    @Expose
    var awardText: String?,

    @ColumnInfo(name = "description")
    @SerializedName("description")
    @Expose
    val description: String?,

    @ColumnInfo(name = "performer_name")
    @SerializedName("performer_name")
    @Expose
    var performerName: String?,

    @ColumnInfo(name = "performer_photo_url")
    @SerializedName("performer_photo_url")
    @Expose
    var performerPhotoUrl: String?,

    @ColumnInfo(name = "total_points_text")
    @SerializedName("total_points_text")
    @Expose
    var totalPointsText: String?
) : Parcelable

@Dao
interface AwardMentorModelDao {

    @Query("SELECT * FROM awardmentormodel WHERE id=:id")
    fun getAwardMentorModel(id: Int): AwardMentorModel?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSingleItem(awardMentorModel: AwardMentorModel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(awardMentorModelList: List<AwardMentorModel>)

}