package com.joshtalks.joshskills.expertcall.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.showToast
import com.joshtalks.joshskills.expertcall.databinding.ItemWalletPaymentLogBinding
import com.joshtalks.joshskills.expertcall.model.WalletLogs

class WalletLogsAdapter():PagingDataAdapter<WalletLogs, WalletLogsAdapter.LogsViewHolder>(
    LogsDiffUtilCallbacks()
) {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogsViewHolder {
        val binding = ItemWalletPaymentLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogsViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item)
        }
    }
}