package com.joshtalks.joshskills.repository.local.entity.practise

import android.os.Parcelable
import androidx.room.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import kotlinx.android.parcel.Parcelize
import java.util.*

@Entity(tableName = "favorite_caller")
@Parcelize
data class FavoriteCaller(
    @PrimaryKey()
    @SerializedName("id") val id: Int,

    @ColumnInfo(name = "name")
    @SerializedName("name")
    val name: String,

    @ColumnInfo(name = "photo_url")
    @SerializedName("photo_url")
    val image: String?,

    @ColumnInfo(name = "minutes_spoken")
    @SerializedName("minutes_spoken")
    val minutesSpoken: Int = 0,

    @ColumnInfo(name = "total_calls")
    @SerializedName("total_calls")
    val totalCalls: Int = 0,

    @ColumnInfo(name = "last_called_at")
    @SerializedName("last_called_at")
    val lastCalledAt: Date = Date(),

    @Expose
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "mentor_id")
    @SerializedName("mentor_id")
    val mentorId: String = EMPTY,

) : Parcelable {
    @Ignore
    @Expose
    var selected: Boolean = false
}

@Dao
interface FavoriteCallerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteCaller(favoriteCaller: FavoriteCaller)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteCallers(favoriteCallerList: List<FavoriteCaller>): List<Long>

    @Query("UPDATE favorite_caller SET is_deleted =1  WHERE id IN (:ids)")
    suspend fun updateFavoriteCallerStatus(ids: List<Int>)

    @Query("DELETE FROM  favorite_caller WHERE id IN (:ids)")
    suspend fun removeFromFavorite(ids: List<Int>)

    @Query(value = "SELECT * from favorite_caller WHERE is_deleted=0 ")
    fun getFavoriteCallers(): List<FavoriteCaller>

    @Query(value = "SELECT * from favorite_caller  WHERE id=:id")
    fun getFavoriteCaller(id: Int): FavoriteCaller?

    @Query(value = "SELECT id from favorite_caller WHERE is_deleted=1 ")
    fun getRemoveFromFavoriteCallers(): List<Int>

    @Query(value = "SELECT COUNT(id) FROM favorite_caller  ")
    suspend fun getCountOfFavoriteCaller(): Long

    @Query("DELETE FROM  favorite_caller")
    suspend fun removeAllFavorite()
}
