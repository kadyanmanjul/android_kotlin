package com.joshtalks.joshskills.ui.callWithExpert.adapter

import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.datetimeutils.DateTimeUtils
import com.joshtalks.joshskills.ui.callWithExpert.model.WalletLogs
import com.joshtalks.joshskills.ui.callWithExpert.utils.toPlusRupees
import java.text.SimpleDateFormat

@BindingAdapter(value = ["setAmountAdded"], requireAll = false)
fun setTextFromInt(view:TextView,item:WalletLogs){
    view.text = item.amount.toString().toPlusRupees()
}

@BindingAdapter(value = ["setPaymentStatus"], requireAll = false)
fun setTextFromBool(view:TextView,item:WalletLogs){
    if (item.is_failed){
        view.text = "FAILED"
        view.setTextColor(ContextCompat.getColor(view.context,R.color.txt_transaction_failed_color))
    }else{
        view.text = "SUCCESS"
    }
}

@BindingAdapter(value = ["setDateFromMills"], requireAll = false)
fun setTextFromLong(view:TextView,item:WalletLogs){
    val date  = DateTimeUtils.formatDate(item.created)
    view.text = SimpleDateFormat("d MMM y, K:mm a").format(date)
}