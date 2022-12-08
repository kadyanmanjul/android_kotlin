package com.joshtalks.joshskills.common.ui.voip.favorite

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.core.pstn_states.PSTNState
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.common.repository.local.entity.practise.FavoriteCaller
import com.joshtalks.joshskills.common.ui.fpp.constants.*
import com.joshtalks.joshskills.common.ui.voip.favorite.adapter.FppFavoriteAdapter
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.utils.getVoipState
import com.joshtalks.joshskills.voip.constant.State
import kotlinx.coroutines.*
import timber.log.Timber

class FavoriteCallerViewModel : com.joshtalks.joshskills.common.base.BaseViewModel() {
    private var favoriteCallerDao = AppObjectController.appDatabase.favoriteCallerDao()
    private val favoriteCallerRepository = FavoriteCallerRepository()
    val favoriteCallerList = MutableLiveData<List<FavoriteCaller>>()
    val checkCallOngoing = MutableLiveData<HashMap<String, String>>()
    val adapter = FppFavoriteAdapter()
    val isProgressBarShow = ObservableBoolean(false)
    val isEmptyCardShow = ObservableBoolean(false)
    val dispatcher: CoroutineDispatcher by lazy { Dispatchers.Main }
    val deleteRecords: MutableSet<FavoriteCaller> = mutableSetOf()
    var selectedUser: FavoriteCaller? = null
    val scope = CoroutineScope(Dispatchers.IO)


    fun getFavorites() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                fetchFavoriteCallersFromApi()
                deleteFavoriteUsers()
            } catch (ex: Exception) {
                Timber.d(ex)
            }
        }
    }

    suspend fun deleteUsersFromFavoriteList(list: MutableList<FavoriteCaller>) {
            try {
                favoriteCallerDao.updateFavoriteCallerStatus(list.map { it.id })
                deleteFavoriteUsers()
            } catch (ex: Exception) {
                Timber.d(ex)
            }
    }

    private fun fetchFavoriteCallersFromApi() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isProgressBarShow.set(true)
                val response = favoriteCallerRepository.getFavList()
                favoriteCallerDao.removeAllFavorite()
                if (response.isNotEmpty()) {
                    favoriteCallerDao.insertFavoriteCallers(response)

                    withContext(dispatcher) {
                        adapter.addItems(response)
                        progressAndEmptyCardVisibility(isProgress = false, isEmptyCard = false)
                    }
                    if (adapter.itemCount <= 0) {
                        withContext(dispatcher) {
                            progressAndEmptyCardVisibility(isProgress = false, isEmptyCard = true)
                        }
                    }
                } else {
                    withContext(dispatcher) {
                        if (adapter.itemCount <= 0) {
                            progressAndEmptyCardVisibility(isProgress = false, isEmptyCard = true)

                        } else {
                            adapter.clearItem(0)
                            progressAndEmptyCardVisibility(isProgress = false, isEmptyCard = true)
                        }
                    }
                }
            } catch (ex: Throwable) {
                if (adapter.itemCount <= 0) {
                    progressAndEmptyCardVisibility(isProgress = true, isEmptyCard = true)
                }
                showToast("An error occurred while fetching data")
                ex.printStackTrace()
            }
        }
    }

    private suspend fun deleteFavoriteUsers() {
            try {
                val deletedList = favoriteCallerDao.getRemoveFromFavoriteCallers()
                if (deletedList.isEmpty()) {
                    return
                }
                if (Utils.isInternetAvailable()) {
                    val requestParams: HashMap<String, List<Int>> = HashMap()
                    requestParams["mentor_ids"] = deletedList
                    val response = favoriteCallerRepository.removeUserFormFppLit(requestParams)
                    if (response.isSuccessful) {
                        favoriteCallerDao.removeFromFavorite(deletedList)
                    }
                }else{
                    showToast("No Internet Connection")
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
    }

    fun onBackPress(view: View) {
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
        message.what = FAV_LIST_SCREEN_BACK_PRESSED
        singleLiveEvent.value = message
    }

    val onItemClick: (FavoriteCaller, Int, Int) -> Unit = { it, type, position ->
        when (type) {
            FAV_CLICK_ON_PROFILE -> {
                message.what = FAV_CLICK_ON_PROFILE
                message.obj = it.mentorId
                message.arg1 = position
                singleLiveEvent.value = message
            }
            FAV_CLICK_ON_CALL -> {
                clickOnPhoneCall(it)
            }
            FAV_USER_LONG_PRESS_CLICK -> {
                updateListRow(position)
            }
        }
    }

    fun onClickOpenRecentCall(view: View) {
        message.what = OPEN_RECENT_SCREEN
        singleLiveEvent.value = message
    }

    fun clickOnPhoneCall(favoriteCaller: FavoriteCaller) {
        if(checkPstnState() != PSTNState.Idle){
            showToast(
                "You can't place a new call while you're already in a call.",
                Toast.LENGTH_LONG
            )
            return
        }
        if (getVoipState() == State.IDLE) {
            Log.d("naa", "clickOnPhoneCall: ${favoriteCaller.mentorId}")
            selectedUser = favoriteCaller
            message.what = START_FPP_CALL
            singleLiveEvent.value = message
        } else {
            showToast(
                "You can't place a new call while you're already in a call.",
                Toast.LENGTH_LONG
            )
        }
    }

    fun updateListRow(position: Int) {
        enableActionMode(position)
    }

    fun enableActionMode(position: Int) {
        message.what = ENABLE_ACTION_MODE
        message.obj = position
        singleLiveEvent.value = message
        toggleSelection(position)
    }

    fun toggleSelection(position: Int) {
        val item = adapter.getItemAtPosition(position)
        if (deleteRecords.contains(item)) {
            item.selected = false
            deleteRecords.remove(item)
        } else {
            item.selected = true
            deleteRecords.add(item)
        }
        adapter.updateItem(item, position)
        if (deleteRecords.isEmpty()) {
            message.what = FINISH_ACTION_MODE
            singleLiveEvent.value = message
            scope.launch {
                deleteRecords.clear()
                adapter.clearSelections()
            }
        } else {
            message.what = SET_TEXT_ON_ENABLE_ACTION_MODE
            message.obj = deleteRecords.size.toString()
            singleLiveEvent.value = message
        }
    }

    suspend fun deleteFavoriteUserFromList() {
        withContext(Dispatchers.Main) {
            showToast(getDeleteMessage())
        }
        adapter.removeAndUpdated()
            deleteUsersFromFavoriteList(deleteRecords.toMutableList())
            if (adapter.getItemSize() <= 0) {
                isEmptyCardShow.set(true)
            }
    }

    private fun getDeleteMessage(): String {
        if (deleteRecords.size > 1) {
            return "${deleteRecords.size} practice partners removed"
        }
        return "${deleteRecords.size} practice partner removed"
    }

    private fun progressAndEmptyCardVisibility(isProgress: Boolean, isEmptyCard: Boolean) {
        isProgressBarShow.set(isProgress)
        isEmptyCardShow.set(isEmptyCard)
    }
}