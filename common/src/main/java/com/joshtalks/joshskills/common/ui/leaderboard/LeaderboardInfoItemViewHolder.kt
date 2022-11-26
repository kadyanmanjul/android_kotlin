package com.joshtalks.joshskills.common.ui.leaderboard

import android.content.Context
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.common.R
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

private const val TAG = "LeaderboardInfoItemView"
class LeaderboardInfoItemViewHolder(
    var text: String,
    var context: Context,
    val type: String
) {

    
    lateinit var textView: AppCompatTextView

    @Resolve
    fun onViewInflated() {
        textView.text = text
        Log.d(TAG, "onViewInflated: $text")
    }

}
