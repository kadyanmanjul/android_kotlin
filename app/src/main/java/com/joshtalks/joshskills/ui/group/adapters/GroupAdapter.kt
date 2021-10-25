package com.joshtalks.joshskills.ui.group.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.GroupItemBinding
import com.joshtalks.joshskills.ui.group.model.GroupItemData

class GroupAdapter(diffCallback: DiffUtil.ItemCallback<GroupItemData>) : PagingDataAdapter<GroupItemData, GroupAdapter.GroupViewHolder>(
    diffCallback
) {
    var itemClick : ((GroupItemData)->Unit)? = null

    inner class GroupViewHolder(val item : GroupItemBinding) : RecyclerView.ViewHolder(item.root) {
        fun onBind(data : GroupItemData) {
           item.itemData = data
            item.groupItemContainer.setOnClickListener {
                itemClick?.invoke(data)
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