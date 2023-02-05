package com.joshtalks.joshskills.premium.ui.callWithExpert.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.core.showToast
import com.joshtalks.joshskills.premium.databinding.ItemWalletPaymentLogBinding
import com.joshtalks.joshskills.premium.ui.callWithExpert.model.WalletLogs

class WalletLogsAdapter():PagingDataAdapter<WalletLogs,WalletLogsAdapter.LogsViewHolder>(LogsDiffUtilCallbacks()) {
    inner class LogsViewHolder(val itemBinding: ItemWalletPaymentLogBinding):RecyclerView.ViewHolder(itemBinding.root){
        fun bind(item: WalletLogs){
            with(itemBinding){
                itemBinding.item = item
                itemBinding.imgCopy.setOnClickListener {
                    val clipboardManager =  AppObjectController.joshApplication.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText(
                        itemView.context.getString(R.string.josh_transaction_id),
                        itemBinding.txtVCrdId.text
                    )
                    clipboardManager.setPrimaryClip(clipData)
                    showToast(itemView.context.getString(R.string.copied_to_clipboard))
                }
            }
        }
    }

    class LogsDiffUtilCallbacks : DiffUtil.ItemCallback<WalletLogs>() {
        override fun areItemsTheSame(oldItem: WalletLogs, newItem: WalletLogs): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: WalletLogs, newItem: WalletLogs): Boolean {
            return oldItem == newItem
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletLogsAdapter.LogsViewHolder {
        val binding = ItemWalletPaymentLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WalletLogsAdapter.LogsViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item)
        }
    }
}