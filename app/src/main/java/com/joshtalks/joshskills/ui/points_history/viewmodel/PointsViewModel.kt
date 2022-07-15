package com.joshtalks.joshskills.ui.points_history.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CURRENT_COURSE_ID
import com.joshtalks.joshskills.core.DEFAULT_COURSE_ID
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.base.local.model.Mentor
import com.joshtalks.joshskills.repository.server.points.PointsHistoryResponse
import com.joshtalks.joshskills.repository.server.points.PointsInfoResponse
import com.joshtalks.joshskills.repository.server.points.SpokenMinutesHistoryResponse
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PointsViewModel(application: Application) : AndroidViewModel(application) {
    val pointsHistoryLiveData: MutableLiveData<PointsHistoryResponse> = MutableLiveData()
    val spokenHistoryLiveData: MutableLiveData<SpokenMinutesHistoryResponse> = MutableLiveData()
    val pointsInfoLiveData: MutableLiveData<PointsInfoResponse> = MutableLiveData()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()

    fun getPointsSummary(mentorId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                val response = AppObjectController.commonNetworkService.getUserPointsHistory(
                    mentorId ?: Mentor.getInstance().getId(),
                    PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID)
                )
                if (response.isSuccessful && response.body() != null) {
                    pointsHistoryLiveData.postValue(response.body())
                }
                apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
            } catch (ex: Exception) {
                apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                ex.showAppropriateMsg()
            }
        }
    }

    fun getSpokenMinutesSummary(mentorId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                val response = AppObjectController.commonNetworkService.getUserSpokenMinutesHistory(
                    mentorId ?: Mentor.getInstance().getId(),
                    PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID)
                )
                if (response.isSuccessful && response.body() != null) {
                    spokenHistoryLiveData.postValue(response.body())
                }
                apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
            } catch (ex: Exception) {
                apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                ex.showAppropriateMsg()
            }
        }
    }

    fun getPointsInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getPointsInfo()
                if (response.isSuccessful && response.body() != null) {
                    pointsInfoLiveData.postValue(response.body())
                }
                apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
            } catch (ex: Exception) {
                apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                ex.showAppropriateMsg()
            }
        }
    }
}
