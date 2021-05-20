package com.joshtalks.joshskills.ui.leaderboard

import android.content.Context
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.leaderboard_info_item)
class LeaderboardInfoItemViewHolder(
    var text: String,
    var context: Context,
    val type: String
) {

    @View(R.id.info_text)
    lateinit var textView: AppCompatTextView

    @Resolve
    fun onViewInflated() {
        textView.text = text
    }

}
