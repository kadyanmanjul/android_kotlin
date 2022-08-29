package com.joshtalks.joshskills.ui.callWithExpert.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ItemExpertListBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.callWithExpert.model.ExpertListModel
import com.joshtalks.joshskills.ui.fpp.constants.FAV_CLICK_ON_CALL

class ExpertListAdapter(var items: List<ExpertListModel> = listOf()) :
    RecyclerView.Adapter<ExpertListAdapter.ExpertViewHolder>() {
    private var itemClickFunction: ((ExpertListModel, Int, Int) -> Unit)? = null

    inner class ExpertViewHolder(val itemExpertListBinding: ItemExpertListBinding) :
        RecyclerView.ViewHolder(itemExpertListBinding.root) {
        fun bind(item: ExpertListModel) {
            with(itemExpertListBinding) {
                itemExpertListBinding.item = item
                expertCallButton.setOnClickListener {
                    if (item.mentorId != Mentor.getInstance().getId()) {
                        itemClickFunction?.invoke(item, FAV_CLICK_ON_CALL, bindingAdapterPosition)
                    } else {
                        showToast("You are an expert")
                    }
                }
            }
        }

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpertListAdapter.ExpertViewHolder {
        val binding = ItemExpertListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpertViewHolder(binding)
    }


    fun setItemClickFunction(function: ((ExpertListModel, Int, Int) -> Unit)?) {
        itemClickFunction = function
    }

    fun addExpertToList(members: List<ExpertListModel>) {
        items = members
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ExpertViewHolder, position: Int) {
        holder.bind(items[position])
    }
}