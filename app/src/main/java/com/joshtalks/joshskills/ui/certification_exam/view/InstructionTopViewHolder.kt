package com.joshtalks.joshskills.ui.certification_exam.view

import android.graphics.Color
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setVectorImage
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve


@Layout(R.layout.instruction_top_view_holder)
class InstructionTopViewHolder(
    val value: Int,
    var label: String,
    val image: String,
    val bgColor: String,
    val textColor: String
) {

    @com.mindorks.placeholderview.annotations.View(R.id.image_view)
    lateinit var imageView: AppCompatImageView

    @com.mindorks.placeholderview.annotations.View(R.id.tv_value)
    lateinit var tvValue: AppCompatTextView

    @com.mindorks.placeholderview.annotations.View(R.id.tv_label)
    lateinit var tvLabel: AppCompatTextView

    @com.mindorks.placeholderview.annotations.View(R.id.root_view)
    lateinit var rootview: CardView

    @Resolve
    fun onViewInflated() {
        rootview.setCardBackgroundColor(Color.parseColor(bgColor))
        tvLabel.setTextColor(Color.parseColor(textColor))
        tvValue.text = value.toString()
        tvLabel.text = label
        imageView.setVectorImage("file:///android_asset/$image.svg")
    }
}
