package com.joshtalks.joshskills.explore.course_details.models


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.common.repository.server.FAQ
import com.joshtalks.joshskills.common.repository.server.FAQCategory
import kotlinx.parcelize.Parcelize

@Parcelize
data class FAQData(
    @SerializedName("title")
    val title: String,

    @SerializedName("category")
    val categoryList: List<FAQCategory>,

    @SerializedName("faqs")
    val faqList: List<FAQ>

) : Parcelable
