package com.joshtalks.joshskills.ui.view_holders

import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.server.CourseDetailsModel
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View


@Layout(R.layout.course_details_view_holder)
class CourseDetailViewHolder(private val courseDetailsModel: CourseDetailsModel) : BaseCell() {

    @View(R.id.image_view)
    lateinit var imageView: AppCompatImageView


    @Resolve
    fun onResolved() {
        Glide.with(getAppContext())
            .load(courseDetailsModel.imageUrl)
            .override(Target.SIZE_ORIGINAL)

            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .into(imageView)

    }
}