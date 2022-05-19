package com.joshtalks.joshskills.ui.cohort_based_course.viewmodels

import android.os.Message
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.ui.cohort_based_course.utils.OPEN_PROMISE_FRAGMENT
import com.joshtalks.joshskills.ui.cohort_based_course.utils.OPEN_SCHEDULE_FRAGMENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommitmentFormViewModel :ViewModel() {

    private var singleLiveEvent = EventLiveData

    fun openPromiseFragment(v: View){
        sendEvent(OPEN_PROMISE_FRAGMENT)
    }

    fun openScheduleFragment(v: View){
        sendEvent(OPEN_SCHEDULE_FRAGMENT)
    }

    private fun sendEvent(fragment: Int) {
        val msg = Message.obtain().apply {
            what = fragment
        }
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                singleLiveEvent.value = msg
            }
        }
    }
}