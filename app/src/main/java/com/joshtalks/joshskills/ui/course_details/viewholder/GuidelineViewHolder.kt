package com.joshtalks.joshskills.ui.course_details.viewholder

import android.app.Activity
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity
import com.google.gson.JsonObject
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.GuidelineViewHolderBinding
import com.joshtalks.joshskills.repository.server.course_detail.Guidelines
import com.joshtalks.joshskills.ui.course_details.extra.GuidelineAdapter

class GuidelineViewHolder(
    val item: GuidelineViewHolderBinding,
    val activity: Activity
) : DetailsBaseViewHolder(item) {

    override fun bindData(sequence: Int, cardData: JsonObject) {
        val data = AppObjectController.gsonMapperForLocal.fromJson(
            cardData.toString(),
            Guidelines::class.java
        )
        item.header.text = data.title
        val fragmentManager = (activity as FragmentActivity).supportFragmentManager
        item.viewPager.adapter = GuidelineAdapter(fragmentManager, data.guidelines.sortedBy { it.sortOrder })
        item.wormDotsIndicator.setViewPager(item.viewPager)
        item.tabLayout.setupWithViewPager(item.viewPager)
        item.viewPager.measure(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
}
