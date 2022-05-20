package com.joshtalks.joshskills.ui.cohort_based_course.viewmodels

import android.os.Message
import android.util.Log
import android.view.View
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.constants.OPEN_PROMISE_FRAGMENT
import com.joshtalks.joshskills.constants.OPEN_SCHEDULE_FRAGMENT
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.cohort_based_course.models.CohortItemModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "CommitmentFormViewModel"
class CommitmentFormViewModel : ViewModel() {

    private var singleLiveEvent = EventLiveData
    private var reminder : String = "Yes"
    val shapath = ObservableField("Yes")
    private var selectedSlot = ObservableField("")

    var cohortBatchList :ArrayList<CohortItemModel>? = ArrayList()
    var userName = ObservableField(EMPTY)

    init {
        getCohortBatches()
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

    fun getCohortBatches(){
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    cohortBatchList =
                        AppObjectController.CbcNetworkService.getCohortBatches()
                            .body() as ArrayList<CohortItemModel>

                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
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

    fun sendBatchSelected(v:View){
        if (selectedSlot.get() != EMPTY){
            val  map: HashMap<String,Any> = HashMap()
            map["time_slot"] = selectedSlot.get().toString()
            map["reminder"] = reminder
            postSelectedBatch(map)
        }else{
            showToast("Please select a slot")
        }
    }
    val setShapath = fun(selection:String){
        Log.d(TAG, "setShapath: 1 $selection")
        shapath.set(selection)
    }

    val setReminder = fun(selection:String){
        Log.d(TAG, "setShapath: 2 $selection")
        reminder = selection

    }

    val selectSlot = fun(slot:String){
        Log.d(TAG, "setShapath: 3 $slot")
        selectedSlot.set(slot)
    }
}