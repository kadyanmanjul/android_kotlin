package com.joshtalks.joshskills.ui.userprofile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.repository.server.GroupInfo
import de.hdodenhof.circleimageview.CircleImageView

class MyGroupsListAdapter(
    private val items: List<GroupInfo> = emptyList()
) : RecyclerView.Adapter<MyGroupsListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.my_groups_row_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var groupIcon: CircleImageView = view.findViewById(R.id.group_icon)
        var groupName: AppCompatTextView = view.findViewById(R.id.tv_group_name)
        var groupText: AppCompatTextView = view.findViewById(R.id.tv_minutes_spoken)
        var groupInfo: GroupInfo?=null
        fun bind(groupInfo: GroupInfo) {
            this.groupInfo=groupInfo
            groupName.text=groupInfo.groupName
            groupText.text=groupInfo.textToShow
            if(groupInfo.groupIcon==null){
                groupIcon.setImageResource(R.drawable.group_default_icon)
            }else{
                groupIcon.setImage(groupInfo.groupIcon,view.context)

            }

        }

    }

}