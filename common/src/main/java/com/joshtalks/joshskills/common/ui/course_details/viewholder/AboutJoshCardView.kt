package com.joshtalks.joshskills.common.ui.course_details.viewholder

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.ImageShowEvent
import com.joshtalks.joshskills.common.repository.server.course_detail.Detail
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import jp.wasabeef.glide.transformations.RoundedCornersTransformation


class AboutJoshCardView(
    private var aboutJoshData: Detail,
    private val context: Context = AppObjectController.joshApplication
) {
    
    lateinit var description: TextView

    
    lateinit var title: TextView

    
    lateinit var image: ImageView

    @Resolve
    fun onResolved() {
        title.text = aboutJoshData.title
        description.text = aboutJoshData.description
        setImageView(aboutJoshData.imageUrl)
    }

    private fun setImageView(url: String) {
        val multi = MultiTransformation(
            RoundedCornersTransformation(
                Utils.dpToPx(4),
                0,
                RoundedCornersTransformation.CornerType.ALL
            )
        )
        Glide.with(context)
            .load(url)
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(RequestOptions.bitmapTransform(multi))
            .into(image)


    }

    
    fun onClick() {
        com.joshtalks.joshskills.common.messaging.RxBus2.publish(ImageShowEvent(aboutJoshData.imageUrl, aboutJoshData.imageUrl))
    }
}