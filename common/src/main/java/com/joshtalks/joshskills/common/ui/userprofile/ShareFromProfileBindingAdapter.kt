package com.joshtalks.joshskills.ui.userprofile.adapter

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.databinding.BindingAdapter

@BindingAdapter("countTextColor")
fun setColorize(view:TextView,subStringToColorize: String) {
    if(subStringToColorize != "0"){
        val spannable: Spannable = SpannableString(view.text)
        spannable.setSpan(
            ForegroundColorSpan( Color.parseColor("#E58638")),
            16,
            16 + subStringToColorize.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        view.setText(spannable, TextView.BufferType.SPANNABLE)
    }
}