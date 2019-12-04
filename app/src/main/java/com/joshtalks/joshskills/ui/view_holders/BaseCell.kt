package com.joshtalks.joshskills.ui.view_holders

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_DELIVER_STATUS
import com.joshtalks.joshskills.repository.local.entity.Sender
import com.joshtalks.joshskills.repository.local.model.Mentor
import androidx.core.graphics.drawable.DrawableCompat.setTint
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.RequestOptions.overrideOf
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.core.Utils
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import java.util.concurrent.Callable

const val IMAGE_SIZE=400
const val ROUND_CORNER=8

abstract class BaseCell() {
    fun getUserId() = Mentor.getInstance().getId()
    fun getDrawablePadding() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 4f)

    fun getScreenWidth() {
        Resources.getSystem().displayMetrics.widthPixels
    }

    fun getAppContext() = AppObjectController.joshApplication


    fun setBlurImageInImageView(iv: AppCompatImageView?, url: String, callback: Runnable? = null) {
        if (iv != null) {
            val multi = MultiTransformation<Bitmap>(
               BlurTransformation(25),
                CropTransformation(Utils.dpToPx(IMAGE_SIZE), Utils.dpToPx(IMAGE_SIZE), CropTransformation.CropType.CENTER),
                RoundedCornersTransformation(Utils.dpToPx(ROUND_CORNER), 0, RoundedCornersTransformation.CornerType.ALL)
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
            val multi = MultiTransformation<Bitmap>(
                CropTransformation(Utils.dpToPx(IMAGE_SIZE), Utils.dpToPx(IMAGE_SIZE), CropTransformation.CropType.CENTER),
                RoundedCornersTransformation(Utils.dpToPx(ROUND_CORNER), 0, RoundedCornersTransformation.CornerType.ALL)
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

        val multi = MultiTransformation<Bitmap>(
            CropTransformation(Utils.dpToPx(IMAGE_SIZE), Utils.dpToPx(IMAGE_SIZE), CropTransformation.CropType.CENTER),
            RoundedCornersTransformation(Utils.dpToPx(ROUND_CORNER), 0, RoundedCornersTransformation.CornerType.ALL)
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

    fun setUrlInImageView(iv: AppCompatImageView?, url: String){
        iv?.let {
            Glide.with(getAppContext())
                .load(url)
                .override(Target.SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .thumbnail(0.1f)

                .override(200,200)
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
            val multi = MultiTransformation<Bitmap>(
                CropTransformation(Utils.dpToPx(IMAGE_SIZE), Utils.dpToPx(IMAGE_SIZE), CropTransformation.CropType.CENTER),
                RoundedCornersTransformation(Utils.dpToPx(ROUND_CORNER), 0, RoundedCornersTransformation.CornerType.ALL)
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




}

