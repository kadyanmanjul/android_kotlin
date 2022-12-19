package com.joshtalks.joshskills.certificate.view

import android.graphics.Color
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.setVectorImage
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve


class InstructionTopViewHolder(
    val value: Int,
    var label: String,
    val image: String,
    val bgColor: String,
    val textColor: String
) {

    
    lateinit var imageView: AppCompatImageView

    
    lateinit var tvValue: AppCompatTextView

    
    lateinit var tvLabel: AppCompatTextView

    
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
