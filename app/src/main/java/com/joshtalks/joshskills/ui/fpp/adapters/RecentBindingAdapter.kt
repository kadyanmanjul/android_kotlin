package com.joshtalks.joshskills.ui.fpp.adapters

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.repository.local.entity.practise.FavoriteCaller
import com.joshtalks.joshskills.ui.fpp.model.RecentCall
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

object RecentBindingAdapter {

    @BindingAdapter(value = ["imageUrl"], requireAll = false)
    @JvmStatic
    fun imageUrl(imageView: ImageView, url: String?) {
        if (url.isNullOrEmpty()) {
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
            .load(url)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(
                RequestOptions.bitmapTransform(multi).apply(
                    RequestOptions().placeholder(R.drawable.ic_josh_course)
                        .error(R.drawable.ic_josh_course)
                        .format(DecodeFormat.PREFER_RGB_565)
                        .disallowHardwareConfig().dontAnimate().encodeQuality(75)
                )
            )
            .thumbnail(0.05f)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(imageView)
    }

    @BindingAdapter(value = ["recentCallImage"], requireAll = false)
    @JvmStatic
    fun recentCallImage(imageView: ImageView, caller: RecentCall?) {
        caller?.let {
            imageView.setUserImageOrInitials(it.photoUrl, it.firstName?:"", isRound = true)
        } ?: imageView.setImageResource(R.drawable.ic_call_placeholder)
    }
}
