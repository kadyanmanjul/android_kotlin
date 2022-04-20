package com.joshtalks.badebhaiya.feed

import android.content.res.ColorStateList
import android.widget.ImageView
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
import com.joshtalks.badebhaiya.utils.DEFAULT_NAME
import com.joshtalks.badebhaiya.utils.setUserImageOrInitials

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
            //view.isEnabled = true
            view.setOnClickListener { callback?.joinRoom(roomListResponseItem, view) }
        }
        NOT_SCHEDULED -> {
            view.text = view.context.getString(R.string.set_reminder)
            view.setTextColor(ColorStateList.valueOf(view.context.resources.getColor(R.color.white)))
            view.backgroundTintList =
                ColorStateList.valueOf(view.context.resources.getColor(R.color.reminder_on_button_color))
            //view.isEnabled = true
            view.setOnClickListener {
                roomListResponseItem.conversationRoomType = SCHEDULED
                view.text = view.context.getString(R.string.reminder_on)
                view.setTextColor(ColorStateList.valueOf(view.context.resources.getColor(R.color.reminder_on_button_color)))
                view.backgroundTintList =
                    ColorStateList.valueOf(view.context.resources.getColor(R.color.base_app_color))
                callback?.setReminder(roomListResponseItem, view)
                view.setOnClickListener { callback?.viewRoom(roomListResponseItem, view) }
            }
        }
        SCHEDULED -> {
            view.text = view.context.getString(R.string.reminder_on)
            view.setTextColor(ColorStateList.valueOf(view.context.resources.getColor(R.color.reminder_on_button_color)))
            view.backgroundTintList =
                ColorStateList.valueOf(view.context.resources.getColor(R.color.base_app_color))
            //view.isEnabled = true
            view.setOnClickListener {
                roomListResponseItem.conversationRoomType = NOT_SCHEDULED
                view.text = view.context.getString(R.string.set_reminder)
                view.setTextColor(ColorStateList.valueOf(view.context.resources.getColor(R.color.white)))
                view.backgroundTintList =
                    ColorStateList.valueOf(view.context.resources.getColor(R.color.reminder_on_button_color))
                callback?.deleteReminder(roomListResponseItem, view)
                view.setOnClickListener { callback?.viewRoom(roomListResponseItem, view) }
            }
        }
    }
}
@BindingAdapter("isImageRequired", "imageUrl", "userName", "isRoundImage", "initialsFontSize", "imageCornerRadius", requireAll = false)
fun ImageView.setDPUrl(isImageRequired: Boolean, url: String?, userName: String?, isRound: Boolean = false, dpToPx: Int, radius: Float) {
    if (isImageRequired)
        this.setUserImageOrInitials(url, userName ?: DEFAULT_NAME, dpToPx, isRound, radius.toInt())
}