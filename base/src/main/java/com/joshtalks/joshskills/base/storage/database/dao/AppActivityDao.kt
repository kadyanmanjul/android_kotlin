package com.joshtalks.joshskills.base.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface AppActivityDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIntoAppActivity(obj: AppActivityModel)
}