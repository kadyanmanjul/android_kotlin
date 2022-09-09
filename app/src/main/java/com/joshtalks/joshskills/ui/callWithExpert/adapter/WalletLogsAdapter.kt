package com.joshtalks.joshskills.ui.callWithExpert.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ItemWalletPaymentLogBinding
import com.joshtalks.joshskills.ui.callWithExpert.model.WalletLogs
import com.moengage.core.internal.utils.MoEUtils.getSystemService

class WalletLogsAdapter():PagingDataAdapter<WalletLogs,WalletLogsAdapter.LogsViewHolder>(LogsDiffUtilCallbacks()) {
    inner class LogsViewHolder(val itemBinding: ItemWalletPaymentLogBinding):RecyclerView.ViewHolder(itemBinding.root){
        fun bind(item: WalletLogs){
            with(itemBinding){
                itemBinding.item = item
                itemBinding.imgCopy.setOnClickListener {
                    val clipboardManager = getSystemService(itemView.context,CLIPBOARD_SERVICE) as ClipboardManager
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