package com.joshtalks.joshskills.ui.voip.favorite

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.entity.practise.FavoriteCaller
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.voip.WebRtcActivity
import java.util.HashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class FavoriteCallerViewModel(application: Application) : AndroidViewModel(application) {
    private var favoriteCallerDao = AppObjectController.appDatabase.favoriteCallerDao()
    private val favoriteCallerRepository = FavoriteCallerRepository()
    val favoriteCallerList = MutableSharedFlow<List<FavoriteCaller>>()
    val apiCallStatus = MutableSharedFlow<ApiCallStatus>()
    val checkCallOngoing = MutableLiveData<HashMap<String, String>>()

    fun getFavorites() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getFavoriteUsersDB()
                fetchFavoriteCallersFromApi()
                deleteFavoriteUsers()
            }catch (ex:Exception){}
        }
    }

    fun deleteUsersFromFavoriteList(list: MutableList<FavoriteCaller>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                favoriteCallerDao.updateFavoriteCallerStatus(list.map { it.id })
                deleteFavoriteUsers()
            }catch (ex:Exception){}
        }
    }

    private fun fetchFavoriteCallersFromApi() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = favoriteCallerRepository.getFavList()
                favoriteCallerDao.removeAllFavorite()
                if (response.isNotEmpty()) {
                    favoriteCallerDao.insertFavoriteCallers(response)
                    getFavoriteUsersDB()
                    return@launch
                }
                apiCallStatus.emit(ApiCallStatus.SUCCESS)
            } catch (ex: Throwable) {
                apiCallStatus.emit(ApiCallStatus.SUCCESS)
                ex.printStackTrace()
            }
        }
    }

    private suspend fun getFavoriteUsersDB() {
        try {
            favoriteCallerList.emit(favoriteCallerDao.getFavoriteCallers())
        } catch (ex: Exception){}
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

    fun getCallOnGoing(toMentorId: String, uid: Int, activity: Activity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val map: HashMap<String, String> = HashMap<String, String>()
                map["from_mentor_id"] = Mentor.getInstance().getId()
                map["to_mentor_id"] = toMentorId
                val response =  favoriteCallerRepository.userIsCallOrNot(map)
                if (response.isSuccessful) {
                    if (response.code() == 200) {
                        val intent =
                            WebRtcActivity.getFavMissedCallbackIntent(uid, activity).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        activity.startActivity(intent)
                    } else {
                        showToast(response.body()?.getValue("message") ?: "", Toast.LENGTH_LONG)
                    }
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }
}