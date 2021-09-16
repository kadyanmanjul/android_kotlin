package com.joshtalks.joshskills.ui.leaderboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.HAS_ENTERED_NAME_IN_FREE_TRIAL
import com.joshtalks.joshskills.core.IS_ENTERED_NAME_IN_FREE_TRIAL
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.LeaderboardMentor
import com.joshtalks.joshskills.repository.server.LeaderboardResponse
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.*
import timber.log.Timber

class LeaderBoardViewModel(application: Application) : AndroidViewModel(application) {

    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val leaderBoardData: MutableLiveData<HashMap<String, LeaderboardResponse>> = MutableLiveData()
    val leaderBoardDataOfPage: MutableLiveData<LeaderboardResponse> = MutableLiveData()
    val leaderBoardDataOfToday: MutableLiveData<LeaderboardResponse> = MutableLiveData()
    val leaderBoardDataOfWeek: MutableLiveData<LeaderboardResponse> = MutableLiveData()
    val leaderBoardDataOfMonth: MutableLiveData<LeaderboardResponse> = MutableLiveData()
    val leaderBoardDataOfBatch: MutableLiveData<LeaderboardResponse> = MutableLiveData()
    val overlayLiveData: MutableLiveData<LeaderboardMentor> = MutableLiveData()
    val leaderBoardDataOfLifeTime: MutableLiveData<LeaderboardResponse> = MutableLiveData()
    val apiCallStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val userEnteredName: MutableLiveData<String> = MutableLiveData()
    val eventLiveData : MutableLiveData<Event?> = MutableLiveData()

    fun getFullLeaderBoardData(mentorId: String, course_id: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatusLiveData.postValue(ApiCallStatus.START)
                val map = HashMap<String, LeaderboardResponse>()
                val call1 = async(Dispatchers.IO) {
                    getMentorData(mentorId, "TODAY", course_id)?.let {
                        leaderBoardDataOfToday.postValue(it)
                        map.put("TODAY", it)
                    }
                }
                val call2 = async(Dispatchers.IO) {
                    getMentorData(mentorId, "WEEK", course_id)?.let {
                        leaderBoardDataOfWeek.postValue(it)
                        map.put("WEEK", it)
                    }
                }
                val call3 = async(Dispatchers.IO) {
                    getMentorData(mentorId, "MONTH", course_id)?.let {
                        leaderBoardDataOfMonth.postValue(it)
                        map.put("MONTH", it)
                    }
                }
                val call4 = async(Dispatchers.IO) {
                    getMentorData(mentorId, "BATCH", course_id)?.let {
                        leaderBoardDataOfBatch.postValue(it)
                        map.put("BATCH", it)
                    }
                }
                val call5 = async(Dispatchers.IO) {
                    getMentorData(mentorId, "LIFETIME", course_id)?.let {
                        leaderBoardDataOfLifeTime.postValue(it)
                        map.put("LIFETIME", it)
                    }
                }
                joinAll(call1, call2, call3, call4, call5)
                apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                leaderBoardData.postValue(map)
                return@launch
            } catch (ex: Exception) {
                apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

    fun getRefreshedLeaderboardData(mentorId: String, courseId: String?, type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getMentorData(mentorId, type, courseId)?.let {
                    when (type) {
                        "TODAY" -> {
                            leaderBoardDataOfToday.postValue(it)
                        }
                        "WEEK" -> {
                            leaderBoardDataOfWeek.postValue(it)
                        }
                        "MONTH" -> {
                            leaderBoardDataOfMonth.postValue(it)
                        }
                        "BATCH" -> {
                            leaderBoardDataOfBatch.postValue(it)
                        }
                        "LIFETIME" -> {
                            leaderBoardDataOfLifeTime.postValue(it)
                        }
                        else -> {
                            Timber.e("error: No type found")
                        }
                    }
                    leaderBoardDataOfToday.postValue(it)
                }
                return@launch
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    suspend fun getMentorData(
        mentorId: String,
        type: String,
        course_id: String?
    ): LeaderboardResponse? {
        try {
            val response =
                AppObjectController.commonNetworkService.getLeaderBoardData(
                    mentorId,
                    type,
                    course_id
                )
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
                    4 -> {
                        intervalType = "BATCH"
                    }
                    3 -> {
                        intervalType = "LIFETIME"
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
            if (isUserOpen4Lesson()) {
                return@async true
            }
            val count =
                AppObjectController.appDatabase.lessonDao().getLessonNumbers(arrayListOf(5))
            if (count > 0) {
                return@async true
            }
            return@async false
        }.await()
    }

    private suspend fun isUserOpen4Lesson(): Boolean {
        return viewModelScope.async(Dispatchers.IO) {
            val count =
                AppObjectController.appDatabase.lessonQuestionDao().getLessonCount(3)
            if (count > 0) {
                return@async true
            }
            return@async false
        }.await()
    }

    fun updateUserName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestMap = mutableMapOf<String, String?>()
                requestMap["first_name"] = name
                val response =
                    AppObjectController.signUpNetworkService.updateUserProfile(
                        Mentor.getInstance().getUserId(), requestMap
                    )
                if (response.isSuccessful) {
                    response.body()?.let {
                        PrefManager.put(HAS_ENTERED_NAME_IN_FREE_TRIAL, true, false)
                        PrefManager.put(IS_ENTERED_NAME_IN_FREE_TRIAL, true, false)
                        User.getInstance().updateFromResponse(it)
                    }
                    apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                    userEnteredName.postValue(name)
                    return@launch
                } else {
                    apiCallStatus.postValue(ApiCallStatus.FAILED)

                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                apiCallStatus.postValue(ApiCallStatus.FAILED)
            }
        }
    }

    fun showOverlay(response : LeaderboardMentor) = overlayLiveData.setValue(response)

    override fun onCleared() {
        super.onCleared()
        LeaderBoardViewPagerActivity.winnerMap.clear()
    }
}
