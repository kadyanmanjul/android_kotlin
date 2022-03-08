package com.joshtalks.joshskills.ui.special_practice.model
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.joshtalks.joshskills.repository.local.entity.LessonModel

@Dao
interface SpecialDao {
    @Query("SELECT * FROM special_table WHERE special_id=:id ORDER BY special_id DESC")
    fun getSpecial(id: Int): SpecialPractice?

    @Query("SELECT * FROM special_table ORDER BY special_id DESC")
//    @Query("SELECT * FROM special_table WHERE chat_id=:chatId ORDER BY special_id DESC")
//    fun getSpecialPracticeFromChatId(chatId: String): SpecialPractice?
    fun getSpecialPracticeFromChatId(): SpecialPractice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSingleItem(specialPractice: SpecialPractice)
}