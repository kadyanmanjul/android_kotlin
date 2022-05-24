package com.joshtalks.joshskills.ui.cohort_based_course.viewmodels

import android.os.Message
import android.view.View
import androidx.databinding.ObservableArrayList
import android.widget.Toast
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.constants.CLOSE_ACTIVITY
import com.joshtalks.joshskills.constants.OPEN_PROMISE_FRAGMENT
import com.joshtalks.joshskills.constants.OPEN_SCHEDULE_FRAGMENT
import com.joshtalks.joshskills.constants.START_CONVERSATION_ACTIVITY
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.cohort_based_course.models.CohortItemModel
import com.joshtalks.joshskills.ui.inbox.OPEN_CONVERSATION_ACTIVITY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "CommitmentFormViewModel"

class CommitmentFormViewModel : ViewModel() {

    private val singleLiveEvent = EventLiveData
    private var reminder: String = "Yes"
    val shapath = ObservableField("Yes")
    val selectedSlot = ObservableField("")
    val cohortBatchList = ObservableArrayList<CohortItemModel>()
    val userName = ObservableField(EMPTY)

    init {
        getCohortBatches()
        getUsername()
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

    fun getCohortBatches() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = AppObjectController.CbcNetworkService.getCohortBatches()
                withContext(Dispatchers.Main) {
                    cohortBatchList.addAll(resp.slots)
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
                showToast("Something Went Wrong, Please try again later!", Toast.LENGTH_LONG)
                sendEvent(CLOSE_ACTIVITY)
            }
        }
    }

    fun postSelectedBatch(map: HashMap<String, Any>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = AppObjectController.CbcNetworkService.postSelectedBatch(map)
                if (resp.isSuccessful) {
                    sendEvent(START_CONVERSATION_ACTIVITY)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                showToast("Something Went Wrong, Please try again later!", Toast.LENGTH_LONG)
                sendEvent(CLOSE_ACTIVITY)
            }
        }
    }

    fun sendBatchSelected(v: View) {
        if (selectedSlot.get() != EMPTY) {
            val map: HashMap<String, Any> = HashMap()
            map["time_slot"] = selectedSlot.get().toString()
            map["reminder"] = reminder == "Yes"
            postSelectedBatch(map)
        } else {
            showToast("Please select a slot")
        }
    }

    val setShapath = fun(selection: String) {
        shapath.set(selection)
    }

    val setReminder = fun(selection: String) {
        reminder = selection
    }

    val selectSlot: (item: String) -> Unit = {
        selectedSlot.set(it)
    }

    fun getUsername() {
        userName.set(User.getInstance().firstName ?: EMPTY)
    }
}