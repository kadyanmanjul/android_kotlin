package com.joshtalks.joshskills.core.extension

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
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
    placeholderImage: Int = R.drawable.video_placeholder,
    scaleDownImage: Boolean = false,
    canRoundCorner: Boolean = true,
    context: Context = AppObjectController.joshApplication
) {
    val multi = if (canRoundCorner) {
        getCropWithRoundTransformation(imageWidth, imageHeight)
    } else {
        getNoCropNoRoundTransformation()
    }

    val requestOptions =
        RequestOptions.bitmapTransform(multi).placeholder(placeholderImage)
            .error(placeholderImage)
    if (scaleDownImage) {
        requestOptions
            .format(DecodeFormat.PREFER_RGB_565)
            .disallowHardwareConfig().dontAnimate().encodeQuality(75)
    }
    Glide.with(context)
        .load(url)
        .override(
            (AppObjectController.screenWidth * .8).toInt(),
            (AppObjectController.screenHeight * .5).toInt()
        )
        //.override(Target.SIZE_ORIGINAL)
        .optionalTransform(
            WebpDrawable::class.java,
            WebpDrawableTransformation(CircleCrop())
        )
        .apply(
            requestOptions
        )
        .thumbnail(0.05f)
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
                callback?.run()
                return false
            }
        })
        .into(this)
}

fun getCropWithRoundTransformation(imageWidth: Int, imageHeight: Int): MultiTransformation<Bitmap> {
    return MultiTransformation(
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
}

fun getNoCropNoRoundTransformation(): MultiTransformation<Bitmap> {
    return MultiTransformation(
        RoundedCornersTransformation(
            0,
            0,
            RoundedCornersTransformation.CornerType.ALL
        )
    )
}


fun AppCompatImageView.setImageInLessonView(
    url: String,
    callback: Runnable? = null,
    placeholderImage: Int = R.drawable.lesson_placeholder,
    context: Context = AppObjectController.joshApplication
) {
/*
    val shimmer =
        Shimmer.AlphaHighlightBuilder()// The attributes for a ShimmerDrawable is set by this builder
            .setDuration(1500) // how long the shimmering animation takes to do one full sweep
            .setBaseAlpha(0.7f) //the alpha of the underlying children
            .setHighlightAlpha(0.6f) // the shimmer alpha amount
            .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
            .setShape(Shimmer.Shape.LINEAR)
            .setAutoStart(true)
            .setFixedHeight(Utils.dpToPx(10))
            .setClipToChildren(true)
            .setRepeatMode(ValueAnimator.INFINITE)
            .build()

    val shimmerDrawable = ShimmerDrawable().apply {
        setShimmer(shimmer)
    }*/

    val requestOptions =
       // RequestOptions().placeholder(shimmerDrawable)
           //.error(shimmerDrawable)
         RequestOptions().placeholder(placeholderImage)
        .format(DecodeFormat.PREFER_RGB_565)
            .disallowHardwareConfig().dontAnimate().encodeQuality(75)
    Glide.with(context)
        .load(url)
        .override(Target.SIZE_ORIGINAL)
        .optionalTransform(
            WebpDrawable::class.java,
            WebpDrawableTransformation(CircleCrop())
        )
        .apply(
            requestOptions
        )
        //.fitCenter()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
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
                callback?.run()
                return false
            }
        })
        .into(this)
}
