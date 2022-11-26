package com.joshtalks.joshskills.common.ui.userprofile.adapters

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
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.ui.userprofile.models.CourseEnrolled
import com.joshtalks.joshskills.common.ui.userprofile.models.GroupInfo
import de.hdodenhof.circleimageview.CircleImageView

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