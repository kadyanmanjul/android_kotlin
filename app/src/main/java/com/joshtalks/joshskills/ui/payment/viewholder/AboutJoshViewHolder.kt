package com.joshtalks.joshskills.ui.payment.viewholder

import android.content.Context
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.course_detail.AboutJosh
import com.joshtalks.joshskills.repository.server.course_detail.RecyclerViewCarouselItemDecorator
import com.joshtalks.joshskills.ui.view_holders.CourseDetailsBaseCell
import com.mindorks.placeholderview.PlaceHolderView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve

@Layout(R.layout.layout_about_josh_view_holder)
class AboutJoshViewHolder(
    override val sequenceNumber: Int,
    private var aboutJoshData: AboutJosh,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.my_josh_recycler_view)
    lateinit var myJoshRecyclerView: PlaceHolderView

    @com.mindorks.placeholderview.annotations.View(R.id.description)
    lateinit var description: TextView

    @com.mindorks.placeholderview.annotations.View(R.id.title)
    lateinit var title: TextView

    @Resolve
    fun onResolved() {
        title.text = aboutJoshData.title
        description.text = aboutJoshData.description
        setCarouselRecyclerView()
        aboutJoshData.details.forEach {
            myJoshRecyclerView.addView(AboutJoshCardView(it))
        }
    }

    private fun setCarouselRecyclerView() {
        val linearLayoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        linearLayoutManager.isSmoothScrollbarEnabled = true

        myJoshRecyclerView.builder
            .setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)

        if (myJoshRecyclerView.itemDecorationCount < 1) {
            val cardWidthPixels = (context.resources.displayMetrics.widthPixels * 0.90f).toInt()
            val cardHintPercent = 0.01f
            myJoshRecyclerView.addItemDecoration(
                RecyclerViewCarouselItemDecorator(
                    context,
                    cardWidthPixels,
                    cardHintPercent
                )
            )
        }
        myJoshRecyclerView.itemAnimator = null
    }
}
