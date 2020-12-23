package com.joshtalks.joshskills.ui.voip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.HashMap


class VoipCallingViewModel(application: Application) : AndroidViewModel(application) {
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()


    fun getUserForTalk(courseId: String, topicId: Int?, aFunction: (String, String, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestParams: HashMap<String, String> = HashMap()
                requestParams["mentor_id"] = Mentor.getInstance().getId()
                requestParams["course_id"] = courseId
                requestParams["topic_id"] = topicId?.toString() ?: ""
                val response =
                    AppObjectController.p2pNetworkService.getAgoraClientToken(requestParams)
                if (response.isSuccessful && response.code() in 200..204) {
                    response.body()?.let {
                        aFunction.invoke(
                            it["token"]!!,
                            it["channel_name"]!!,
                            it["uid"]!!.toInt()
                        )
                    }
                } else {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                }
            } catch (ex: Throwable) {
                apiCallStatusLiveData.postValue(ApiCallStatus.FAILED_PERMANENT)
                ex.printStackTrace()
            }
        }
    }

}
