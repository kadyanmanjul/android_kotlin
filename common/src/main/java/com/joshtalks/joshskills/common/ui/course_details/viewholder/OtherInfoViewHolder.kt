package com.joshtalks.joshskills.common.ui.course_details.viewholder

import android.content.Context
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.repository.server.course_detail.CardType
import com.joshtalks.joshskills.common.repository.server.course_detail.OtherInfo
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve


class OtherInfoViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    val data: OtherInfo?,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(type, sequenceNumber) {

    
    lateinit var imgView: AppCompatImageView

    @Resolve
    fun onResolved() {
        if (sequenceNumber != -1)
            data?.imgUrl?.let {
                setImageView(data.imgUrl, imgView)
            }
        else imgView.setImageDrawable(
            ContextCompat.getDrawable(
                AppObjectController.joshApplication,
                R.drawable.payment_bottom
            )
        )
    }

    private fun setImageView(url: String, imageView: ImageView) {
        Glide.with(context)
            .load(url)
            .fitCenter()
            .override(Target.SIZE_ORIGINAL)
            .into(imageView)
    }
}