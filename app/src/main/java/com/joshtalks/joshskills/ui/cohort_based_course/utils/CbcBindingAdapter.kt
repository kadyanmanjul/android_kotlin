package com.joshtalks.joshskills.ui.cohort_based_course.utils

import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.databinding.BindingAdapter
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.TAG

@BindingAdapter("setTextAdapter")
fun AutoCompleteTextView.setTextAdapter(a:String) {
    Log.d(TAG, "setTextAdapter: 1")
    val option = resources.getStringArray(R.array.form_options)
    val arrayAdapter = ArrayAdapter(context, R.layout.dropdown_item, option)
    this.setAdapter(arrayAdapter)
}

@BindingAdapter("setButtonBackground")
fun MaterialButton.setBackgroundState(boolean: Boolean){
    when(boolean){
        true->{
            this.isEnabled = true
        }
        false->{
            this.isEnabled = false
        }
    }
}