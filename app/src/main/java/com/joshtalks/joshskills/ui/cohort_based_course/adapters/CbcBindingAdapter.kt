package com.joshtalks.joshskills.ui.cohort_based_course.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.ItemTimePickBinding
import com.joshtalks.joshskills.ui.voip.voip_rating.model.OptionModel

class ScheduleAdapter(
    private val timeStrings:List<String>
    ): RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    inner class ViewHolder(binding: ItemTimePickBinding):
        RecyclerView.ViewHolder(binding.root){
            val text =binding.txtViewTimeSlot
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d("TAG", "onCreateViewHolder: reached")
        val inflater = LayoutInflater.from(parent.context)
        val itemBinding = ItemTimePickBinding.inflate(inflater,parent,false)
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d("TAG", "onBindViewHolder: Reached")
        holder.text.text = timeStrings[position]
    }

    override fun getItemCount(): Int {
        Log.d("TAG", "getItemCount: reached")
        return timeStrings.size
    }
}