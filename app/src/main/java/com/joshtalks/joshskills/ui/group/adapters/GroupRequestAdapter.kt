package com.joshtalks.joshskills.ui.group.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.GroupRequestItemBinding
import com.joshtalks.joshskills.ui.group.model.GroupMemberRequest

class GroupRequestAdapter(var requestList: List<GroupMemberRequest> = listOf()):
    RecyclerView.Adapter<GroupRequestAdapter.RequestViewHolder>() {

    var itemClick: ((Boolean) -> Unit)? = null

    inner class RequestViewHolder(private val item: GroupRequestItemBinding) :
        RecyclerView.ViewHolder(item.root) {
        fun onBind(request: GroupMemberRequest) {
            item.itemData = request
            item.declineJoin.setOnClickListener {
                itemClick?.invoke(false)
            }
            item.allowToJoin.setOnClickListener {
                itemClick?.invoke(true)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = DataBindingUtil.inflate<GroupRequestItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.group_request_item,
            parent,
            false
        )
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.onBind(requestList[position])
    }

    override fun getItemCount(): Int {
        return requestList.size
    }

    fun setListener(function: ((Boolean) -> Unit)?) {
        itemClick = function
    }

    fun addRequestsToList(requests: List<GroupMemberRequest>) {
        requestList = requests
        notifyDataSetChanged()
    }
}