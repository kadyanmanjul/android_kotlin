package com.joshtalks.joshskills.ui.newonboarding.viewholder

import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setImage
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.carousel_image_view_holder)
class CarouselImageViewHolder(var path: String) {

    @View(R.id.image_view)
    lateinit var imageView: AppCompatImageView

    @View(R.id.question)
    lateinit var question: TextView

    @Resolve
    fun onViewInflated() {
        imageView.setImage(path)

    }
}
