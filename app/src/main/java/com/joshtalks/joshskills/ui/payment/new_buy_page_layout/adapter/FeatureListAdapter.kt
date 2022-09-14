package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.ItemCourseDetailsBinding

class FeatureListAdapter(var amountList: List<String>? = listOf()) : RecyclerView.Adapter<FeatureListAdapter.FeatureViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val binding  = ItemCourseDetailsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeatureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        holder.setData(amountList?.get(position) ?: EMPTY)
    }

    override fun getItemCount(): Int = amountList?.size ?:0

    fun addFeatureList(members: List<String>?) {
        amountList = members
        notifyDataSetChanged()
    }

    inner class FeatureViewHolder(private val binding: ItemCourseDetailsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(name: String) {
            with(binding) {
                Log.d("sagar", "setData: $name", )
                this.featureName.text = name
            }
        }
    }
}