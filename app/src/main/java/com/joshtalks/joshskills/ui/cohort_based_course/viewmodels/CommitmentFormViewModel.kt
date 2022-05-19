package com.joshtalks.joshskills.ui.cohort_based_course.viewmodels

import android.os.Message
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.constants.OPEN_PROMISE_FRAGMENT
import com.joshtalks.joshskills.constants.OPEN_SCHEDULE_FRAGMENT
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.TAG
import com.joshtalks.joshskills.ui.cohort_based_course.models.CohortItemModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommitmentFormViewModel : ViewModel() {

    private var singleLiveEvent = EventLiveData
    var reminder: String = "Yes"
    val cohortBatchList by lazy {
        getCohortBatches()
    }

    init {
        cohortBatchList
    }

    fun openPromiseFragment(v: View) {
        sendEvent(OPEN_PROMISE_FRAGMENT)
    }

    fun openScheduleFragment(v: View) {
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

    fun getCohortBatches(): ArrayList<CohortItemModel> {
        var resp: ArrayList<CohortItemModel> = ArrayList()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    resp =
                        AppObjectController.CbcNetworkService.getCohortBatches()
                            .body() as ArrayList<CohortItemModel>

                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
        return resp
    }

    fun postSelectedBatch(map: HashMap<String, Any>) {
        viewModelScope.launch {
            try {
                AppObjectController.CbcNetworkService.postSelectedBatch(map)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}