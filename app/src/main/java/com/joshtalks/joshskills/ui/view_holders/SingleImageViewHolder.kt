package com.joshtalks.joshskills.ui.view_holders

import androidx.appcompat.widget.AppCompatImageView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View


@Layout(R.layout.single_image_view_holder)
class SingleImageViewHolder(
    override val sequenceNumber: Int,
    val url: String, var title: String? = null
) : CourseDetailsBaseCell(CardType.OTHER_INFO, sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.text_title)
    lateinit var titleTV: JoshTextView

    @View(R.id.image_view)
    lateinit var imageView: AppCompatImageView

    @Resolve
    fun onViewInflated() {
        setDefaultImageView(imageView, url)
        if (title.isNullOrEmpty().not()) {
            titleTV.text = title
            titleTV.visibility = android.view.View.VISIBLE
        }

    }
}