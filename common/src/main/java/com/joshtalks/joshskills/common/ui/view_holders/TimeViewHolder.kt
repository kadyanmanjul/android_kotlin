package com.joshtalks.joshskills.common.ui.view_holders

import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.custom_ui.custom_textview.JoshTextView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.util.*



class TimeViewHolder(var time: Date) : BaseCell() {

    
    lateinit var textView: JoshTextView


    @Resolve
    fun onViewInflated() {
        textView.text = Utils.dateHeaderDateFormat(time).toUpperCase(Locale.getDefault())
    }
}
