package com.joshtalks.joshskills.ui.errorState

import androidx.room.*

@Entity(tableName = "error_screen")
data class ErrorScreen(
    @PrimaryKey
    @ColumnInfo(name = "error_code")
    val errorCode: String,
)

@Dao
interface ErrorScreenDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insetErrorCode(errorScreen: ErrorScreen)


}