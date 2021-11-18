package com.joshtalks.joshskills.ui.group.utils

import androidx.recyclerview.widget.DiffUtil
import com.joshtalks.joshskills.ui.group.model.GroupItemData

object GroupItemComparator : DiffUtil.ItemCallback<GroupItemData>() {
    override fun areItemsTheSame(oldItem: GroupItemData, newItem: GroupItemData): Boolean {
        return oldItem.getUniqueId() == newItem.getUniqueId()
    }

    override fun areContentsTheSame(oldItem: GroupItemData, newItem: GroupItemData): Boolean {
        return oldItem.getTitle() == newItem.getTitle() //&& oldItem.getSubTitle() == newItem.getSubTitle()
    }

}