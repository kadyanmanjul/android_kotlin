package com.joshtalks.joshskills.ui.callWithExpert.adapter

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.datetimeutils.DateTimeUtils
import com.joshtalks.joshskills.ui.callWithExpert.model.WalletLogs
import java.text.SimpleDateFormat

@BindingAdapter(value = ["setAmountAdded"], requireAll = false)
fun setTextFromInt(view:TextView,item:WalletLogs){
    view.text = "+₹ " + item.amount.toString()
}

@BindingAdapter(value = ["setPaymentStatus"], requireAll = false)
fun setTextFromBool(view:TextView,item:WalletLogs){
    if (item.is_failed){
        view.text = "FAILED"
        view.setTextColor(R.color.txt_money_deducted)
    }else{
        view.text = "SUCCESS"
    }
}

@BindingAdapter(value = ["setDateFromMills"], requireAll = false)
fun setTextFromLong(view:TextView,item:WalletLogs){
    val date  = DateTimeUtils.formatDate(item.created)
    view.text = SimpleDateFormat("d MMM y, k:m a").format(date)
}