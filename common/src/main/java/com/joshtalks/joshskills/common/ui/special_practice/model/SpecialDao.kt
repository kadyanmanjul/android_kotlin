package com.joshtalks.joshskills.common.ui.special_practice.model

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface SpecialDao {

    @Query("SELECT * FROM special_table WHERE chat_id = :chatId")
    fun getSpecialPractice(chatId: String): SpecialPractice?

    @Query("SELECT * FROM special_table WHERE special_id = :specialId")
    fun getSpecialPracticeFromId(specialId: String): SpecialPractice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSingleItem(specialPractice: SpecialPractice)

    @Query("UPDATE special_table SET recorded_video = :recordedVideo where special_id == :specialId")
    fun updateRecordedTable(specialId: String, recordedVideo: String)
}