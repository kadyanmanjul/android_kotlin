package com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.util

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Chronometer
import android.widget.FrameLayout
import androidx.databinding.Observable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import com.google.firebase.firestore.OnProgressListener
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.constants.FROM_CALL_BAR
import com.joshtalks.joshskills.base.constants.STARTING_POINT
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.CallBar
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.VoipPref
import com.joshtalks.joshskills.ui.voip.new_arch.ui.utils.startTimer
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity
import java.lang.Exception

private const val TAG = "CallBarLayout"
class CallBarLayout @JvmOverloads
constructor(context: Context, attributes: AttributeSet? = null) : FrameLayout(context, attributes) {
    private val callBarContainer: FrameLayout by lazy {
        this.findViewById(R.id.ongoing_call_bar)
    }

    private val callTimer: Chronometer by lazy { this.findViewById(R.id.call_timer) }
    private val callBar = CallBar()

    init {
        try {
            View.inflate(getContext(), R.layout.call_bar_layout, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onCallBarClick()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, "onAttachedToWindow: ")
        observeStartTime()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(TAG, "onDetachedFromWindow: ")
        stopObservingStartTime()
    }

    fun onCallBarClick() {
        callBarContainer.setOnClickListener {
            val intent = Intent(context,VoiceCallActivity::class.java).apply {
                putExtra(STARTING_POINT, FROM_CALL_BAR)
            }
            context.startActivity(intent)
        }
    }

    private fun timerObserver(startTime : Long) {
        Log.d(TAG, "timerObserver: $startTime")
        if(startTime == 0L) {
            callBarContainer.visibility = GONE
            callTimer.stop()
            return
        } else {
            callBarContainer.visibility = VISIBLE
            callTimer.stop()
            callTimer.base = startTime
            callTimer.start()
        }
    }

    fun observeStartTime() {
        callBar.getTimerLiveData().observeForever(::timerObserver)
    }

    fun stopObservingStartTime() {
        callBar.getTimerLiveData().removeObserver(::timerObserver)
    }
}