package com.joshtalks.joshskills.ui.leaderboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CURRENT_COURSE_ID
import com.joshtalks.joshskills.core.DEFAULT_COURSE_ID
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.repository.server.PreviousLeaderboardResponse
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PreviousLeaderBoardViewModel(application: Application) : AndroidViewModel(application) {

    private val jobs = arrayListOf<Job>()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val previousLeaderBoardData: MutableLiveData<PreviousLeaderboardResponse> = MutableLiveData()

    fun getPreviousLeaderboardData(mentorId: String, type: String) {
        jobs == viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.commonNetworkService.getPreviousLeaderboardData(
                        mentorId,
                        type,
                        PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID)
                    )
                if (response.isSuccessful && response.body() != null) {
                    previousLeaderBoardData.postValue(response.body())
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }

}
