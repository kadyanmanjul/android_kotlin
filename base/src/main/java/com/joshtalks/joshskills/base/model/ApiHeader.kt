package com.joshtalks.joshskills.base.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ApiHeader(val token : String,
                     val versionName : String,
                     val versionCode : String,
                     val userAgent : String,
                     val acceptLanguage : String) : Parcelable {
                         companion object {
                             fun empty() = ApiHeader("","","","","")
                         }
                     }


data class NotificationData(val title : String) {
    companion object {
        fun default() : NotificationData {
            return NotificationData(
                title = "User, Start your English Practice now."
            )
        }
    }
}