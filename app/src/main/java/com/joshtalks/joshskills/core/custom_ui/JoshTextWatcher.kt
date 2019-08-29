package com.joshtalks.joshskills.core.custom_ui

import android.text.Editable
import android.text.TextWatcher


abstract class JoshTextWatcher : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

    }

    override fun afterTextChanged(s: Editable) {

    }
}
