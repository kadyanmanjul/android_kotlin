package com.joshtalks.joshskills.ui.inbox.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ItemTransactionsBinding
import com.joshtalks.joshskills.ui.inbox.model.TransactionHistory
import java.text.SimpleDateFormat

class TransactionAdapter(val transactions: List<TransactionHistory>) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(transactions[position])
    }

    override fun getItemCount() = transactions.size

    class ViewHolder(val item: ItemTransactionsBinding) : RecyclerView.ViewHolder(item.root) {
        fun bindData(data: TransactionHistory) {
            item.courseTitle.text = data.courseName
            item.transAmount.text = "₹${data.amount}"
            item.transPaymentId.text = data.paymentId
            item.transId.text = data.orderId.toString()

            val destFormat = SimpleDateFormat("dd MMMM yy, hh:mm aa")
            item.transTime.text = destFormat.format(data.getTransactionTime()).toString()

            when {
                data.paymentId == null && data.amount == 0F -> {
                    item.transStatus.text = "Successful"
                    item.transPaymentId.visibility = GONE
                }
                data.paymentId == null && data.status == null  -> {
                    item.transStatus.text = "Failed"
                    item.transStatusIcon.setImageResource(R.drawable.close_circle)
                    item.transPaymentId.visibility = GONE
                }
                data.status == null && data.paymentId.isNullOrBlank().not() -> {
                    item.transStatus.text = "Processing"
                    item.transStatusIcon.setImageResource(R.drawable.alert_circle)
                }
                data.status == false -> {
                    item.transStatus.text = "Failed"
                    item.transStatusIcon.setImageResource(R.drawable.close_circle)
                }
                data.status == true -> {
                    item.transStatus.text = "Successful"
                }
            }

            item.transId.setOnClickListener {
                val clipboardManager = it.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("Order ID:", data.orderId.toString())
                clipboardManager.setPrimaryClip(clipData)
                showToast("Order ID copied to clipboard!")
            }

            Glide.with(itemView)
                .load(data.courseIcon)
                .into(item.courseImage)
        }
    }
}

//TRANSACTION STATUS
/**
 ** ID = NULL & amt = 0          :   SUCCESS
 ** Status = NULL & ID == NULL   :   FAILED
 ** Status = NULL & ID != NULL   :   PENDING
 ** Status = False               :   FAILED
 ** Status = True                :   SUCCESS
 **/