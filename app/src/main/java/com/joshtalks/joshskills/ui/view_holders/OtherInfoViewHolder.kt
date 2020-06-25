package com.joshtalks.joshskills.ui.view_holders

import android.content.Context
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.course_detail.OtherInfo
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve

@Layout(R.layout.other_info_view_holder)
class OtherInfoViewHolder(
    override val sequenceNumber: Int,
    val data: OtherInfo,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.imageView)
    lateinit var imgView: AppCompatImageView

    @Resolve
    fun onResolved() {
        setImageView(data.imgUrl, imgView)
    }

    private fun setImageView(url: String, imageView: ImageView) {
        Glide.with(context)
            .load(url)
            .fitCenter()
            .override(Target.SIZE_ORIGINAL)
            .into(imageView)
    }
}
