package com.joshtalks.joshskills.ui.cohort_based_course.viewmodels

import android.os.Message
import android.view.View
import android.widget.Toast
import androidx.databinding.ObservableArrayList
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
import com.joshtalks.joshskills.repository.server.reminder.ReminderRequest
import com.joshtalks.joshskills.repository.server.reminder.ReminderResponse
import com.joshtalks.joshskills.ui.cohort_based_course.models.CohortItemModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KFunction3

private const val TAG = "CommitmentFormViewModel"

class CommitmentFormViewModel : ViewModel() {

    private val singleLiveEvent = EventLiveData
    private var reminder: String = "Yes"
    val shapath = ObservableField("Yes")
    val selectedSlot = ObservableField<CohortItemModel>()
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
                val resp = AppObjectController.CbcNetworkService.getCohortBatches().body()
                withContext(Dispatchers.Main) {
                    cohortBatchList.addAll(resp?.slots as ArrayList<CohortItemModel>)
                }
                //throw Exception("Problem!")    to test the try catch block

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
                } else {
                    showToast("Something Went Wrong, Please try again later!", Toast.LENGTH_LONG)
                    sendEvent(CLOSE_ACTIVITY)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                showToast("Something Went Wrong, Please try again later!", Toast.LENGTH_LONG)
                sendEvent(CLOSE_ACTIVITY)
            }
        }
    }

    fun sendBatchSelected(v: View) {
        if (selectedSlot.get() != null) {
            val map: HashMap<String, Any> = HashMap()
            map["time_slot"] = selectedSlot.get()!!.name.toString()
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

    val selectSlot: (item: CohortItemModel) -> Unit = {
        selectedSlot.set(it)
    }

    fun getUsername() {
        userName.set(User.getInstance().firstName ?: EMPTY)
    }

   /* fun submitReminder(
        reminderId: Int,
        frequency: String,
        status: String,
        mentorId: String,
        previousTime: String = EMPTY,
        onAlarmSetSuccess: KFunction3<Int, Int, Int, Unit>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                (selectedSlot.get()?.timeSlot)?.split(" -")?.get(0)?.trim()?.let {
                    val calendar = Calendar.getInstance()
                    calendar.time =
                        SimpleDateFormat("hh:mm aa", Locale.getDefault()).parse(it) as Date
                    calendar.add(Calendar.MINUTE, -2)
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val minute = calendar.get(Calendar.MINUTE)
                    val startTime =
                        "${if (hour < 10) "0" else ""}${hour}:${if (minute < 10) "0" else ""}${minute}:00"
                    val response = AppObjectController.commonNetworkService.setReminder(
                        ReminderRequest(
                            mentorId,
                            startTime,
                            frequency,
                            status,
                            previousTime
                        )
                    )
                    if (response.isSuccessful) {
                        response.body()?.let { response1 ->
                            if (response1.success) {
                                val id: Int =
                                    if (previousTime.isBlank()) response1.responseData else reminderId
                                onAlarmSetSuccess.invoke(id, hour, minute)
                                AppObjectController.appDatabase.reminderDao().insertReminder(
                                    ReminderResponse(
                                        id, mentorId, frequency, status, startTime
                                    )
                                )
                            } else {
                                showToast(response1.message)
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }*/
}