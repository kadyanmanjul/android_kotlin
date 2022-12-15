package com.joshtalks.joshskills.groups.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.groups.R
import com.joshtalks.joshskills.groups.databinding.GroupMemberItemBinding
import com.joshtalks.joshskills.common.repository.local.entity.groups.GroupMember

class GroupMemberAdapter(var memberList: List<GroupMember> = listOf()) :
    RecyclerView.Adapter<GroupMemberAdapter.MemberViewHolder>() {

    val memberLimit: Int = 6
    var showAllMembers = false
    var itemClick: ((GroupMember, View) -> Unit)? = null

    inner class MemberViewHolder(private val item: GroupMemberItemBinding) :
        RecyclerView.ViewHolder(item.root) {
        fun onBind(member: GroupMember) {
            item.itemData = member
            item.memberContainer.setOnClickListener {
                itemClick?.invoke(member, item.root)
            }
        }
    }

    fun setListener(function: ((GroupMember, View) -> Unit)?) {
        itemClick = function
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
            showAllMembers -> memberList.size
            memberList.size >= memberLimit -> memberLimit
            else -> memberList.size
        }
    }

    fun shouldShowAll(boolean: Boolean) {
        showAllMembers = boolean
        notifyDataSetChanged()
    }

    fun addMembersToList(members: List<GroupMember>) {
        memberList = members
        showAllMembers = false
        notifyDataSetChanged()
    }
}