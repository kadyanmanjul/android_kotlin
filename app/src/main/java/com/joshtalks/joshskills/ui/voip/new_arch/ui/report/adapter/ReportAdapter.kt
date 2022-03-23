package com.joshtalks.joshskills.ui.voip.new_arch.ui.report.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getDrawable
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.textColorSet
import com.joshtalks.joshskills.databinding.LayoutReportItemBinding
import com.joshtalks.joshskills.ui.voip.new_arch.ui.report.model.OptionModel

class ReportAdapter(
    private val optionList: List<OptionModel>,
    private val context: Context,
) : RecyclerView.Adapter<ReportAdapter.ViewHolder>() {

    var prevHolder: ViewHolder? = null
    var itemClick: ((Boolean) -> Unit)? = null
    var optionIdUpdate:((Int)->Unit)?=null
    var optionId: Int = 0

    inner class ViewHolder(val binding: LayoutReportItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OptionModel) {
            binding.issue = item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val listItemBinding = LayoutReportItemBinding.inflate(inflater, parent, false)
        return ViewHolder(listItemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(optionList[position])
        holder.itemView.setOnClickListener {
            optionId=optionList[position].id
            holder.binding.issueItem.textColorSet(R.color.white)
            holder.binding.issueItem.background =
                getDrawable(context, R.drawable.black_button_round_enabled)

            if (prevHolder != null && prevHolder != holder) {
                prevHolder!!.binding.issueItem.textColorSet(R.color.report_black)
                prevHolder!!.binding.issueItem.background =
                    getDrawable(context, R.drawable.white_button_round_enabled)
            }
            prevHolder = holder
            itemClick?.invoke(true)
            optionIdUpdate?.invoke(optionList[position].id)
        }
    }

    override fun getItemCount(): Int {
        return optionList.size
    }

    fun setBackgroundListener(function: ((Boolean) -> Unit)?) {
        itemClick = function
    }
    fun setOptionListener(function: ((Int) -> Unit)?) {
        optionIdUpdate = function
    }

}