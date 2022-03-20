package com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.util

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Chronometer
import android.widget.FrameLayout
import com.joshtalks.joshskills.R
import java.lang.Exception

class CallBarLayout @JvmOverloads
constructor(context: Context, attributes: AttributeSet? = null) : FrameLayout(context, attributes) {
    private val callBarContainer: FrameLayout by lazy {
        this.findViewById(R.id.ongoing_call_bar)
    }

    private val callTimer: Chronometer by lazy {
        this.findViewById(R.id.call_timer)
    }

    init {
        try {
            View.inflate(getContext(), R.layout.call_bar_layout, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onCallBarClick(function: () -> Unit) {
        callBarContainer.setOnClickListener {
            function.invoke()
        }
    }

    fun startCallTimer(function: () -> Unit) {
        callTimer.start()
    }
}