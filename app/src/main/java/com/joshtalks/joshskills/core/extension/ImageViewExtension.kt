package com.joshtalks.joshskills.core.extension

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.ShimmerImageView
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

const val IMAGE_WIDTH_SIZE = 500
const val IMAGE_HEIGHT_SIZE = 335

fun ImageView.setImageWithPlaceholder(
    url: String?,
    placeholderImage: Int = R.drawable.ic_josh_course,
    context: Context = AppObjectController.joshApplication
) {
    Glide.with(context)
        .load(url)
        .apply(RequestOptions().placeholder(placeholderImage).error(placeholderImage))
        .override(Target.SIZE_ORIGINAL)
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .skipMemoryCache(false)
        .into(this)
}


fun ImageView.setResourceImageDefault(
    resource: Int,
    context: Context = AppObjectController.joshApplication
) {
    Glide.with(context)
        .load(resource)
        .override(Target.SIZE_ORIGINAL)
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .skipMemoryCache(false)
        .into(this)
}

fun ShimmerImageView.setImageViewPH(
    url: String,
    callback: Runnable? = null,
    imageWidth: Int = IMAGE_WIDTH_SIZE,
    imageHeight: Int = IMAGE_HEIGHT_SIZE,
    placeholderImage: Int = R.drawable.josh_skill,
    context: Context = AppObjectController.joshApplication
) {
    val multi = MultiTransformation(
        CropTransformation(
            Utils.dpToPx(imageWidth),
            Utils.dpToPx(imageHeight),
            CropTransformation.CropType.CENTER
        ),
        RoundedCornersTransformation(
            Utils.dpToPx(ROUND_CORNER),
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
        .apply(
            RequestOptions().placeholder(R.drawable.video_placeholder)
                .error(R.drawable.video_placeholder)
        )
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        //  .skipMemoryCache(false)
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                this@setImageViewPH
                callback?.run()
                return false
            }
        })
        .into(this)
}

fun AppCompatImageView.setImageViewWRPH(
    url: String,
    context: Context = AppObjectController.joshApplication
) {
    MultiTransformation(
        RoundedCornersTransformation(
            Utils.dpToPx(ROUND_CORNER),
            0,
            RoundedCornersTransformation.CornerType.TOP
        )
    )
    Glide.with(context)
        .load(url)
        .override(
            (AppObjectController.screenWidth * .8).toInt(),
            (AppObjectController.screenHeight * .5).toInt()
        )
        .optionalTransform(
            WebpDrawable::class.java,
            WebpDrawableTransformation(CircleCrop())
        )
        .apply(
            RequestOptions().placeholder(R.drawable.video_placeholder)
                .error(R.drawable.video_placeholder)
        )
        .fitCenter()
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .skipMemoryCache(false)
        .into(this)
}

