package com.joshtalks.badebhaiya.feed

import android.util.Log
import android.view.View
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem

private const val TAG = "GroupBindingAdapter"

@BindingAdapter("feedAdapter", "onFeedItemClick")
fun setFeedAdapter(
    view: RecyclerView,
    adapter: FeedAdapter,
    function: ((RoomListResponseItem,View) -> Unit)?
) {
    view.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
    view.setHasFixedSize(false)
    view.adapter = adapter
    adapter.setListener(function)
}
