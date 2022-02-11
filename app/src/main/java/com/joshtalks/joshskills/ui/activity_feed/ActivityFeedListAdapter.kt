package com.joshtalks.joshskills.ui.activity_feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ActivityFeedRowItemBinding
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedResponseFirebase
import java.util.*

class ActivityFeedListAdapter(
    private val items: ArrayList<ActivityFeedResponseFirebase>
) : RecyclerView.Adapter<ActivityFeedListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view=DataBindingUtil.inflate<ActivityFeedRowItemBinding>(
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

    override fun getItemCount(): Int = items.size
    inner class ViewHolder(val view:ActivityFeedRowItemBinding) : RecyclerView.ViewHolder(view.root) {
        fun bind(activityFeedResponse: ActivityFeedResponseFirebase) {
            view.itemData=activityFeedResponse
        }
    }
}