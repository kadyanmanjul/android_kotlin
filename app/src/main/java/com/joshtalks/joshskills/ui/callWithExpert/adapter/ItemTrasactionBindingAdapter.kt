package com.joshtalks.joshskills.ui.callWithExpert.adapter

import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.datetimeutils.DateTimeUtils
import com.joshtalks.joshskills.ui.callWithExpert.model.Transaction
import com.joshtalks.joshskills.ui.callWithExpert.utils.removeNegative
import com.joshtalks.joshskills.ui.callWithExpert.utils.toMinusRupees
import com.joshtalks.joshskills.ui.callWithExpert.utils.toPlusRupees
import java.text.SimpleDateFormat

@BindingAdapter(value = ["setDateFromMills"], requireAll = false)
fun setTimeFromLong(view: TextView, item: Transaction){
    val date  = DateTimeUtils.formatDate(item.created)
    view.text = SimpleDateFormat("d MMM y, K:mm a").format(date)
}

@BindingAdapter(value = ["setAmountDeductedOrAdded"], requireAll = false)
fun setDeductedFromInt(view:TextView,item: Transaction){
    if (item.amount > 0){
        view.text = item.amount.toString().toPlusRupees()
        view.setTextColor(ContextCompat.getColor(view.context,R.color.success))
    }else{
        view.text = item.amount.toString().removeNegative().toMinusRupees()
        view.setTextColor(ContextCompat.getColor(view.context,R.color.critical))
    }
}