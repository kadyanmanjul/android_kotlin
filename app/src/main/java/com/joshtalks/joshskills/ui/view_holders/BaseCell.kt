package com.joshtalks.joshskills.ui.view_holders

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.model.Mentor
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

const val IMAGE_SIZE = 400
const val ROUND_CORNER = 8

abstract class BaseCell {
    fun getUserId() = Mentor.getInstance().getId()
    fun getDrawablePadding() = Utils.dpToPx(getAppContext(), 4f)

    fun getScreenWidth() {
        Resources.getSystem().displayMetrics.widthPixels
    }

    fun getAppContext() = AppObjectController.joshApplication


    fun setBlurImageInImageView(iv: AppCompatImageView?, url: String, callback: Runnable? = null) {
        if (iv != null) {
            val multi = MultiTransformation(
                RoundedCornersTransformation(
                    Utils.dpToPx(ROUND_CORNER),
                    0,
                    RoundedCornersTransformation.CornerType.ALL
                )
            )
            Glide.with(getAppContext())
                .load(url)
                .override(Target.SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .apply(RequestOptions.bitmapTransform(multi))
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
                        //iv?.tag = url
                        return false
                    }

                })
                .into(iv)
        }

    }

    fun setImageInImageView(iv: AppCompatImageView?, url: String, callback: Runnable? = null) {
        if (iv != null) {
            val multi = MultiTransformation(
                CropTransformation(
                    Utils.dpToPx(IMAGE_SIZE),
                    Utils.dpToPx(IMAGE_SIZE),
                    CropTransformation.CropType.CENTER
                ),
                RoundedCornersTransformation(
                    Utils.dpToPx(ROUND_CORNER),
                    0,
                    RoundedCornersTransformation.CornerType.ALL
                )
            )

            Glide.with(getAppContext())
                .load(url)
                .override(Target.SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .apply(RequestOptions.bitmapTransform(multi))
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

                .into(iv)
        }

    }


    fun setVideoImageView(iv: AppCompatImageView, url: Int, callback: Runnable? = null) {

        val multi = MultiTransformation(
            CropTransformation(
                Utils.dpToPx(IMAGE_SIZE),
                Utils.dpToPx(IMAGE_SIZE),
                CropTransformation.CropType.CENTER
            ),
            RoundedCornersTransformation(
                Utils.dpToPx(ROUND_CORNER),
                0,
                RoundedCornersTransformation.CornerType.ALL
            )
        )
        Glide.with(getAppContext())
            .load(url)
            .override(Target.SIZE_ORIGINAL)

            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(RequestOptions.bitmapTransform(multi))
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
            .into(iv)

    }

    fun setUrlInImageView(iv: AppCompatImageView?, url: String) {
        iv?.let {
            Glide.with(getAppContext())
                .load(url)
                .override(Target.SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .thumbnail(0.1f)
                .override(200, 200)
                .into(it)
        }

    }

    fun setImageViewImageNotFound(iv: AppCompatImageView) {
        Glide.with(getAppContext())
            .load(R.drawable.ic_file_error)
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(RequestOptions.bitmapTransform(AppObjectController.multiTransformation))
            .into(iv)

    }

    fun setImageInImageView(iv: ImageView?, url: String) {
        if (iv != null) {
            val multi = MultiTransformation(
                CropTransformation(
                    Utils.dpToPx(IMAGE_SIZE),
                    Utils.dpToPx(IMAGE_SIZE),
                    CropTransformation.CropType.CENTER
                ),
                RoundedCornersTransformation(
                    Utils.dpToPx(ROUND_CORNER),
                    0,
                    RoundedCornersTransformation.CornerType.ALL
                )
            )

            Glide.with(getAppContext())
                .load(url)
                .override(Target.SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .apply(RequestOptions.bitmapTransform(multi))
                .into(iv)
        }

    }

    fun setDefaultImageView(iv: ImageView, url: String) {
        val multi = MultiTransformation(
            GranularRoundedCorners(
                Utils.dpToPx(ROUND_CORNER).toFloat(),
                Utils.dpToPx(ROUND_CORNER).toFloat(),
                0F, 0F
            )
        )
        Glide.with(getAppContext())
            .load(url)
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(RequestOptions.bitmapTransform(multi))
            .into(iv)
    }

    fun setResourceInImageView(iv: ImageView?, resource: Int) {
        if (iv != null) {
            val multi = MultiTransformation(
                RoundedCornersTransformation(
                    Utils.dpToPx(ROUND_CORNER),
                    0,
                    RoundedCornersTransformation.CornerType.ALL
                )
            )

            Glide.with(getAppContext())
                .load(resource)
                .override(Target.SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .apply(RequestOptions.bitmapTransform(multi))
                .into(iv)
        }

    }

    fun setImageInImageViewV2(iv: ImageView?, url: String) {
        if (iv != null) {
            Glide.with(getAppContext())
                .load(url)
                .override(IMAGE_SIZE, IMAGE_SIZE)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .apply(
                    RequestOptions().placeholder(R.drawable.video_placeholder)
                        .error(R.drawable.video_placeholder)
                )
                .into(iv)
        }

    }


}

