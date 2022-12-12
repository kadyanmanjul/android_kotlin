package com.joshtalks.joshskills.ui.course_details.viewholder

import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.databinding.LayoutAboutJoshViewHolderBinding
import com.joshtalks.joshskills.repository.server.course_detail.AboutJosh
import com.joshtalks.joshskills.repository.server.course_detail.RecyclerViewCarouselItemDecorator
import com.joshtalks.joshskills.ui.course_details.extra.AboutJoshAdapter

class AboutJoshViewHolder(
    val item: LayoutAboutJoshViewHolderBinding,
) : DetailsBaseViewHolder(item) {

    override fun bindData(sequence: Int, cardData: JsonObject) {
        val data = AppObjectController.gsonMapperForLocal.fromJson(
            cardData.toString(),
            AboutJosh::class.java
        )
        item.title.text = data.title
        item.description.text = data.description

        val linearLayoutManager =
            LinearLayoutManager(getAppContext(), LinearLayoutManager.HORIZONTAL, false)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        item.myJoshRecyclerView.itemAnimator = null
        item.myJoshRecyclerView.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(getAppContext(), 8f)
            )
        )
        item.myJoshRecyclerView.setHasFixedSize(true)
        item.myJoshRecyclerView.layoutManager = linearLayoutManager

        val cardWidthPixels = (getAppContext().resources.displayMetrics.widthPixels * 0.90f).toInt()
        val cardHintPercent = 0.01f
        item.myJoshRecyclerView.addItemDecoration(
            RecyclerViewCarouselItemDecorator(
                getAppContext(),
                cardWidthPixels,
                cardHintPercent
            )
        )

        item.myJoshRecyclerView.adapter = AboutJoshAdapter(data.details)
    }
}
