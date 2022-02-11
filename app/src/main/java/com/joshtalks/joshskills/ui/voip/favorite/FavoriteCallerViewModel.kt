package com.joshtalks.joshskills.ui.voip.favorite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.repository.local.entity.practise.FavoriteCaller
import com.joshtalks.joshskills.repository.local.model.Mentor
import java.util.HashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class FavoriteCallerViewModel(application: Application) : AndroidViewModel(application) {
    private var favoriteCallerDao = AppObjectController.appDatabase.favoriteCallerDao()
    private val p2pNetworkService = AppObjectController.p2pNetworkService
    val favoriteCallerList = MutableSharedFlow<List<FavoriteCaller>>()
    val apiCallStatus = MutableSharedFlow<ApiCallStatus>()

    fun getFavorites() {
        viewModelScope.launch(Dispatchers.IO) {
            getFavoriteUsersDB()
            fetchFavoriteCallersFromApi()
            deleteFavoriteUsers()
        }
    }

    fun deleteUsersFromFavoriteList(list: MutableList<FavoriteCaller>) {
        viewModelScope.launch(Dispatchers.IO) {
            favoriteCallerDao.updateFavoriteCallerStatus(list.map { it.id })
            deleteFavoriteUsers()
        }
    }

    private fun fetchFavoriteCallersFromApi() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = p2pNetworkService.getFavoriteCallerList(Mentor.getInstance().getId())
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
        favoriteCallerList.emit(favoriteCallerDao.getFavoriteCallers().sortedBy { it.lastCalledAt })
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
                val response =
                    p2pNetworkService.removeFavoriteCallerList(
                        Mentor.getInstance().getId(),
                        requestParams
                    )
                if (response.isSuccessful) {
                    favoriteCallerDao.removeFromFavorite(list)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}