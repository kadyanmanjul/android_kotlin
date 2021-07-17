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
    val leaderBoardDataOfLifeTime: MutableLiveData<LeaderboardResponse> = MutableLiveData()
    val apiCallStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val userEnteredName: MutableLiveData<String> = MutableLiveData()

    fun getFullLeaderBoardData(mentorId: String, course_id: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var isApiFailed= false
                apiCallStatusLiveData.postValue(ApiCallStatus.START)
                val map = HashMap<String, LeaderboardResponse>()
                val call1 = async(Dispatchers.IO) {
                    val resultResponse = getMentorData(mentorId, "TODAY", course_id)
                    if (resultResponse!=null){
                        leaderBoardDataOfToday.postValue(resultResponse)
                        map.put("TODAY", resultResponse)
                    } else {
                        isApiFailed=true
                        return@async
                    }
                }
                val call2 = async(Dispatchers.IO) {
                    val resultResponse = getMentorData(mentorId, "WEEK", course_id)
                    if (resultResponse!=null){
                        leaderBoardDataOfWeek.postValue(resultResponse)
                        map.put("WEEK", resultResponse)
                    } else {
                        isApiFailed=true
                        return@async
                    }
                }
                val call3 = async(Dispatchers.IO) {
                    val resultResponse = getMentorData(mentorId, "MONTH", course_id)
                    if (resultResponse!=null){
                        leaderBoardDataOfMonth.postValue(resultResponse)
                        map.put("MONTH", resultResponse)
                    } else {
                        isApiFailed=true
                        return@async
                    }
                }
                val call4 = async(Dispatchers.IO) {

                    val resultResponse = getMentorData(mentorId, "BATCH", course_id)
                    if (resultResponse!=null){
                        leaderBoardDataOfBatch.postValue(resultResponse)
                        map.put("BATCH", resultResponse)
                    } else {
                        isApiFailed=true
                        return@async
                    }
                }
                val call5 = async(Dispatchers.IO) {
                    val resultResponse = getMentorData(mentorId, "LIFETIME", course_id)
                    if (resultResponse!=null){
                        leaderBoardDataOfLifeTime.postValue(resultResponse)
                        map.put("LIFETIME", resultResponse)
                    } else {
                        isApiFailed=true
                        return@async
                    }
                }
                joinAll(call1, call2, call3, call4, call5)
                if (isApiFailed){
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                    return@launch
                }
                apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                leaderBoardData.postValue(map)
                return@launch
            } catch (ex: Exception) {
                ex.printStackTrace()
                apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

    // not used after removing swipe functionality
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
            return null
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
}
