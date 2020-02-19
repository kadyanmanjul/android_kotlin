package com.joshtalks.joshskills.ui.view_holders

import androidx.appcompat.widget.AppCompatImageView
import com.joshtalks.joshskills.R
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View


@Layout(R.layout.single_image_view_holder)
class SingleImageViewHolder(val url: String) : BaseCell() {

    @View(R.id.image_view)
    lateinit var imageView: AppCompatImageView

    @Resolve
    fun onViewInflated() {
        setDefaultImageView(imageView, url)

    }
}