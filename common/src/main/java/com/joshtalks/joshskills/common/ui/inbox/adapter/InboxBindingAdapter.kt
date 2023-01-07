package com.joshtalks.joshskills.common.ui.inbox.adapter

import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.setUserImageOrInitials
import com.joshtalks.joshskills.common.repository.local.entity.practise.FavoriteCaller
import com.joshtalks.joshskills.common.ui.view_holders.ROUND_CORNER
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

object InboxBindingAdapter {

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
