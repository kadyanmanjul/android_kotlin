package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.AppObjectController
import java.lang.reflect.Type

data class PractiseFlowOptionModel(
    @SerializedName("header") val header: String,
    @SerializedName("sub_header") val subHeader: String
) {
    companion object {

        @JvmStatic
        fun getPractiseFlowDetails(): List<PractiseFlowOptionModel> {
            val data = AppObjectController.getFirebaseRemoteConfig()
                .getString("practise_flow_steps")
            val typeToken: Type = object : TypeToken<List<PractiseFlowOptionModel>>() {}.type
            return try {
                val obj =
                    AppObjectController.gsonMapperForLocal.fromJson<List<PractiseFlowOptionModel>>(
                        data,
                        typeToken
                    )
                obj
            } catch (ex: Exception) {
                emptyList()
            }

        }

    }
}


