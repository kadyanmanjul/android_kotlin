package com.joshtalks.joshskills.ui.leaderboard.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.entity.leaderboard.RecentSearch
import com.joshtalks.joshskills.repository.server.LeaderboardMentor
import com.joshtalks.joshskills.repository.server.LeaderboardType
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class LeaderBoardSearchViewModel : ViewModel() {
    private val waitMs: Long = 300
    private var debounceJob: Job? = null
    private var currentSearchedKey: String = ""

    val searchedKeyLiveData: MutableLiveData<String> = MutableLiveData()
    val leaderBoardDataOfToday: MutableLiveData<List<LeaderboardMentor>> = MutableLiveData()
    val leaderBoardDataOfWeek: MutableLiveData<List<LeaderboardMentor>> = MutableLiveData()
    val leaderBoardDataOfMonth: MutableLiveData<List<LeaderboardMentor>> = MutableLiveData()
    val leaderBoardDataOfLifeTime: MutableLiveData<List<LeaderboardMentor>> = MutableLiveData()
    val leaderBoardDataOfBatch: MutableLiveData<List<LeaderboardMentor>> = MutableLiveData()
    val recentSearchLiveData: MutableLiveData<List<RecentSearch>> = MutableLiveData()

    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()

    fun fetchRecentSearch() {
        viewModelScope.launch {
            recentSearchLiveData.postValue(
                AppObjectController.appDatabase.recentSearch().getRecentSearchHistory()
            )
        }
    }

    fun performSearch(key: String) {
        debounceJob?.cancel()
        currentSearchedKey = key
        searchedKeyLiveData.postValue(currentSearchedKey)
        debounceJob = viewModelScope.launch(Dispatchers.IO) {
            leaderBoardDataOfToday.postValue(ArrayList())
            leaderBoardDataOfWeek.postValue(ArrayList())
            leaderBoardDataOfMonth.postValue(ArrayList())
            leaderBoardDataOfLifeTime.postValue(ArrayList())
            if (key.isEmpty())
                return@launch
            delay(waitMs)
            val call1 = async(Dispatchers.IO) {
                searchQuery(key, LeaderboardType.TODAY, 0)?.let {
                    leaderBoardDataOfToday.postValue(it)
                }
            }
            val call2 = async(Dispatchers.IO) {
                searchQuery(key, LeaderboardType.WEEK, 0)?.let {
                    leaderBoardDataOfWeek.postValue(it)
                }
            }
            val call3 = async(Dispatchers.IO) {
                searchQuery(key, LeaderboardType.MONTH, 0)?.let {
                    leaderBoardDataOfMonth.postValue(it)
                }
            }
            val call4 = async(Dispatchers.IO) {
                searchQuery(key, LeaderboardType.BATCH, 0)?.let {
                    leaderBoardDataOfBatch.postValue(it)
                }
            }

            val call5 = async(Dispatchers.IO) {
                searchQuery(key, LeaderboardType.LIFETIME, 0)?.let {
                    leaderBoardDataOfLifeTime.postValue(it)
                }
            }

            joinAll(call1, call2, call3,call4,call5)
            apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
            return@launch
        }
    }

    private suspend fun searchQuery(
        key: String,
        intervalType: LeaderboardType, pageNo: Int
    ): List<LeaderboardMentor>? {
        try {
            val response =
                AppObjectController.commonNetworkService.searchLeaderboardMember(
                    key,
                    pageNo,
                    intervalType
                )
            if (response.isSuccessful && response.body() != null) {
                return response.body()
            }

        } catch (ex: Throwable) {
            ex.showAppropriateMsg()
        }
        return null
    }

    fun getMoreResults(type: LeaderboardType, pageNo: Int) {
        when (type) {
            LeaderboardType.TODAY -> {
                getTodaySearch(currentSearchedKey, pageNo)
            }
            LeaderboardType.WEEK -> {

                getWeekSearch(currentSearchedKey, pageNo)
            }
            LeaderboardType.MONTH -> {
                getMonthSearch(currentSearchedKey, pageNo)
            }
            LeaderboardType.BATCH -> {
                getBatchSearch(currentSearchedKey, pageNo)
            }
            LeaderboardType.LIFETIME -> {
                getLifeTimeSearch(currentSearchedKey, pageNo)
            }
        }
    }

    private fun getTodaySearch(key: String, pageNo: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            apiCallStatusLiveData.postValue(ApiCallStatus.START)
            val result = searchQuery(key, LeaderboardType.TODAY, pageNo)
            if (result != null)
                leaderBoardDataOfToday.postValue(result)
        }
    }

    private fun getWeekSearch(key: String, pageNo: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            apiCallStatusLiveData.postValue(ApiCallStatus.START)
            val result = searchQuery(key, LeaderboardType.WEEK, pageNo)
            if (result != null)
                leaderBoardDataOfWeek.postValue(result)
        }
    }

    private fun getMonthSearch(key: String, pageNo: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            apiCallStatusLiveData.postValue(ApiCallStatus.START)
            val result = searchQuery(key, LeaderboardType.MONTH, pageNo)
            if (result != null)
                leaderBoardDataOfMonth.postValue(result)
        }
    }

    private fun getBatchSearch(key: String, pageNo: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            apiCallStatusLiveData.postValue(ApiCallStatus.START)
            val result = searchQuery(key, LeaderboardType.BATCH, pageNo)
            if (result != null)
                leaderBoardDataOfBatch.postValue(result)
        }
    }

    private fun getLifeTimeSearch(key: String, pageNo: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            apiCallStatusLiveData.postValue(ApiCallStatus.START)
            val result = searchQuery(key, LeaderboardType.LIFETIME, pageNo)
            if (result != null)
                leaderBoardDataOfLifeTime.postValue(result)
        }
    }

    fun insertRecentSearch(keyword: String) {
        viewModelScope.launch {
            val result = AppObjectController.appDatabase.recentSearch()
                .insertSearch(
                    RecentSearch(keyword,System.currentTimeMillis())
                )
            if (result > 0)
                fetchRecentSearch()
        }
    }

    fun clearResultHistory() {
        viewModelScope.launch {
            AppObjectController.appDatabase.recentSearch()
                .clearHistory()
            fetchRecentSearch()
        }
    }

    companion object {
        const val SEARCH_RESULT_LIMIT = 50

        private const val QUERY_DEBOUNCE = 500L
        private const val PREFETCH_DISTANCE = 20
    }
}