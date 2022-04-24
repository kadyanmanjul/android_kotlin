package com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar

import androidx.lifecycle.LiveData
import com.joshtalks.joshskills.base.model.VoipUIState
import com.joshtalks.joshskills.ui.call.data.local.VoipPrefListener
import kotlinx.coroutines.flow.StateFlow

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

    // TODO: Can be removed
    fun observerVoipState(): LiveData<Int> {
        return prefListener.observerVoipState()
    }
}