package com.joshtalks.joshskills.ui.cohort_based_course.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.ItemTimePickBinding
import com.joshtalks.joshskills.ui.cohort_based_course.models.CohortItemModel
import com.joshtalks.joshskills.ui.voip.voip_rating.model.OptionModel

class ScheduleAdapter(

    private val cohortItemModelList:ArrayList<CohortItemModel>
    ): RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    var itemClick: ((String) -> Unit)? = null

    inner class ViewHolder(binding: ItemTimePickBinding):
        RecyclerView.ViewHolder(binding.root){
            val text =binding.txtViewTimeSlot
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemBinding = ItemTimePickBinding.inflate(inflater,parent,false)
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.text.text = cohortItemModelList[position].timeSlot
        holder.itemView.setOnClickListener {
            itemClick?.invoke(holder.text.text.toString())
        }
    }

    override fun getItemCount(): Int {
        return cohortItemModelList.size
    }

    fun setClickListener(function:((timeSlot:String)->Unit)?){
        itemClick=function
    }
}