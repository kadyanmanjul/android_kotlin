package com.joshtalks.joshskills.ui.view_holders

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View


@Layout(R.layout.single_image_view_holder)
class SingleImageViewHolder(
    override val sequenceNumber: Int,
    val url: String,
    var title: String? = null
) : CourseDetailsBaseCell(sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.text_title)
    lateinit var titleTV: JoshTextView

    @View(R.id.cardView)
    lateinit var cardView: MaterialCardView

    @View(R.id.image_view)
    lateinit var imageView: AppCompatImageView

    @View(R.id.layout)
    lateinit var layout: LinearLayout

    @Resolve
    fun onViewInflated() {
        if (url.isNullOrBlank()) {
            cardView.background = ContextCompat.getDrawable(
                AppObjectController.joshApplication,
                R.drawable.payment_bottom
            )
            cardView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 200)
            layout.visibility = android.view.View.GONE
        } else setDefaultImageView(imageView, url)
        if (title.isNullOrEmpty().not()) {
            titleTV.text = title
            titleTV.visibility = android.view.View.VISIBLE
        }

    }
}
