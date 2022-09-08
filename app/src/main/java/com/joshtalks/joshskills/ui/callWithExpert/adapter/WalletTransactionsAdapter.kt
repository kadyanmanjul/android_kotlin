package com.joshtalks.joshskills.ui.callWithExpert.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.ItemWalletTransactionBinding
import com.joshtalks.joshskills.ui.callWithExpert.model.Transaction

class WalletTransactionsAdapter(var items: List<Transaction> = listOf()):RecyclerView.Adapter<WalletTransactionsAdapter.TransactionViewHolder>() {
    inner class TransactionViewHolder(val itemBinding: ItemWalletTransactionBinding):RecyclerView.ViewHolder(itemBinding.root){
        fun bind(item: Transaction){
            with(itemBinding){
                itemBinding.item = item
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletTransactionsAdapter.TransactionViewHolder {
        val binding = ItemWalletTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    fun addTransactionToList(members: List<Transaction>) {
        items = members
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: WalletTransactionsAdapter.TransactionViewHolder, position: Int) {
        holder.bind(items[position])
    }
}