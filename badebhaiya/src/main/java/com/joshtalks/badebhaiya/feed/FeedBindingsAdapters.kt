package com.joshtalks.badebhaiya.feed

import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.ConversationRoomType
import com.joshtalks.badebhaiya.feed.model.ConversationRoomType.*
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.ALLOWED_JOIN_ROOM_TIME
import com.joshtalks.badebhaiya.utils.DEFAULT_NAME
import com.joshtalks.badebhaiya.utils.setUserImageOrInitials
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Runnable
import java.util.*

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

@BindingAdapter("setCardActionButton", "setCallback", "roomListItem", "adapterReference", "viewHolder")
fun setConversationRoomCardActionButton(
    view: MaterialButton,
    type: ConversationRoomType,
    callback: FeedAdapter.ConversationRoomItemCallback?,
    roomListResponseItem: RoomListResponseItem,
    adapter: RecyclerView.Adapter<FeedAdapter.FeedViewHolder>,
    viewHolder: RecyclerView.ViewHolder
) {
    when (type) {
        LIVE -> {
            view.text = view.context.getString(R.string.join_now)
            view.setTextColor(ColorStateList.valueOf(view.context.resources.getColor(R.color.white)))
            view.backgroundTintList =
                ColorStateList.valueOf(view.context.resources.getColor(R.color.reminder_on_button_color))
            Log.i("YASHENDRA", "setConversationRoomCardActionButton: ")
            view.setOnClickListener { callback?.joinRoom(roomListResponseItem, view) }
        }
        NOT_SCHEDULED -> {
            view.text = view.context.getString(R.string.set_reminder)
            view.setTextColor(ColorStateList.valueOf(view.context.resources.getColor(R.color.white)))
            view.backgroundTintList =
                ColorStateList.valueOf(view.context.resources.getColor(R.color.reminder_on_button_color))
            view.setOnClickListener {
                roomListResponseItem.conversationRoomType = SCHEDULED
                view.text = view.context.getString(R.string.reminder_on)
                view.setTextColor(ColorStateList.valueOf(view.context.resources.getColor(R.color.reminder_on_button_color)))
                view.backgroundTintList =
                    ColorStateList.valueOf(view.context.resources.getColor(R.color.base_app_color))
                callback?.setReminder(roomListResponseItem, view)
                view.setOnClickListener {
                    Timber.d("room type is => $type")
//                    callback?.viewRoom(roomListResponseItem, view)
                }
            }
            roomListResponseItem.startTime?.let { it1 -> setTimer(it1,view,roomListResponseItem,adapter,viewHolder,callback) }
        }
        SCHEDULED -> {
            Timber.d("room is scheduled already")

            view.text = view.context.getString(R.string.reminder_on)
            view.setTextColor(ColorStateList.valueOf(view.context.resources.getColor(R.color.reminder_on_button_color)))
            view.backgroundTintList =
                ColorStateList.valueOf(view.context.resources.getColor(R.color.base_app_color))
            if (roomListResponseItem.speakersData != null && User.getInstance().userId == roomListResponseItem.speakersData.userId) {
//                val startTime = roomListResponseItem.startTime ?: Long.MAX_VALUE
                if (roomListResponseItem.startTime!! <= System.currentTimeMillis()) {
                        Timber.d("JOIN ROOM BUTTOn")
                    (adapter as FeedAdapter).updateScheduleRoomStatusForSpeaker(viewHolder.absoluteAdapterPosition)
                } else {
                    Timber.d("JOIN ROOM TIMING GALAT")

                    setAlarmForLiveRoom(viewHolder, roomListResponseItem, adapter)
                }
                view.setOnClickListener(null)
            } else {

            }
            roomListResponseItem.startTime?.let { it1 -> setTimer(it1,view,roomListResponseItem,adapter,viewHolder,callback) }
        }

    }
}

fun setTimer(time:Long,view: MaterialButton,roomListResponseItem: RoomListResponseItem,adapter: RecyclerView.Adapter<FeedAdapter.FeedViewHolder>,
             viewHolder: RecyclerView.ViewHolder,callback: FeedAdapter.ConversationRoomItemCallback?)
{
    val count= time-Date().time
    val handler5 = Handler(Looper.getMainLooper())
    handler5.postDelayed({view.text = view.context.getString(R.string.join_now)
        view.setTextColor(ColorStateList.valueOf(view.context.resources.getColor(R.color.white)))
        roomListResponseItem.conversationRoomType = LIVE
        view.backgroundTintList =
            ColorStateList.valueOf(view.context.resources.getColor(R.color.reminder_on_button_color))
        view.text="Join this room"
        if (viewHolder.absoluteAdapterPosition != -1)
            (adapter as FeedAdapter).updateScheduleRoomStatusForSpeaker(viewHolder.absoluteAdapterPosition)
        view.setOnClickListener {
            callback?.joinRoom(roomListResponseItem, view) }
    }, count)
}

@BindingAdapter("isImageRequired", "imageUrl", "userName", "isRoundImage", "initialsFontSize", "imageCornerRadius", requireAll = false)
fun ImageView.setDPUrl(isImageRequired: Boolean, url: String?, userName: String?, isRound: Boolean = false, dpToPx: Int, radius: Float) {
    if (isImageRequired)
        this.setUserImageOrInitials(url, userName ?: DEFAULT_NAME, dpToPx, isRound, radius.toInt())
}

fun setAlarmForLiveRoom(viewHolder: RecyclerView.ViewHolder, room: RoomListResponseItem, adapter: RecyclerView.Adapter<FeedAdapter.FeedViewHolder>) {
    CoroutineScope(Dispatchers.Default).launch {
        delay((room.startTime!! - room.currentTime))
        withContext(Dispatchers.Main) {
            if (viewHolder.absoluteAdapterPosition != -1) {
                (adapter as FeedAdapter).updateScheduleRoomStatusForSpeaker(viewHolder.absoluteAdapterPosition)
            }
        }
    }
}
