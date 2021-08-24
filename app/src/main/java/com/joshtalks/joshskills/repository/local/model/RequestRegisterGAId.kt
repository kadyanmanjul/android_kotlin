package com.joshtalks.joshskills.repository.local.model


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
    var test: Int? = null,

    @SerializedName("utm_medium")
    var utmMedium: String? = EMPTY,

    @SerializedName("utm_source")
    var utmSource: String? = EMPTY,

    @SerializedName("explore_type")
    var exploreCardType: ExploreCardType? = ExploreCardType.NORMAL

)

enum class ExploreCardType(value: String) {

    @SerializedName("NORMAL")
    NORMAL("NORMAL"),

    @SerializedName("FFCOURSE")
    FFCOURSE("FFCOURSE"),

    @SerializedName("FREETRIAL")
    FREETRIAL("FREETRIAL"),

}
