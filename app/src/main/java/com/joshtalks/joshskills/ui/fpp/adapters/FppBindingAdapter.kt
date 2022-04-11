package com.joshtalks.joshskills.ui.fpp.adapters

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.repository.local.entity.practise.FavoriteCaller
import com.joshtalks.joshskills.ui.fpp.model.PendingRequestDetail
import com.joshtalks.joshskills.ui.fpp.model.RecentCall
import com.joshtalks.joshskills.ui.voip.favorite.adapter.FppFavoriteAdapter

@BindingAdapter(value = ["userImage"], requireAll = false)
fun seeAllRequestUserImage(imageView: ImageView, caller: PendingRequestDetail?) {
    caller?.let {
        imageView.setUserImageOrInitials(it.photoUrl, it.fullName ?: "", isRound = true)
    } ?: imageView.setImageResource(R.drawable.ic_call_placeholder)
}

@BindingAdapter(value = ["recentCallImage"], requireAll = false)
fun recentCallImage(imageView: ImageView, caller: RecentCall?) {
    caller?.let {
        imageView.setUserImageOrInitials(it.photoUrl, it.firstName?:"", isRound = true)
    } ?: imageView.setImageResource(R.drawable.ic_call_placeholder)
}

@BindingAdapter("seeAllRequestAdapter", "onFppItemClick")
fun setSeeAllRequestMemberAdapter(
    view: RecyclerView,
    adapter: SeeAllRequestsAdapter,
    function: ((PendingRequestDetail, Int) -> Unit)?
) {
    view.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
    view.setHasFixedSize(false)
    view.adapter = adapter

    adapter.setListener(function)
}

@BindingAdapter("recentAllRequestAdapter", "onFppRecentItemClick")
fun setRecentRequestMemberAdapter(
    view: RecyclerView,
    adapter: RecentCallsAdapter,
    function: ((RecentCall, Int, Int) -> Unit)?
) {
    view.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
    view.setHasFixedSize(false)
    view.adapter = adapter

    adapter.setListener(function)
}

@BindingAdapter("favouriteListAdapter", "onFavouriteItemClick")
fun setFavouriteListAdapter(
    view: RecyclerView,
    adapter: FppFavoriteAdapter,
    function: ((FavoriteCaller, Int, Int) -> Unit)?
) {
    view.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
    view.setHasFixedSize(false)
    view.adapter = adapter

    adapter.setListener(function)
}



