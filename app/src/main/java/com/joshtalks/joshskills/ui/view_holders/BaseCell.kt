package com.joshtalks.joshskills.ui.view_holders

import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import jp.wasabeef.glide.transformations.BlurTransformation
import java.util.concurrent.Callable


abstract class BaseCell() {
    fun getUserId() = Mentor.getInstance().getId()
    fun getDrawablePadding() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 4f)

    fun getScreenWidth() {
        Resources.getSystem().displayMetrics.widthPixels
    }

    fun getAppContext() = AppObjectController.joshApplication



    fun setBlurImageInImageView(iv: AppCompatImageView?, url: String,callback:Runnable?=null){
       /* if (iv.tag != null) {
            return
        }*/

        val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
        if (iv != null) {
            Glide.with(getAppContext())
                .load(url)
                // .override((AppObjectController.screenWidth*.4).toInt(),(AppObjectController.screenHeight*.6).toInt())            //.thumbnail(Glide.with(activityRef.get()!!).load(url))
                //.centerCrop()
               // .apply(RequestOptions.bitmapTransform(BlurTransformation(20, 3)))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .optionalTransform(WebpDrawable::class.java,  WebpDrawableTransformation(CircleCrop()))
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

     fun setImageInImageView(iv: AppCompatImageView?, url: String,callback:Runnable?=null){
       /* if (iv.tag != null) {
            return
        }
*/
         if (iv != null) {
             Glide.with(getAppContext())
                 .load(url)
                 .diskCacheStrategy(DiskCacheStrategy.ALL)
                 .optionalTransform(WebpDrawable::class.java,  WebpDrawableTransformation(CircleCrop()))
                 .centerCrop()
                 //.thumbnail(Glide.with(activityRef.get()!!).load(url))
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


    fun setVideoImageView(iv: AppCompatImageView, url:Int,callback:Runnable?=null){
        Glide.with(getAppContext())
            .load(url)
            .centerCrop()
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

