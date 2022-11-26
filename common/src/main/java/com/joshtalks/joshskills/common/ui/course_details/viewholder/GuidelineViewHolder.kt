package com.joshtalks.joshskills.common.ui.course_details.viewholder

import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentManager
import com.google.android.material.tabs.TabLayout
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.custom_ui.HeightWrappingViewPager
import com.joshtalks.joshskills.common.repository.server.course_detail.CardType
import com.joshtalks.joshskills.common.repository.server.course_detail.Guidelines
import com.joshtalks.joshskills.common.ui.course_details.extra.GuidelineAdapter
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator



class GuidelineViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private val guidelines: Guidelines,
    private val fragmentManager: FragmentManager
) : CourseDetailsBaseCell(type, sequenceNumber) {


    lateinit var headerTV: AppCompatTextView


    lateinit var tabLayout: TabLayout


    lateinit var viewPager: HeightWrappingViewPager


    lateinit var wormDotsIndicator: WormDotsIndicator


    @Resolve
    fun onResolved() {
        headerTV.text = guidelines.title
        viewPager.adapter =
            GuidelineAdapter(fragmentManager, guidelines.guidelines.sortedBy { it.sortOrder })
        wormDotsIndicator.setViewPager(viewPager)
        tabLayout.setupWithViewPager(viewPager)
        viewPager.measure(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
}
