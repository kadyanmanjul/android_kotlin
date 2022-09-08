package com.joshtalks.joshskills.ui.callWithExpert.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ItemWalletPaymentLogBinding
import com.joshtalks.joshskills.ui.callWithExpert.model.WalletLogs
import com.moengage.core.internal.utils.MoEUtils.getSystemService

class WalletLogsAdapter(var items: List<WalletLogs> = listOf()):RecyclerView.Adapter<WalletLogsAdapter.LogsViewHolder>() {
    inner class LogsViewHolder(val itemBinding: ItemWalletPaymentLogBinding):RecyclerView.ViewHolder(itemBinding.root){
        fun bind(item: WalletLogs){
            with(itemBinding){
                itemBinding.item = item
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletLogsAdapter.LogsViewHolder {
        val binding = ItemWalletPaymentLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogsViewHolder(binding)
    }

    fun addWalletLogsToList(members: List<WalletLogs>) {
        items = members
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: WalletLogsAdapter.LogsViewHolder, position: Int) {
        holder.bind(items[position])
        holder.itemBinding.imgCopy.setOnClickListener {
            val clipboardManager = getSystemService(holder.itemView.context,CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(
                "josh_transaction_id",
                holder.itemBinding.txtVCrdId.text
            )
            clipboardManager.setPrimaryClip(clipData)
            showToast("Copied to Clipboard")
        }
    }
}