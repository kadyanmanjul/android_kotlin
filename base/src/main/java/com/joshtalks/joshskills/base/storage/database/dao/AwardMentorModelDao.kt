package com.joshtalks.joshskills.base.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AwardMentorModelDao {

    @Query("SELECT * FROM awardmentormodel WHERE id=:id")
    fun getAwardMentorModel(id: Int): AwardMentorModel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSingleItem(awardMentorModel: AwardMentorModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(awardMentorModelList: List<AwardMentorModel>)

}