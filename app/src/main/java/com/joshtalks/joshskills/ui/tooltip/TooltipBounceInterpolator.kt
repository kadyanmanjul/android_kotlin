package com.joshtalks.joshskills.ui.tooltip

import android.view.animation.Interpolator

internal class TooltipBounceInterpolator(val amplitude: Double = 1.0, val frequency: Double = 10.0) : Interpolator {

    override fun getInterpolation(time: Float): Float {
        return (-1 * Math.pow(
            Math.E,
            -time / amplitude
        ) * Math.cos(frequency * time) + 1).toFloat()
    }
}