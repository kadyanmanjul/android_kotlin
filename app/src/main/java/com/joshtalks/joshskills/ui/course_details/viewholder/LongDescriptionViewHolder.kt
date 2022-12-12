package com.joshtalks.joshskills.ui.course_details.viewholder

import com.google.gson.JsonObject
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.MIN_LINES
import com.joshtalks.joshskills.databinding.LayoutLongDescriptionCardViewHolderBinding
import com.joshtalks.joshskills.repository.server.course_detail.LongDescription

class LongDescriptionViewHolder(
    val item: LayoutLongDescriptionCardViewHolderBinding
) : DetailsBaseViewHolder(item) {

    override fun bindData(sequence: Int, cardData: JsonObject) {
        val data = AppObjectController.gsonMapperForLocal.fromJson(
            cardData.toString(),
            LongDescription::class.java
        )
        item.title.text = data.title
        item.description.text = data.description
        item.description.maxLines = MIN_LINES

        item.textReadMore.setOnClickListener {
            if (item.description.maxLines == MIN_LINES) {
                item.description.maxLines = Integer.MAX_VALUE
                item.textReadMore.text = getAppContext().getString(R.string.read_less)
            } else {
                item.description.maxLines = MIN_LINES
                item.textReadMore.text = getAppContext().getString(R.string.read_more)
            }
        }
    }
}
