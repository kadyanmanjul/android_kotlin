package com.joshtalks.joshskills.expertcall.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.expertcall.databinding.ItemWalletTransactionBinding
import com.joshtalks.joshskills.expertcall.model.Transaction

class WalletTransactionsAdapter(): PagingDataAdapter<Transaction, WalletTransactionsAdapter.TransactionViewHolder>(
    TransactionDiffUtilsCallbacks()
) {
    inner class TransactionViewHolder(val itemBinding: ItemWalletTransactionBinding):RecyclerView.ViewHolder(itemBinding.root){
        fun bind(item: Transaction){
            with(itemBinding){
                itemBinding.item = item
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemWalletTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
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
