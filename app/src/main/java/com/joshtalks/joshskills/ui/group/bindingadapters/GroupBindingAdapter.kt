package com.joshtalks.joshskills.ui.group.bindingadapters

import android.util.Log
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import com.joshtalks.joshskills.ui.group.views.GroupsAppBar

@BindingAdapter("onBackPressed")
fun GroupsAppBar.onBackPress(function : () -> Unit) = this.onBackPressed(function)

@BindingAdapter("onFirstIconPressed")
fun GroupsAppBar.onFirstIconPress(function : () -> Unit) = this.onBackPressed(function)

@BindingAdapter("onSecondIconPressed")
fun GroupsAppBar.onSecondIconPress(function : () -> Unit) = this.onBackPressed(function)