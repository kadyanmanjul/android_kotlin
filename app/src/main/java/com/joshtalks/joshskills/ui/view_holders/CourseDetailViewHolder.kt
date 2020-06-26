package com.joshtalks.joshskills.ui.view_holders

import androidx.appcompat.widget.AppCompatImageView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.server.CourseDetailsModel
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View


@Layout(R.layout.course_details_view_holder)
class CourseDetailViewHolder(
    override val sequenceNumber: Int,
    private val courseDetailsModel: CourseDetailsModel
) :
    CourseDetailsBaseCell(sequenceNumber) {

    @View(R.id.image_view)
    lateinit var imageView: AppCompatImageView


    @Resolve
    fun onResolved() {
        setCircleImageInView(imageView, courseDetailsModel.imageUrl)
    }
}