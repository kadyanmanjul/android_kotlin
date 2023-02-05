package com.joshtalks.joshskills.premium.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.premium.repository.server.FAQ
import com.joshtalks.joshskills.premium.repository.server.FAQCategory
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FAQData(
    @SerializedName("title")
    val title: String,

    @SerializedName("category")
    val categoryList: List<FAQCategory>,

    @SerializedName("faqs")
    val faqList: List<FAQ>

) : Parcelable
