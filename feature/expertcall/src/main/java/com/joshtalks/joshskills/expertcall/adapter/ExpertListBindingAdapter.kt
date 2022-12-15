package com.joshtalks.joshskills.expertcall.adapter

import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.expertcall.R
import com.joshtalks.joshskills.common.core.datetimeutils.DateTimeUtils
import com.joshtalks.joshskills.common.core.setUserImageOrInitials
import com.joshtalks.joshskills.expertcall.model.ExpertListModel
import com.joshtalks.joshskills.expertcall.model.Transaction
import com.joshtalks.joshskills.expertcall.model.WalletLogs
import com.joshtalks.joshskills.expertcall.utils.removeNegative
import com.joshtalks.joshskills.expertcall.utils.toMinusRupees
import com.joshtalks.joshskills.expertcall.utils.toPlusRupees
import java.text.SimpleDateFormat

@BindingAdapter("expertListAdapter", "onExpertItemClick")
fun expertListAdapter(
    view: RecyclerView,
    adapter: ExpertListAdapter,
    function: ((ExpertListModel, Int, Int) -> Unit)?
) {
    view.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
    view.setHasFixedSize(false)
    view.adapter = adapter

    adapter.setItemClickFunction(function)
}

@BindingAdapter(value = ["expertImage"], requireAll = false)
fun expertImage(imageView: ImageView, caller: ExpertListModel?) {
    caller?.let {
        try {
            imageView.setUserImageOrInitials(it.expertImage, it.expertName ?: "", 20, isRound = true)
        } catch (e: Exception) {
            imageView.setImageResource(R.drawable.ic_call_placeholder)
            e.printStackTrace()
        }
    } ?: imageView.setImageResource(R.drawable.ic_call_placeholder)
}

@BindingAdapter(value = ["setPricePerMinute"], requireAll = false)
fun setPricePerMinute(view: AppCompatTextView, str: ExpertListModel) {
    view.text = "\u20B9" + str.expertPricePerMinute.toString().plus("/min")
}

@BindingAdapter(value = ["setDateFromMills"], requireAll = false)
fun setTimeFromLong(view: TextView, item: Transaction){
    val date  = DateTimeUtils.formatDate(item.created)
    view.text = SimpleDateFormat("d MMM y, K:mm a").format(date)
}

@BindingAdapter(value = ["setAmountDeductedOrAdded"], requireAll = false)
fun setDeductedFromInt(view: TextView, item: Transaction){
    if (item.amount > 0){
        view.text = item.amount.toString().toPlusRupees()
        view.setTextColor(ContextCompat.getColor(view.context,R.color.success))
    }else{
        view.text = item.amount.toString().removeNegative().toMinusRupees()
        view.setTextColor(ContextCompat.getColor(view.context,R.color.critical))
    }
}

@BindingAdapter(value = ["setAmountAdded"], requireAll = false)
fun setTextFromInt(view:TextView,item: WalletLogs){
    view.text = item.amount.toString().toPlusRupees()
}

@BindingAdapter(value = ["setPaymentStatus"], requireAll = false)
fun setTextFromBool(view:TextView,item: WalletLogs){
    if (item.is_failed){
        view.text = "FAILED"
        view.setTextColor(ContextCompat.getColor(view.context,R.color.critical))
    }else{
        view.text = "SUCCESS"
    }
}

@BindingAdapter(value = ["setDateFromMills"], requireAll = false)
fun setTextFromLong(view:TextView,item: WalletLogs){
    val date  = DateTimeUtils.formatDate(item.created)
    view.text = SimpleDateFormat("d MMM y, K:mm a").format(date)
}