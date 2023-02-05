package com.joshtalks.joshskills.premium.ui.course_details.viewholder

import android.content.Context
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.core.MIN_LINES
import com.joshtalks.joshskills.premium.repository.server.course_detail.CardType
import com.joshtalks.joshskills.premium.repository.server.course_detail.LongDescription
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve


@Layout(R.layout.layout_long_description_card_view_holder)
class LongDescriptionViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private var longDescription: LongDescription,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(type, sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.title)
    lateinit var title: TextView

    @com.mindorks.placeholderview.annotations.View(R.id.description)
    lateinit var description: AppCompatTextView

    @com.mindorks.placeholderview.annotations.View(R.id.text_read_more)
    lateinit var readMoreTV: TextView


    @Resolve
    fun onResolved() {
        title.text = longDescription.title
        description.text = longDescription.description
        description.maxLines =
            MIN_LINES
    }

    @Click(R.id.text_read_more)
    fun onClick() {
        if (description.maxLines == MIN_LINES) {
            description.maxLines = Integer.MAX_VALUE
            readMoreTV.text = context.getString(R.string.read_less)
        } else {
            description.maxLines =
                MIN_LINES
            readMoreTV.text = context.getString(R.string.read_more)
        }
    }
}
