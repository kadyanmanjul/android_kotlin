package com.joshtalks.joshskills.ui.callWithExpert.adapter

import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.datetimeutils.DateTimeUtils
import com.joshtalks.joshskills.ui.callWithExpert.model.Transaction
import com.joshtalks.joshskills.ui.callWithExpert.utils.removeNegative
import java.text.SimpleDateFormat

@BindingAdapter(value = ["setDateFromMills"], requireAll = false)
fun setTimeFromLong(view: TextView, item: Transaction){
    val date  = DateTimeUtils.formatDate(item.created)
    view.text = SimpleDateFormat("d MMM y, k:m a").format(date)
}

@BindingAdapter(value = ["setAmountDeductedOrAdded"], requireAll = false)
fun setDeductedFromInt(view:TextView,item: Transaction){
    if (item.amount > 0){
        view.text = "+₹ " + item.amount.toString()
        view.setTextColor(ContextCompat.getColor(view.context,R.color.txt_money_added))
    }else{
        view.text = "-₹ " + item.amount.toString().removeNegative()
        view.setTextColor(ContextCompat.getColor(view.context,R.color.txt_money_deducted))
    }
}