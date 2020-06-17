package com.joshtalks.joshskills.repository.server.help


import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController

class HelpCenterOptions {

    companion object {
        fun getHelpOptionsModelObject(): HelpCenterOptionsModel? {
            val value = AppObjectController.getFirebaseRemoteConfig().getString("help_options")
            return try {
                AppObjectController.gsonMapper.fromJson(
                    value,
                    HelpCenterOptionsModel::class.java
                )
            } catch (ex: Exception) {
                null
            }
        }
    }
}

data class HelpCenterOptionsModel(
    @SerializedName("title") val title: String, // Student help center
    @SerializedName("options") val options: List<Option>,
    @SerializedName("support_message") val supportMessage: String? // Student help center


)

data class Option(
    @SerializedName("action") val action: Action, // CALL
    @SerializedName("id") val id: Int, // 1
    @SerializedName("name") val name: String, // Call HelpLine
    @SerializedName("url") val url: String, // http://
    @SerializedName("action_data") val actionData: String? // Action data

)


enum class Action {
    CALL, HELP_DESK, FAQ, OTHER
}


