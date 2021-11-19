package com.joshtalks.joshskills.quizgame.ui.main.adapter

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.maps.model.Circle
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

class ImageAdapter {
    companion object{
        fun imageUrl(imageView: ImageView, url: String?) {
            val imageUrl=url?.replace("\n","")

            if (imageUrl.isNullOrEmpty()) {
                imageView.setImageResource(R.drawable.ic_josh_course)
                return
            }

            val multi = MultiTransformation(
                CropTransformation(
                    Utils.dpToPx(48),
                    Utils.dpToPx(48),
                    CropTransformation.CropType.CENTER
                ),
                RoundedCornersTransformation(
                    Utils.dpToPx(ROUND_CORNER),
                    0,
                    RoundedCornersTransformation.CornerType.ALL
                )
            )
            Glide.with(AppObjectController.joshApplication)
                .load(imageUrl)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .apply(
                    RequestOptions.bitmapTransform(multi).apply(
                        RequestOptions().placeholder(R.drawable.ic_josh_course)
                            .error(R.drawable.ic_josh_course)
                    )

                ).transform(CircleCrop())
                .into(imageView)
        }
    }
}