package com.joshtalks.joshskills.ui.voip.new_arch.ui.report

import android.widget.Button
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableBoolean
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.ui.voip.new_arch.ui.report.adapter.ReportAdapter
import com.joshtalks.joshskills.ui.voip.new_arch.ui.report.model.OptionModel

@BindingAdapter("setIssueAdapter", "setBackgroundListener","setOptionListener", requireAll = false)
fun RecyclerView.setIssueAdapter(items: List<OptionModel>?, function1: ((Boolean) -> Unit)?,function2: ((Int) -> Unit)?) {
    val manager = FlexboxLayoutManager(context)
    manager.flexDirection = FlexDirection.ROW
    manager.justifyContent = JustifyContent.FLEX_START
    val adapter = items?.let { ReportAdapter(it, context) }
    this.adapter = adapter
    this.layoutManager = manager
    adapter?.setBackgroundListener(function1)
    adapter?.setOptionListener(function2)
}

@BindingAdapter( "submitBackground")
fun Button.submitBtnSetup(changeBackground: ObservableBoolean) {
    if (changeBackground.get()) {
        this.setBackgroundResource(R.drawable.rounded_state_button_bg)
        this.isEnabled = true
    }
}

