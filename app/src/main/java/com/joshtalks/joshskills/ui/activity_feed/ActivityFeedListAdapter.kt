package com.joshtalks.joshskills.ui.activity_feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ActivityFeedRowItemBinding
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedResponseFirebase
import com.joshtalks.joshskills.ui.activity_feed.utils.OPEN_FEED_USER_PROFILE
import com.joshtalks.joshskills.ui.activity_feed.utils.OPEN_PROFILE_IMAGE_FRAGMENT

class ActivityFeedListAdapter :
    RecyclerView.Adapter<ActivityFeedListAdapter.ViewHolder>() {
    var itemClick: ((ActivityFeedResponseFirebase, Int) -> Unit)? = null
    var items: ArrayList<ActivityFeedResponseFirebase> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = DataBindingUtil.inflate<ActivityFeedRowItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.activity_feed_row_item,
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun setListener(function: ((ActivityFeedResponseFirebase, Int) -> Unit)?) {
        itemClick = function
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val view: ActivityFeedRowItemBinding) :
        RecyclerView.ViewHolder(view.root) {
        fun bind(activityFeedResponse: ActivityFeedResponseFirebase) {
            view.itemData = activityFeedResponse
            view.rootView.setOnClickListener {
                itemClick?.invoke(activityFeedResponse, OPEN_FEED_USER_PROFILE)
            }
            view.updatedPic.setOnClickListener {
                itemClick?.invoke(activityFeedResponse, OPEN_PROFILE_IMAGE_FRAGMENT)
            }
        }
    }
    fun addFeedToList(feedList: ArrayList<ActivityFeedResponseFirebase>) {
        items = feedList
        notifyDataSetChanged()
    }
}