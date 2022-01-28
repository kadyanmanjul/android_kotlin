package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.quizgame.base.GameBaseViewModel
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstance
import com.joshtalks.joshskills.quizgame.ui.data.repository.FavouriteRepo
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

class FavouriteViewModelGame(var favouriteViewModelApplication: Application) : GameBaseViewModel(favouriteViewModelApplication) {

    val favouriteRepo = FavouriteRepo(RetrofitInstance.getRetrofitInstance())
    val favData: MutableLiveData<FavouriteList> = MutableLiveData()
    val fromTokenData: MutableLiveData<ChannelData> = MutableLiveData()
    val agoraToToken: MutableLiveData<AgoraToTokenResponse> = MutableLiveData()
    val agoraCallResponse: MutableLiveData<Success> = MutableLiveData()
    var fppData: MutableLiveData<Success> = MutableLiveData()

    fun fetchFav(mentorId: String) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch {
                    coroutineScope {

                        val fav = async {
                            val response = favouriteRepo.getFavourite(mentorId)
                            if (response?.isSuccessful == true && response.body() != null) {
                                favData.postValue(response.body())
                            }
                        }

                        val fromToken = async {
                            val response = favouriteRepo.getAgoraFromToken(AgoraFromToken(mentorId,mentorId))
                            if (response?.isSuccessful == true && response.body() != null) {
                                fromTokenData.postValue(response.body())
                            }
                        }

                        try {
                            fav.await()
                            fromToken.await()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }

    fun getChannelData(agoraToId: String?, channelName: String?) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = favouriteRepo.getChannelData(agoraToId, channelName)
                    if (response?.isSuccessful == true && response.body() != null) {
                        agoraToToken.postValue(response.body())
                    }
                }
            }
        } catch (ex: Throwable) {
            Timber.d(ex)
        }
    }

    fun addFavouritePracticePartner(addFavouritePartner: AddFavouritePartner) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = favouriteRepo.addFavouritePartner(addFavouritePartner)
                    if (response?.isSuccessful == true && response.body() != null) {
                        fppData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }
}