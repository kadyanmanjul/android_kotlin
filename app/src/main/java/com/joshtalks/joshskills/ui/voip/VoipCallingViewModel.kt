package com.joshtalks.joshskills.ui.voip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.server.voip.VoipCallDetailModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.ProtocolException
import java.util.HashMap
import java.util.UUID

const val RETRY_MIN_COUNT = 6

class VoipCallingViewModel(application: Application) : AndroidViewModel(application) {
    val voipDetailsLiveData: MutableLiveData<VoipCallDetailModel> = MutableLiveData()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    private var attemptCount = 0
    private var supportUser = "false"
    private var attemptedMentorListCall = mutableSetOf<String>()
    private var sessionId = UUID.randomUUID().toString()

    fun getUserForTalk(courseId: String, topicId: Int?) {
        if (attemptCount > RETRY_MIN_COUNT) {
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED_PERMANENT)
            return
        }
        attemptCount += 1
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (attemptCount == RETRY_MIN_COUNT - 1) {
                    supportUser = "true"
                }
                val params: HashMap<String, String?> = HashMap()
                params["course_id"] = courseId
                params["topic_id"] = topicId?.toString()
                params["support_user"] = supportUser
                params["call_session_id"] = sessionId
                params["old_mentors_list"] = getAllRecentMentorCall()
                val response = AppObjectController.commonNetworkService.getP2PUser(params)
                voipDetailsLiveData.postValue(response)
                attemptedMentorListCall.add(response.mentorId ?: EMPTY)
            } catch (ex: ProtocolException) {
                apiCallStatusLiveData.postValue(ApiCallStatus.RETRY)
            } catch (ex: Throwable) {
                apiCallStatusLiveData.postValue(ApiCallStatus.RETRY)
                voipDetailsLiveData.postValue(null)
            }
        }
    }

    private fun getAllRecentMentorCall(): String {
        if (attemptedMentorListCall.size == 0) {
            return EMPTY
        }
        val resp = StringBuilder()
        attemptedMentorListCall.forEach {
            resp.append(it).append(",")
        }
        return resp.toString().substring(0, resp.toString().length - 1)
    }
}
