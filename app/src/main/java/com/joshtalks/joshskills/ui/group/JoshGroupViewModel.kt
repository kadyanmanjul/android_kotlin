package com.joshtalks.joshskills.ui.group

import android.util.Log
import androidx.databinding.ObservableBoolean
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED

private const val TAG = "JoshGroupViewModel"
class JoshGroupViewModel : BaseViewModel() {
    val hasGroups = ObservableBoolean(false)

    fun onBackPress() {
        message.what = ON_BACK_PRESSED
        singleLiveEvent.value = message
    }

}