package com.joshtalks.joshskills.ui.cohort_based_course.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.textColorSet
import com.joshtalks.joshskills.databinding.ItemTimePickBinding
import com.joshtalks.joshskills.ui.cohort_based_course.models.CohortItemModel

class ScheduleAdapter(
    private val cohortItemModelList:ArrayList<CohortItemModel>,
    private val context: Context
    ): RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    var itemClick: ((String) -> Unit)? = null
    var prevHolder:ViewHolder?=null

    inner class ViewHolder(val binding: ItemTimePickBinding) :
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
        holder.binding.txtViewTimeSlot
        holder.itemView.setOnClickListener {
            itemClick?.invoke(cohortItemModelList[position].name)

            holder.binding.txtViewTimeSlot.textColorSet(R.color.blue_cbc_text_timeslot)
            holder.binding.crdViewTimeSlot.background = ContextCompat.getDrawable(context,R.drawable.round_rect_with_blue_border)

            if(prevHolder!=null && prevHolder!=holder){
                prevHolder?.binding?.crdViewTimeSlot?.background = ContextCompat.getDrawable(context,R.drawable.round_rect_with_blueish_border)
                prevHolder?.binding?.txtViewTimeSlot?.textColorSet( R.color.gray_cbc_text_timeslot)
            }
            prevHolder =  holder
        }

    }

    override fun getItemCount(): Int {
        return cohortItemModelList.size
    }

    fun setClickListener(function:((timeSlot:String)->Unit)?){
        itemClick=function
    }
}