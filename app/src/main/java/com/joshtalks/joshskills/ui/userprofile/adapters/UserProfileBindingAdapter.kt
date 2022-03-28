package com.joshtalks.joshskills.ui.userprofile.adapters

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.ui.userprofile.models.CourseEnrolled
import com.joshtalks.joshskills.ui.userprofile.models.GroupInfo
import de.hdodenhof.circleimageview.CircleImageView


@BindingAdapter("imageResource")
fun CircleImageView.setImage(url: String?) {
    if (url.isNullOrEmpty()) {
        this.setImageResource(R.drawable.ic_call_placeholder)
    } else {
        val requestOptions = RequestOptions().placeholder(R.drawable.ic_call_placeholder)
            .error(R.drawable.ic_call_placeholder)
            .format(DecodeFormat.PREFER_RGB_565)
            .disallowHardwareConfig().dontAnimate().encodeQuality(75)
        Glide.with(AppObjectController.joshApplication)
            .load(url)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(requestOptions)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(this)
    }
}

@BindingAdapter("enrolledListAdapter", "onEnrolledItemClick")
fun setEnrolledCoursesAdapter(
    view: RecyclerView,
    adapter: EnrolledCoursesListAdapter,
    function: ((CourseEnrolled, Int) -> Unit)?,
) {
    view.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

    view.setHasFixedSize(false)
    view.adapter = adapter

    adapter.setListener(function)
}

@BindingAdapter("groupListAdapter","onGroupItemClick")
fun setMyGroupAdapter(
    view: RecyclerView,
    adapter: MyGroupsListAdapter,
    function: ((GroupInfo, Int) -> Unit)?,
) {
    view.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

    view.setHasFixedSize(false)
    view.adapter = adapter

    adapter.setListener(function)
}