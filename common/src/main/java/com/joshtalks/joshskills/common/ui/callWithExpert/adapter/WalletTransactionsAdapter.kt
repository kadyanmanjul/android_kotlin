package com.joshtalks.joshskills.common.ui.callWithExpert.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.common.databinding.ItemWalletTransactionBinding
import com.joshtalks.joshskills.common.ui.callWithExpert.model.Transaction

class WalletTransactionsAdapter(): PagingDataAdapter<Transaction,WalletTransactionsAdapter.TransactionViewHolder>(TransactionDiffUtilsCallbacks()) {
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

    override fun onBindViewHolder(holder: WalletTransactionsAdapter.TransactionViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item)
        }
    }
}

class TransactionDiffUtilsCallbacks : DiffUtil.ItemCallback<Transaction>() {
    override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem == newItem
    }

}
