package com.joshtalks.joshskills.ui.group.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.GroupRequestItemBinding
import com.joshtalks.joshskills.base.model.groups.GroupMemberRequest

class GroupRequestAdapter(var requestList: List<GroupMemberRequest> = listOf()) :
    RecyclerView.Adapter<GroupRequestAdapter.RequestViewHolder>() {

    var itemClick: ((String, String, Boolean) -> Unit)? = null
    var openProfileOnClick: ((String) -> Unit)? = null

    inner class RequestViewHolder(private val item: GroupRequestItemBinding) :
        RecyclerView.ViewHolder(item.root) {

        //TODO : Improvise the logic for onClick (only on success)
        fun onBind(request: GroupMemberRequest) {
            item.itemData = request
            item.declineJoin.setOnClickListener {
                itemClick?.invoke(request.mentorId, request.memberName, false)
                item.requestItemButtons.visibility = GONE
                item.memberAnswer.text = "DECLINED"
            }
            item.allowToJoin.setOnClickListener {
                itemClick?.invoke(request.mentorId, request.memberName, true)
                item.requestItemButtons.visibility = GONE
                item.memberAnswer.text = "ACCEPTED"
                item.memberAnswer.setTextColor(Color.parseColor("#107BE5"))
            }
            item.memberName.setOnClickListener {
                openProfileOnClick?.invoke(request.mentorId)
            }
            item.memberIconImg.setOnClickListener {
                openProfileOnClick?.invoke(request.mentorId)
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

    fun setListener(function: ((String, String, Boolean) -> Unit)?) {
        itemClick = function
    }

    fun setProfileOpen(function: (String) -> Unit) {
        openProfileOnClick = function
    }

    fun addRequestsToList(requests: List<GroupMemberRequest>) {
        requestList = requests
        notifyDataSetChanged()
    }
}