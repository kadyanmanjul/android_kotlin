package com.joshtalks.joshskills.ui.inbox

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R

@BindingAdapter("substringTextColor")
fun TextView.setColorize(subStringToColorize: String) {
    val spannable: Spannable = SpannableString(text)
    spannable.setSpan(
        ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorPrimary)),
        7,
        31,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    spannable.setSpan(
        StyleSpan(Typeface.BOLD),
        7,
        31,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    setText(spannable, TextView.BufferType.SPANNABLE)
}

@BindingAdapter("isProgressVisible")
fun progressVisibility(textView: MaterialTextView, isProgressVisible: Boolean) {
    if (isProgressVisible) {
        textView.showProgress {
            gravity = DrawableButton.GRAVITY_CENTER
            progressRadiusRes = R.dimen.dp8
            progressStrokeRes = R.dimen.dp2
            textMarginRes = R.dimen.dp8
            progressColorRes = R.color.white
        }
        textView.isEnabled = false
    } else {
        textView.isEnabled = true
        textView.hideProgress(R.string.extend_free_trial_btn_text)
    }
}