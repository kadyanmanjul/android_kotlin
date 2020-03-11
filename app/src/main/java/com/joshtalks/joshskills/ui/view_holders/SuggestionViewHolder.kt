package com.joshtalks.joshskills.ui.view_holders

import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.TagMessageEventBus
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.suggestion_item_layout)
class SuggestionViewHolder(private var suggestion: String) {
    @View(R.id.text_view)
    lateinit var textView: AppCompatTextView

    @Resolve
    fun onInflateView() {
        textView.text = suggestion
    }

    @Click(R.id.text_view)
    fun onSelect() {
        textView.isSelected = true
        RxBus2.publish(TagMessageEventBus(suggestion))

    }
}
