package com.joshtalks.joshskills.ui.payment.viewholder

import android.content.Context
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.repository.server.course_detail.AboutJosh
import com.joshtalks.joshskills.ui.view_holders.CourseDetailsBaseCell
import com.mindorks.placeholderview.PlaceHolderView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.vanniktech.emoji.Utils

@Layout(R.layout.layout_about_josh_view_holder)
class AboutJoshViewHolder(
    override val sequenceNumber: Int,
    private var aboutJoshData: AboutJosh,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.my_josh_recycler_view)
    lateinit var my_josh_recycler_view: PlaceHolderView

    @com.mindorks.placeholderview.annotations.View(R.id.description)
    lateinit var description: TextView

    @com.mindorks.placeholderview.annotations.View(R.id.title)
    lateinit var title: TextView

    @Resolve
    fun onResolved() {
        val linearLayoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        my_josh_recycler_view.builder
            .setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
        my_josh_recycler_view.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    context,
                    2f
                )
            )
        )
        title.text = aboutJoshData.title
        description.text = aboutJoshData.description
        my_josh_recycler_view.itemAnimator = null
        aboutJoshData.details.forEach {
            my_josh_recycler_view.addView(AboutJoshCardView(it))
        }
    }
}