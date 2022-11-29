package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class SingleImageData(

    @SerializedName("imgUrl")
    val imgUrl: String

) : Parcelable
