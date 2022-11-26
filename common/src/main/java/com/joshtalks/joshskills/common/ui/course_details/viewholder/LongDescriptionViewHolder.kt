package com.joshtalks.joshskills.common.ui.course_details.viewholder

import android.content.Context
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.MIN_LINES
import com.joshtalks.joshskills.common.repository.server.course_detail.CardType
import com.joshtalks.joshskills.common.repository.server.course_detail.LongDescription
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve



class LongDescriptionViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private var longDescription: LongDescription,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(type, sequenceNumber) {

    
    lateinit var title: TextView

    
    lateinit var description: AppCompatTextView

    
    lateinit var readMoreTV: TextView


    @Resolve
    fun onResolved() {
        title.text = longDescription.title
        description.text = longDescription.description
        description.maxLines =
            MIN_LINES
    }

    
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
