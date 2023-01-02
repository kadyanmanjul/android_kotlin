package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model

import androidx.room.*
import com.joshtalks.joshskills.ui.inbox.payment_verify.Payment

@Entity(tableName = "branch_log_table")
data class BranchLog (
    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "course_name")
    val courseName:String,

    @ColumnInfo(name = "test_id")
    val testId:String,

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "order_id")
    val orderId: String,

    @ColumnInfo(name = "is_sync")
    val isSync :Int = 0
)

@Dao
interface BranchLogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun inertBranchEntry(obj: BranchLog): Long

//    @Query(value = "UPDATE branch_log_table SET is_sync = 1 where order_id= :id ")
//    suspend fun updateBranchStatus(id: String)
//
//    @Query("DELETE from branch_log_table where order_id = :id")
//    suspend fun deleteBranchEntry(id:String) : Int

    @Query(value = "SELECT * FROM branch_log_table where is_sync = 0")
    suspend fun getBranchLogData() : BranchLog?
}