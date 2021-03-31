package com.joshtalks.joshskills.ui.leaderboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LeaderboardResponse
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.*

class LeaderBoardViewModel(application: Application) : AndroidViewModel(application) {

    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val leaderBoardData: MutableLiveData<HashMap<String, LeaderboardResponse>> = MutableLiveData()
    val leaderBoardDataOfPage: MutableLiveData<LeaderboardResponse> = MutableLiveData()
    val leaderBoardDataOfToday: MutableLiveData<LeaderboardResponse> = MutableLiveData()
    val leaderBoardDataOfWeek: MutableLiveData<LeaderboardResponse> = MutableLiveData()
    val leaderBoardDataOfMonth: MutableLiveData<LeaderboardResponse> = MutableLiveData()

    fun getFullLeaderBoardData(mentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatusLiveData.postValue(ApiCallStatus.START)
                val map = HashMap<String, LeaderboardResponse>()
                val call1 = async(Dispatchers.IO) {
                    getMentorData(mentorId, "TODAY")?.let {
                        leaderBoardDataOfToday.postValue(it)
                        map.put("TODAY", it)
                    }
                }
                val call2 = async(Dispatchers.IO) {
                    getMentorData(mentorId, "WEEK")?.let {
                        leaderBoardDataOfWeek.postValue(it)
                        map.put("WEEK", it)
                    }
                }
                val call3 = async(Dispatchers.IO) {
                    getMentorData(mentorId, "MONTH")?.let {
                        leaderBoardDataOfMonth.postValue(it)
                        map.put("MONTH", it)
                    }
                }
                joinAll(call1, call2, call3)
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

    fun getMentorDataViaPage(mentorId: String, type: String, pageNumber: Int = 2) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.commonNetworkService.getLeaderBoardDataViaPage(
                        mentorId,
                        type,
                        pageNumber.plus(1)
                    )
                if (response.isSuccessful && response.body() != null) {
                    leaderBoardDataOfPage.postValue(response.body())
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            // return null
        }
    }

    fun engageLeaderBoardimpression(
        mapOfVisitedPage: java.util.HashMap<Int, Int>,
        position: Int
    ) {
        if (mapOfVisitedPage.get(position)!! > 1) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var intervalType = EMPTY
                when (position) {
                    0 -> {
                        intervalType = "TODAY"
                    }
                    1 -> {
                        intervalType = "WEEK"
                    }
                    2 -> {
                        intervalType = "MONTH"
                    }
                }

                AppObjectController.commonNetworkService.engageLeaderBoardImpressions(
                    mapOf(
                        "mentor_id" to Mentor.getInstance().getId(), "interval_type" to intervalType
                    )
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    suspend fun isUserHad4And5Lesson(): Boolean {
        return viewModelScope.async(Dispatchers.IO) {
            val count =
                AppObjectController.appDatabase.lessonDao().getLessonNumbers(arrayListOf(4, 5))
            if (count > 0) {
                return@async true
            }
            return@async false
        }.await()
    }
}
