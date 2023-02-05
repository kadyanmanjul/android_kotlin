package com.joshtalks.joshskills.premium.ui.inbox.adapter

import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.core.Utils
import com.joshtalks.joshskills.premium.core.setUserImageOrInitials
import com.joshtalks.joshskills.premium.repository.local.entity.practise.FavoriteCaller
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.model.CourseDetailsList
import com.joshtalks.joshskills.premium.ui.view_holders.ROUND_CORNER
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

object InboxBindingAdapter {

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

    @BindingAdapter(value = ["favoriteCallerImage"], requireAll = false)
    @JvmStatic
    fun favoriteCallerImage(imageView: ImageView, caller: FavoriteCaller?) {
        caller?.let {
            imageView.setUserImageOrInitials(it.image, it.name, isRound = true)
        } ?: imageView.setImageResource(R.drawable.ic_call_placeholder)
    }

    @BindingAdapter("recommendedCourseListAdapter", "onCourseItemClick")
    @JvmStatic
    fun recommendedCourseListAdapter(
        view: RecyclerView,
        adapter: InboxRecommendedAdapter,
        function: ((InboxRecommendedCourse, Int, Int) -> Unit)?
    ) {
        view.visibility = View.VISIBLE
        view.setHasFixedSize(false)
        view.adapter = adapter
        adapter.setListener(function)
    }

    @BindingAdapter(value = ["recommendedImage"], requireAll = false)
    @JvmStatic
    fun recommendedImage(imageView: ImageView, caller: InboxRecommendedCourse?) {
        caller?.let {
            imageView.setUserImageOrInitials(it.courseIcon, it.courseName, isRound = true)
        } ?: imageView.setImageResource(R.drawable.ic_call_placeholder)
    }
}
