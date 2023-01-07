package com.joshtalks.joshskills.common.util

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshskills.common.R

/**
This class is a custom view to show remaining time in ending free trial.
 */

class TrialTimerView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    private var trialEndsTv: TextView
    private var endingHour: TextView
    private var endingMinutes: TextView
    private var endingSeconds: TextView
    private var timerTexts: Group
    private var timerRoot: ConstraintLayout

    init {
        inflate(context, R.layout.trial_timer, this).apply {
            trialEndsTv = findViewById(R.id.freeTrialEndsIn)
            timerRoot = findViewById(R.id.timerRoot)
            endingHour = findViewById(R.id.endingHour)
            endingMinutes = findViewById(R.id.endingMinutes)
            endingSeconds = findViewById(R.id.endingSeconds)
            timerTexts = findViewById(R.id.timerTexts)
        }
    }

    fun setTimer(hours: String, minutes: String, seconds: String) {
            timerRoot.visible()
            timerTexts.visible()
            trialEndsTv.text = context.getString(R.string.free_trial_ends_in)
            endingHour.text = hours
            endingMinutes.text = minutes
            endingSeconds.text = seconds
    }

    fun startTimer(timeInMillis: Long) {
        setTimer(
            UtilTime.getRemainingHours(timeInMillis),
            UtilTime.getRemainingMinutes(timeInMillis),
            UtilTime.getRemainingSeconds(timeInMillis)
        )
    }

    fun endFreeTrial() {
        Log.d("sagar", "endFreeTrial() called")
            timerTexts.gone()
            trialEndsTv.text = context.getString(R.string.free_trial_ended)
    }

    fun removeTimer() {
        timerRoot.gone()
    }

}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}