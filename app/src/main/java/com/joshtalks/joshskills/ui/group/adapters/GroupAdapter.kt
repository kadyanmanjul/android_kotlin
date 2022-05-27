package com.joshtalks.joshskills.ui.group.adapters

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.constants.INIT_GROUP_CBC_TOOLTIP
import com.joshtalks.joshskills.constants.REMOVE_GROUP_AND_CLOSE
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.databinding.GroupItemBinding
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_SEEN_GROUP_LIST_CBC_TOOLTIP

class GroupAdapter(diffCallback: DiffUtil.ItemCallback<GroupItemData>,var search:String = EMPTY) : PagingDataAdapter<GroupItemData, GroupAdapter.GroupViewHolder>(
    diffCallback
) {
    var itemClick : ((GroupItemData)->Unit)? = null

    inner class GroupViewHolder(val item : GroupItemBinding) : RecyclerView.ViewHolder(item.root) {
        fun onBind(data : GroupItemData) {
           item.itemData = data
            item.groupItemContainer.setOnClickListener {
                itemClick?.invoke(data)
            }

            if (!PrefManager.getBoolValue(HAS_SEEN_GROUP_LIST_CBC_TOOLTIP)) {
                Handler(Looper.getMainLooper()).post {
                    val messageObj = Message()
                    messageObj.what = INIT_GROUP_CBC_TOOLTIP
                    EventLiveData.value = messageObj
                }
            }

            if (search == "search") {
                if (data.getLastMessageText() == EMPTY && data.getGroupCategory() == "fpp") {
                    item.groupItemContainer.visibility = View.GONE
                } else {
                    item.groupItemContainer.visibility = View.VISIBLE
                }
            }
        }
    }

    fun setListener(function : (GroupItemData)->Unit) {
        itemClick = function
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        getItem(position)?.let { holder.onBind(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = DataBindingUtil.inflate<GroupItemBinding>(LayoutInflater.from(parent.context), R.layout.group_item, parent, false)
        return GroupViewHolder(view)
    }
}