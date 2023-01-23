package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BuyCourseFeatureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuyCourseFeatureData(readingVideo: BuyCourseFeatureModelNew?)

    @Query(value = "SELECT * FROM buy_course_feature")
    suspend fun getBuyCourseFeatureData() : BuyCourseFeatureModelNew?

}