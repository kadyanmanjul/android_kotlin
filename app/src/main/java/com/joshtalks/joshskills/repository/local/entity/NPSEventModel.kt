package com.joshtalks.joshskills.repository.local.entity


import android.os.Parcelable
import androidx.room.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.dateStartOfDay
import kotlinx.android.parcel.Parcelize
import java.lang.reflect.Type
import java.util.*

const val ALL_NPS_STATE = "_all_nps_state"
const val CURRENT_NPS_STATE = "_current_nps_state"

@Parcelize
@Entity(
    tableName = "nps_event_table",
    primaryKeys = ["day", "event_id", "event_name"]
)
data class NPSEventModel(
    @ColumnInfo
    @SerializedName("id")
    var id: Int = -1,

    @ColumnInfo
    @SerializedName("day")
    var day: Int = -1,

    @ColumnInfo
    @SerializedName("enable")
    var enable: Boolean = false,

    @ColumnInfo
    @SerializedName("event")
    var event: NPSEvent = NPSEvent.STANDARD_TIME_EVENT,

    @ColumnInfo(name = "event_name")
    @SerializedName("event_name")
    var eventName: String = "",


    @ColumnInfo(name = "event_id")
    @Expose var eventId: String = "",

    @ColumnInfo(name = "created_at")
    @Expose
    var createdAt: Date = Date()

) : Parcelable {
    constructor() : this(
        id = -1,
        eventId = "",
        day = -1,
        enable = false,
        event = NPSEvent.STANDARD_TIME_EVENT,
        eventName = "",
        createdAt = Date()
    )


    companion object {

        val NPS_EVENT_MODEL_TYPE_TOKEN: Type = object : TypeToken<List<NPSEventModel>>() {}.type

        @JvmStatic
        fun getAllNpaList(): List<NPSEventModel>? {
            return try {
                AppObjectController.gsonMapperForLocal.fromJson<List<NPSEventModel>>(
                    PrefManager.getStringValue(ALL_NPS_STATE),
                    NPS_EVENT_MODEL_TYPE_TOKEN
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }

        @JvmStatic
        fun setNPSList(nps: String) {
            PrefManager.put(ALL_NPS_STATE, nps)
        }

        @JvmStatic
        fun getCurrentNPA(): NPSEvent? {
            return try {
                AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(CURRENT_NPS_STATE), NPSEvent::class.java
                )
            } catch (ex: Exception) {
                null
            }
        }

        @JvmStatic
        fun setCurrentNPA(npaEvent: NPSEvent?) {
            PrefManager.put(CURRENT_NPS_STATE, AppObjectController.gsonMapper.toJson(npaEvent))
        }

        @JvmStatic
        fun removeCurrentNPA() {
            PrefManager.removeKey(CURRENT_NPS_STATE)
        }
    }

}

@Parcelize
enum class NPSEvent(val type: String) : Parcelable {
    STANDARD_TIME_EVENT("Standard Time event"),
    FIRST_CLASS("First Class"),
    PAYMENT_SUCCESS("Payment Success"),
    PAYMENT_FAILED("Payment Failed"),
    PRACTICE_COMPLETED("Practice completed"),
    WATCH_VIDEO("WATCH_VIDEO")
}

@Dao
interface NPSEventModelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNPSEvent(npsEventModel: NPSEventModel)

    @Query(value = "SELECT count() FROM nps_event_table where event_name= :eventName AND day= :day AND event_id= :eventId ")
    suspend fun isEventExist(
        eventName: String,
        day: Int,
        eventId: String = EMPTY
    ): Long

    @Query(value = "SELECT COUNT() FROM nps_event_table where created_at >= :startDate AND created_at <= :endDate")
    suspend fun getTotalRecords(startDate: Date, endDate: Date): Long


    @Transaction
    suspend fun getTotalCountOfRows(): Long {
        val startDate = dateStartOfDay()
        val endDate = Date()
        return getTotalRecords(startDate, endDate)
    }
}


