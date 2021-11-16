package com.joshtalks.joshskills.ui.group.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.ui.group.model.GroupMember
import com.joshtalks.joshskills.databinding.GroupMemberItemBinding
import com.joshtalks.joshskills.ui.group.viewmodels.GroupChatViewModel

class GroupMemberAdapter(val vm: GroupChatViewModel, val memberList: List<GroupMember>) :
    RecyclerView.Adapter<GroupMemberAdapter.MemberViewHolder>() {

    val memberLimit: Int = 6

    inner class MemberViewHolder(private val item: GroupMemberItemBinding) :
        RecyclerView.ViewHolder(item.root) {
        fun onBind(member: GroupMember) {
            item.itemData = member
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = DataBindingUtil.inflate<GroupMemberItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.group_member_item,
            parent,
            false
        )
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.onBind(memberList[position])
    }

    override fun getItemCount(): Int {
        return when {
            vm.showAllMembers.get() -> memberList.size
            memberList.size >= memberLimit -> memberLimit
            else -> memberList.size
        }
    }
}