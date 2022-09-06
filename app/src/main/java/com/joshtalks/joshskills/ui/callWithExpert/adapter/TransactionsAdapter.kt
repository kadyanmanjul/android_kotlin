package com.joshtalks.joshskills.ui.callWithExpert.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.ItemWalletTransactionBinding
import com.joshtalks.joshskills.ui.callWithExpert.model.cardDetails

class TransactionsAdapter(var items: List<cardDetails> = listOf()):RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder>() {
    inner class TransactionViewHolder(val itemBinding: ItemWalletTransactionBinding):RecyclerView.ViewHolder(itemBinding.root){
        fun bind(itemBinding: cardDetails){

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionsAdapter.TransactionViewHolder {
        val binding = ItemWalletTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }


    fun addTransactionToList(members: List<cardDetails>) {
        items = members
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: TransactionsAdapter.TransactionViewHolder, position: Int) {
        holder.bind(items[position])
    }
}