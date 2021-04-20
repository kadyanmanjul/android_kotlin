package com.joshtalks.joshskills.core.bd

import android.widget.EditText
import androidx.databinding.BindingAdapter

object AppBindingAdapter {

    @BindingAdapter(value = ["setValue"], requireAll = false)
    @JvmStatic
    fun setValue(view: EditText, str: String?) {
        if (view.text.toString() != str) {
            view.setText(str)
            view.setSelection(view.text.length)
        }
    }
}
