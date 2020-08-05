package com.joshtalks.joshskills.repository.local.model


import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY

data class RequestRegisterGAId(

    @SerializedName("gaid")
    var gaid: String = EMPTY,

    @SerializedName("install_on")
    var installOn: Long = 0,

    @SerializedName("other_info")
    val otherInfo: Map<String, String> = emptyMap(),

    @SerializedName("test")
    var test: Int = 0,

    @SerializedName("utm_medium")
    var utmMedium: String = EMPTY,

    @SerializedName("utm_source")
    var utmSource: String = EMPTY,

    @Expose(serialize = false, deserialize = true)
    var id: Int = 0,

    @SerializedName("explore_type")
    var exploreType: ExploreType = ExploreType.NORMAL

)

enum class ExploreType(value: String) {
    NORMAL("NORMAL"),
    FFCOURSE("FFCOURSE"),
    FREETRIAL("FREETRIAL"),
    SUBSCRIPTION("SUBSCRIPTION")
}
