package com.joshtalks.joshskills.ui.errorState

import androidx.room.*
import com.google.gson.annotations.SerializedName

@Entity(tableName = "error_screen")
data class ErrorScreen(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id:Int = 0,
    @ColumnInfo(name = "error_code")
    @SerializedName("api_code")
    val apiErrorCode: String,

    @ColumnInfo(name = "payload")
    @SerializedName("payload")
    val payload:String,

    @ColumnInfo(name = "exception")
    @SerializedName("exception")
    val exception:String
)

@Dao
interface ErrorScreenDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insetErrorCode(errorScreen: ErrorScreen)


}