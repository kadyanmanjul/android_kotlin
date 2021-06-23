package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.SerializedName

data class ShortNotificationObject(
    @SerializedName("id")
    var id: String? = null,

    @SerializedName("nType")
    var nType: ShortNotificationAction? = null       // Notification Type
)

enum class ShortNotificationAction(val value: String) {
    @SerializedName("CR")
    INCOMING_CALL_NOTIFICATION("CR"),

    @SerializedName("CD")
    CALL_DISCONNECT_NOTIFICATION("CD"),

    @SerializedName("CFR")
    CALL_FORCE_CONNECT_NOTIFICATION("CFR"),

    @SerializedName("CFD")
    CALL_FORCE_DISCONNECT_NOTIFICATION("CFD"),

    @SerializedName("NUF")
    CALL_NO_USER_FOUND_NOTIFICATION("NUF"),

    @SerializedName("COH")
    CALL_ON_HOLD_NOTIFICATION("COH"),

    @SerializedName("CRS")
    CALL_RESUME_NOTIFICATION("CRS"),

    @SerializedName("CC")
    CALL_CONNECTED_NOTIFICATION("CC"),
}
