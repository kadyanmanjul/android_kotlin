package com.joshtalks.joshskills.repository.local.model.nps


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import kotlinx.android.parcel.Parcelize
import java.lang.reflect.Type

const val ALL_NPS_STATE = "_all_nps_state"
const val CURRENT_NPS_STATE = "_current_nps_state"

@Parcelize
data class NPSEventModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("day")
    val day: Int,

    @SerializedName("enable")
    val enable: Boolean,

    @SerializedName("event")
    val event: NPSEvent,

    @SerializedName("event_name")
    val eventName: String,

    @SerializedName("filter_by")
    val filterBy: NPAFilter
) : Parcelable {

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
                null
            }
        }

        @JvmStatic
        fun setNPSList(npsList: List<NPSEventModel>) {
            PrefManager.put(ALL_NPS_STATE, AppObjectController.gsonMapper.toJson(npsList))
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
    PAYMENT_SUCCESS("Payment Success"),
    PAYMENT_FAILED("Payment Failed"),
    PRACTICE_COMPLETED("Practise completed"),
    FIRST_CLASS("First Class"),
    STANDARD_TIME_EVENT("Standard Time event")
}

@Parcelize
enum class NPAFilter(val type: String) : Parcelable {
    DAY("Day"),
    EVENT("Event")
}

