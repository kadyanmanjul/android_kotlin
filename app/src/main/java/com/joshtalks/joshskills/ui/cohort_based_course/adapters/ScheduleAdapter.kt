package com.joshtalks.joshskills.ui.cohort_based_course.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.ItemTimePickBinding

class ScheduleAdapter(private val timeStrings:List<String>): RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {
    inner class ViewHolder(binding: ItemTimePickBinding):
            RecyclerView.ViewHolder(binding.root){
                val time = binding.txtViewTimeSlot
            }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemBinding = ItemTimePickBinding.inflate(inflater,parent,false)
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.time.text = timeStrings[position]
    }

    override fun getItemCount(): Int {
        return timeStrings.size
    }
}