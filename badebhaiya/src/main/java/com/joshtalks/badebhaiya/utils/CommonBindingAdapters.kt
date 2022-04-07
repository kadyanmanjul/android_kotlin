package com.joshtalks.badebhaiya.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import androidx.databinding.BindingAdapter

@BindingAdapter("textWatcher")
fun setTextWatcher(button: Button, editText: EditText) {
    editText.addTextChangedListener(object: TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            button.isEnabled = editText.text.toString().isNotEmpty()
        }

        override fun afterTextChanged(p0: Editable?) {

        }
    })
}