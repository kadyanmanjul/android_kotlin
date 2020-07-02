package com.joshtalks.joshskills.ui.course_details.viewholder

import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentManager
import com.google.android.material.tabs.TabLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.HeightWrappingViewPager
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.joshtalks.joshskills.repository.server.course_detail.Guidelines
import com.joshtalks.joshskills.ui.course_details.extra.GuidelineAdapter
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator


@Layout(R.layout.guideline_view_holder)
class GuidelineViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private val guidelines: Guidelines,
    private val fragmentManager: FragmentManager
) : CourseDetailsBaseCell(type, sequenceNumber) {

    @View(R.id.header)
    lateinit var headerTV: AppCompatTextView

    @View(R.id.tab_layout)
    lateinit var tabLayout: TabLayout

    @View(R.id.view_pager)
    lateinit var viewPager: HeightWrappingViewPager

    @View(R.id.worm_dots_indicator)
    lateinit var wormDotsIndicator: WormDotsIndicator


    @Resolve
    fun onResolved() {
        headerTV.text = guidelines.title
        viewPager.adapter =
            GuidelineAdapter(fragmentManager, guidelines.guidelines.sortedBy { it.category })
        wormDotsIndicator.setViewPager(viewPager)
        tabLayout.setupWithViewPager(viewPager)
        viewPager.measure(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
}
