package com.joshtalks.joshskills.common.ui.course_details.viewholder

import android.content.Context
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.common.repository.server.course_detail.AboutJosh
import com.joshtalks.joshskills.common.repository.server.course_detail.CardType
import com.joshtalks.joshskills.common.repository.server.course_detail.RecyclerViewCarouselItemDecorator
import com.mindorks.placeholderview.PlaceHolderView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve


class AboutJoshViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private var aboutJoshData: AboutJosh,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(type, sequenceNumber) {

    
    lateinit var myJoshRecyclerView: PlaceHolderView

    
    lateinit var description: TextView

    
    lateinit var title: TextView

    @Resolve
    fun onResolved() {
        title.text = aboutJoshData.title
        description.text = aboutJoshData.description
        if (myJoshRecyclerView.viewAdapter == null || myJoshRecyclerView.viewAdapter.itemCount == 0) {
            val linearLayoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            linearLayoutManager.isSmoothScrollbarEnabled = true
            myJoshRecyclerView.itemAnimator = null
            myJoshRecyclerView.addItemDecoration(
                LayoutMarginDecoration(
                    Utils.dpToPx(
                        getAppContext(),
                        8f
                    )
                )
            )
            myJoshRecyclerView.builder
                .setHasFixedSize(true)
                .setLayoutManager(linearLayoutManager)
            val cardWidthPixels = (context.resources.displayMetrics.widthPixels * 0.90f).toInt()
            val cardHintPercent = 0.01f
            myJoshRecyclerView.addItemDecoration(
                RecyclerViewCarouselItemDecorator(
                    context,
                    cardWidthPixels,
                    cardHintPercent
                )
            )

            aboutJoshData.details.forEach {
                myJoshRecyclerView.addView(
                    AboutJoshCardView(
                        it
                    )
                )
            }
        }
    }

}
