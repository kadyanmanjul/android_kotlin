package com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.util

import androidx.databinding.BindingAdapter

@BindingAdapter("onCallBarClick")
fun CallBarLayout.onCallBarClick(function : ()->Unit)= this.onCallBarClick(function)