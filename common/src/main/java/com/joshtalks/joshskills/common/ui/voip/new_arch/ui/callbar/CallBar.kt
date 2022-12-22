package com.joshtalks.joshskills.common.ui.voip.new_arch.ui.callbar

import androidx.lifecycle.LiveData
import com.joshtalks.joshskills.common.ui.voip.local.VoipPrefListener

/**
 * Require DataBinding in targeted xml with following instruction ->
 * 1. import @View
 * 2. add variable @callBar of type CallBar class
 * 3. add @CallBarLayout in xml with required attributes
 * 4. use binding adapters setters @onCallBarClick (callBar::intentToCallActivity)
 * 5. toggle visibility using @isCallONGoing ObservableBoolean
 *  Required to set Variable @callBar in Activity as "binding.callBar= CallBar(this)"
 */

private const val TAG = "CallBar"

class CallBar {
    val prefListener by lazy { VoipPrefListener }

    fun getTimerLiveData(): LiveData<Long> {
        return prefListener.observerStartTime()
    }
}