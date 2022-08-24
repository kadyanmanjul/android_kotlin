package com.joshtalks.joshskills.ui.callWithExpert.adapter

import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.ui.callWithExpert.model.ExpertListModel

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
            imageView.setUserImageOrInitials(it.expertImage, it.expertName?:"", isRound = true)
        }catch (e:Exception){
            imageView.setImageResource(R.drawable.ic_call_placeholder)
            e.printStackTrace()
        }
    } ?: imageView.setImageResource(R.drawable.ic_call_placeholder)
}

@BindingAdapter(value = ["setPricePerMinute"], requireAll = false)
fun setPricePerMinute(view: AppCompatTextView, str: ExpertListModel) {
        view.text = "\u20B9"+str.expertPricePerMinute.toString().plus("/min")
}

@BindingAdapter(value = ["setExperienceYears"], requireAll = false)
fun setExperienceYears(view: AppCompatTextView, str: ExpertListModel) {
    view.text = "Exp: "+str.expertExperience.toString().plus("/Years")
}
