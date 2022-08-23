package com.joshtalks.joshskills.ui.callWithExpert.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.ItemAmountBinding

class AmountAdapter(
    private val amountList: List<String>,
    private val onItemClick: (String) -> Unit
): RecyclerView.Adapter<AmountAdapter.AmountViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AmountViewHolder {
        val binding = ItemAmountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AmountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AmountViewHolder, position: Int) {
        holder.setData(amountList[position])
    }

    override fun getItemCount(): Int = amountList.size

    inner class AmountViewHolder(private val binding: ItemAmountBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(amount: String) {
            with(binding) {
                this.amount = amount
                root.setOnClickListener {
                    onItemClick.invoke(amount)
                }
            }
        }
    }
}