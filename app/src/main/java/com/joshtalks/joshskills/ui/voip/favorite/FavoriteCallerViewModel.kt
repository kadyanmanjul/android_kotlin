package com.joshtalks.joshskills.ui.voip.favorite

import android.view.View
import android.widget.Toast
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
import com.joshtalks.joshskills.repository.local.entity.practise.FavoriteCaller
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.fpp.constants.FAV_CLICK_ON_CALL
import com.joshtalks.joshskills.ui.fpp.constants.FAV_CLICK_ON_PROFILE
import com.joshtalks.joshskills.ui.fpp.constants.FAV_LIST_SCREEN_BACK_PRESSED
import com.joshtalks.joshskills.ui.fpp.constants.FAV_USER_LONG_PRESS_CLICK
import com.joshtalks.joshskills.ui.fpp.constants.OPEN_CALL_SCREEN
import com.joshtalks.joshskills.ui.fpp.constants.OPEN_RECENT_SCREEN
import com.joshtalks.joshskills.ui.fpp.constants.FINISH_ACTION_MODE
import com.joshtalks.joshskills.ui.fpp.constants.SET_TEXT_ON_ENABLE_ACTION_MODE
import com.joshtalks.joshskills.ui.fpp.constants.ENABLE_ACTION_MODE
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.joshtalks.joshskills.ui.voip.favorite.adapter.FppFavoriteAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class FavoriteCallerViewModel : BaseViewModel() {
    private var favoriteCallerDao = AppObjectController.appDatabase.favoriteCallerDao()
    private val favoriteCallerRepository = FavoriteCallerRepository()
    val favoriteCallerList = MutableLiveData<List<FavoriteCaller>>()
    val checkCallOngoing = MutableLiveData<HashMap<String, String>>()
    val adapter = FppFavoriteAdapter()
    val isProgressBarShow = ObservableBoolean(false)
    val isEmptyCardShow = ObservableBoolean(false)
    val dispatcher: CoroutineDispatcher by lazy { Dispatchers.Main }
    val deleteRecords: MutableSet<FavoriteCaller> = mutableSetOf()

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

    fun deleteUsersFromFavoriteList(list: MutableList<FavoriteCaller>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                favoriteCallerDao.updateFavoriteCallerStatus(list.map { it.id })
                deleteFavoriteUsers()
            } catch (ex: Exception) {
                Timber.d(ex)
            }
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
                        isProgressBarShow.set(false)
                        isEmptyCardShow.set(false)
                    }
                    if (adapter.itemCount <= 0) {
                        withContext(dispatcher) {
                            isProgressBarShow.set(false)
                            isEmptyCardShow.set(true)
                        }
                    }
                } else {
                    withContext(dispatcher) {
                        isProgressBarShow.set(false)
                        isEmptyCardShow.set(true)
                    }
                }
            } catch (ex: Throwable) {
                isProgressBarShow.set(false)
                isEmptyCardShow.set(false)
                ex.printStackTrace()
            }
        }
    }

    private fun deleteFavoriteUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = favoriteCallerDao.getRemoveFromFavoriteCallers()
                if (list.isEmpty()) {
                    return@launch
                }
                val requestParams: HashMap<String, List<Int>> = HashMap()
                requestParams["mentor_ids"] = list
                val response = favoriteCallerRepository.removeUserFormFppLit(requestParams)
                if (response.isSuccessful) {
                    favoriteCallerDao.removeFromFavorite(list)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun getCallOnGoing(toMentorId: String, uid: Int) {
        if (UpdateReceiver.isNetworkAvailable()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val map: HashMap<String, String> = HashMap()
                    map["from_mentor_id"] = Mentor.getInstance().getId()
                    map["to_mentor_id"] = toMentorId
                    val response = favoriteCallerRepository.userIsCallOrNot(map)
                    if (response.isSuccessful) {
                        if (response.code() == 200) {
                            withContext(dispatcher) {
                                message.what = OPEN_CALL_SCREEN
                                message.obj = uid
                                singleLiveEvent.value = message
                            }
                        } else {
                            showToast("Partner is on another call", Toast.LENGTH_LONG)
                        }
                    }
                    if (response.code() == 400){
                        showToast("Partner is on another call", Toast.LENGTH_LONG)
                    }
                } catch (ex: Throwable) {
                    ex.printStackTrace()
                }
            }
        }else{
            showToast("Seems like your Internet is too slow or not available.")
        }
    }

    fun onBackPress(view: View) {
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
        if (WebRtcService.isCallOnGoing.value == false) {
            getCallOnGoing(favoriteCaller.mentorId, favoriteCaller.id)
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
            deleteRecords.clear()
            adapter.clearSelections()
        } else {
            message.what = SET_TEXT_ON_ENABLE_ACTION_MODE
            message.obj = deleteRecords.size.toString()
            singleLiveEvent.value = message
        }
    }

    fun deleteFavoriteUserFromList() {
        showToast(getDeleteMessage())
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
}