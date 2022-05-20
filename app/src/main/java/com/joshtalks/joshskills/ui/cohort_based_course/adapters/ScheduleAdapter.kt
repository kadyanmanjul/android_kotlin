package com.joshtalks.joshskills.ui.cohort_based_course.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.ItemTimePickBinding
import com.joshtalks.joshskills.ui.cohort_based_course.models.CohortItemModel

class ScheduleAdapter(

    private val cohortItemModelList:ArrayList<CohortItemModel>
    ): RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    var itemClick: ((String) -> Unit)? = null

    inner class ViewHolder(private val binding: ItemTimePickBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(cohortItemModel: CohortItemModel) {
            binding.itemData = cohortItemModel
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemBinding = ItemTimePickBinding.inflate(inflater,parent,false)
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(cohortItemModelList[position])
        holder.itemView.setOnClickListener {
            itemClick?.invoke(cohortItemModelList[position].name)
        }
    }

    override fun getItemCount(): Int {
        return cohortItemModelList.size
    }

    fun setClickListener(function:((timeSlot:String)->Unit)?){
        itemClick=function
    }
}