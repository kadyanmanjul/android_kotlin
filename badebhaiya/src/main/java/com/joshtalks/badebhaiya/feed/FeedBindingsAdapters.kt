package com.joshtalks.badebhaiya.feed

import android.content.res.ColorStateList
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.ConversationRoomType
import com.joshtalks.badebhaiya.feed.model.ConversationRoomType.*
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem

private const val TAG = "GroupBindingAdapter"

@BindingAdapter("feedAdapter", "onFeedItemClick")
fun setFeedAdapter(
    view: RecyclerView,
    adapter: FeedAdapter,
    callback: FeedAdapter.ConversationRoomItemCallback
) {
    view.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
    view.setHasFixedSize(false)
    view.adapter = adapter
    adapter.setListener(callback)
}

@BindingAdapter("onRefresh", "setRefreshing")
fun setSwipeToRefreshAdapter(
    view: SwipeRefreshLayout,
    function: (() -> Unit)?,
    isRefreshing: Boolean = false
) {
    view.setOnRefreshListener {
        function?.invoke()
    }
    view.isRefreshing = isRefreshing
}

@BindingAdapter("setCardActionButton", "setCallback", "roomListItem")
fun setConversationRoomCardActionButton(
    view: MaterialButton,
    type: ConversationRoomType,
    callback: FeedAdapter.ConversationRoomItemCallback?,
    roomListResponseItem: RoomListResponseItem
) {
    when (type) {
        LIVE -> {
            view.text = view.context.getString(R.string.join_now)
            view.setTextColor(ColorStateList.valueOf(view.context.resources.getColor(R.color.white)))
            view.backgroundTintList =
                ColorStateList.valueOf(view.context.resources.getColor(R.color.reminder_on_button_color))
            view.isEnabled = true
            view.setOnClickListener { callback?.joinRoom(roomListResponseItem, view) }
        }
        NOT_SCHEDULED -> {
            view.text = view.context.getString(R.string.set_reminder)
            view.setTextColor(ColorStateList.valueOf(view.context.resources.getColor(R.color.white)))
            view.backgroundTintList =
                ColorStateList.valueOf(view.context.resources.getColor(R.color.reminder_on_button_color))
            view.isEnabled = true
            view.setOnClickListener { callback?.setReminder(roomListResponseItem, view) }
        }
        SCHEDULED -> {
            view.text = view.context.getString(R.string.reminder_on)
            view.setTextColor(ColorStateList.valueOf(view.context.resources.getColor(R.color.reminder_on_button_color)))
            view.backgroundTintList =
                ColorStateList.valueOf(view.context.resources.getColor(R.color.reminder_button_color))
            view.isEnabled = false
        }
    }
}