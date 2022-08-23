package com.joshtalks.joshskills.ui.callWithExpert.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.ItemExpertListBinding
import com.joshtalks.joshskills.ui.callWithExpert.model.ExpertListModel

class ExpertListAdapter(var items: List<ExpertListModel> = listOf()) :
    RecyclerView.Adapter<ExpertListAdapter.ExpertViewHolder>() {

    inner class ExpertViewHolder(val itemExpertListBinding: ItemExpertListBinding) :
        RecyclerView.ViewHolder(itemExpertListBinding.root) {
        fun bind(item: ExpertListModel) {
            itemExpertListBinding.item = item
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpertListAdapter.ExpertViewHolder {
        val binding = ItemExpertListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpertViewHolder(binding)
    }

    fun addRecentCallToList(members: List<ExpertListModel>) {
        items = members
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ExpertViewHolder, position: Int) {
        holder.bind(items[position])
    }
}