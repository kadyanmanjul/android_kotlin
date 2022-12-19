package com.joshtalks.joshskills.common.ui.voip.favorite.adapter

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.common.repository.local.entity.practise.FavoriteCaller

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