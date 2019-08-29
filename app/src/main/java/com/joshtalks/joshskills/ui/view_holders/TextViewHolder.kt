package com.joshtalks.joshskills.ui.view_holders

import android.content.Context
import com.joshtalks.joshskills.R
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Position
import com.mindorks.placeholderview.annotations.Resolve


@Layout(R.layout.chat_text_message_holder)
class TextViewHolder(private val context: Context){



    @JvmField
    @Position
    var position: Int = 0;


    @Resolve
    fun onResolved() {

    }
}