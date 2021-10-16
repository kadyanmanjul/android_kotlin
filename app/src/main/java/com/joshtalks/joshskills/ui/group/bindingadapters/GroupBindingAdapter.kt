package com.joshtalks.joshskills.ui.group.bindingadapters

import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter

@BindingAdapter("onBackPressed")
fun setOnBackPressed(view : AppCompatImageView, function : () -> Unit) = view.setOnClickListener {
    function.invoke()
}