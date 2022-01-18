package com.joshtalks.joshskills.ui.voip.voip_rating.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.getDrawable
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.textColorSet
import com.joshtalks.joshskills.databinding.ItemReportLayoutBinding
import com.joshtalks.joshskills.ui.voip.voip_rating.model.OptionModel
import timber.log.Timber

class ReportAdapter(
    private val optionList: List<OptionModel>,
    private val context: Context,
    val listener: (Int) -> Unit
) : RecyclerView.Adapter<ReportAdapter.ViewHolder>() {


    inner class ViewHolder(val binding: ItemReportLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OptionModel) {
            binding.issue = item
        }

    }

    var prevHolder: ViewHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val listItemBinding = ItemReportLayoutBinding.inflate(inflater, parent, false)
        return ViewHolder(listItemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(optionList[position])
        holder.itemView.setOnClickListener {
            listener(optionList[position].id)

            holder.binding.issueItem.textColorSet(R.color.white)
            holder.binding.issueItem.background =
                getDrawable(context, R.drawable.black_button_round_enabled)

            if (prevHolder != null && prevHolder != holder) {
                prevHolder!!.binding.issueItem.textColorSet(R.color.report_black)
                prevHolder!!.binding.issueItem.background =
                    getDrawable(context, R.drawable.white_button_round_enabled)

            }

            prevHolder = holder

        }


    }

    override fun getItemCount(): Int {
        return optionList.size
    }

}