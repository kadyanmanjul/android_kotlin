package com.joshtalks.joshskills.leaderboard

import android.content.Context
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView
import com.mindorks.placeholderview.annotations.Resolve

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
