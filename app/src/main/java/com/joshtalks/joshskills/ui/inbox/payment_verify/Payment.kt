package com.joshtalks.joshskills.ui.inbox.payment_verify

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverters
import androidx.room.Update
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.local.PaymentStatusConverters

@Entity(tableName = "payment_table")
data class Payment(
    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "joshtalks_order_id")
    val joshtalksOrderId: Int,

    @ColumnInfo(name = "razorpay_key_id")
    val razorpayKeyId: String,


    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "razorpay_order_id")
    val razorpayOrderId: String,

    @ColumnInfo(name = "status")
    @TypeConverters(
        PaymentStatusConverters::class
    )
    var status: PaymentStatus? = PaymentStatus.CREATED,

    @ColumnInfo(name = "time_stamp")
    val timeStamp :Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_sync")
    val isSync :Boolean = false,

    @ColumnInfo(name = "is_deleted")
    val isdeleted :Boolean = false,
    @ColumnInfo(name = "response")
    var response :String = EMPTY
)

@Dao
interface PaymentDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun inertPaymentEntry(obj: Payment): Long

    @Query("DELETE FROM payment_table")
    suspend fun deleteAllPaymentEntry()

    @Query("UPDATE payment_table SET is_deleted = 1 where razorpay_order_id = :id")
    suspend fun deletePaymentEntry(id:String) : Int

    @Query(value = "SELECT * FROM payment_table WHERE is_deleted = 0")
    suspend fun getAllPaymentEntry() : List<Payment>

    @Query(value = "UPDATE payment_table SET status = :status , is_sync = 1 where razorpay_order_id= :id ")
    suspend fun updatePaymentStatus(id: String, status: PaymentStatus)

    @Update
    suspend fun updatePayment(last: Payment)
}