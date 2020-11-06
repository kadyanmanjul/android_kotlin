package com.joshtalks.joshskills.ui.leaderboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.LeaderboardResponse
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LeaderBoardViewModel(application: Application) : AndroidViewModel(application) {

    private val jobs = arrayListOf<Job>()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val leaderBoardData: MutableLiveData<HashMap<String, LeaderboardResponse>> = MutableLiveData()

    fun getFullLeaderBoardData(mentorId: String) {
        jobs == viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatusLiveData.postValue(ApiCallStatus.START)
                val map = HashMap<String, LeaderboardResponse>()
                getMentorData(mentorId, "TODAY")?.let {
                    map.put("TODAY", it)
                }
                getMentorData(mentorId, "WEEK")?.let {
                    map.put("WEEK", it)
                }
                getMentorData(mentorId, "MONTH")?.let {
                    map.put("MONTH", it)
                }
                leaderBoardData.postValue(map)
                apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                return@launch

            } catch (ex: Exception) {
                apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

    suspend fun getMentorData(mentorId: String, type: String): LeaderboardResponse? {
        try {
            val response =
                AppObjectController.commonNetworkService.getLeaderBoardData(mentorId, type)
            if (response.isSuccessful && response.body() != null) {
                return response.body()!!
            }

        } catch (ex: Throwable) {
            ex.showAppropriateMsg()
        }
        return null
    }
}
