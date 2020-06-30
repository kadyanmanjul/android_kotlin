package com.joshtalks.joshskills.ui.view_holders

import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.course_detail.CardType

abstract class CourseDetailsBaseCell(
    open val type: CardType = CardType.OTHER_INFO,
    open val sequenceNumber: Int
) {

    fun getAppContext() = AppObjectController.joshApplication

    fun setDefaultImageView(iv: ImageView, url: String) {
        Glide.with(getAppContext())
            .load(url)
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .into(iv)
    }

    fun setCircleImageInView(imgView: AppCompatImageView, url: String) {
        Glide.with(getAppContext())
            .load(url)
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .into(imgView)
    }
}
