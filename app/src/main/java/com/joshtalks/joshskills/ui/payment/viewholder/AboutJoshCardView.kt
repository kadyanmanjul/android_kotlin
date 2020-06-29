package com.joshtalks.joshskills.ui.payment.viewholder

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
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.server.course_detail.Detail
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import jp.wasabeef.glide.transformations.RoundedCornersTransformation


@Layout(R.layout.layout_about_josh_card)
class AboutJoshCardView(
    private var aboutJoshData: Detail,
    private val context: Context = AppObjectController.joshApplication
) {
    @com.mindorks.placeholderview.annotations.View(R.id.description)
    lateinit var description: TextView

    @com.mindorks.placeholderview.annotations.View(R.id.title)
    lateinit var title: TextView

    @com.mindorks.placeholderview.annotations.View(R.id.image)
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
}
