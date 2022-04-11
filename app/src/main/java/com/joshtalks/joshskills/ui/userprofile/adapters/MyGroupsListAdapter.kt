package com.joshtalks.joshskills.ui.userprofile.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.MyGroupsRowItemBinding
import com.joshtalks.joshskills.ui.userprofile.models.GroupInfo

class MyGroupsListAdapter(
    var items: ArrayList<GroupInfo>? = arrayListOf()
) : RecyclerView.Adapter<MyGroupsListAdapter.ViewHolder>() {
    var itemClick: ((GroupInfo, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = DataBindingUtil.inflate<MyGroupsRowItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.my_groups_row_item,
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items?.get(position)!!)
    }

    fun setListener(function: ((GroupInfo, Int) -> Unit)?) {
        itemClick = function
    }

    override fun getItemCount(): Int = items?.size ?: 0

    inner class ViewHolder(val view: MyGroupsRowItemBinding) : RecyclerView.ViewHolder(view.root) {
        fun bind(groupInfo: GroupInfo) {
            view.itemData = groupInfo
        }
    }

    fun addMyGroupToList(myGroupList: ArrayList<GroupInfo>?) {
        items = myGroupList
        notifyDataSetChanged()
    }
}