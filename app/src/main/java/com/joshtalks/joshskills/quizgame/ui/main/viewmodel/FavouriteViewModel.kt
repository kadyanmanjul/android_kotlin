package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.FavouriteRepo
import kotlinx.coroutines.*
import timber.log.Timber

class FavouriteViewModel(application: Application, private val favouriteRepo: FavouriteRepo) : AndroidViewModel(application){

    val favData:MutableLiveData<FavouriteList> = MutableLiveData()
    val fromTokenData:MutableLiveData<ChannelData> = MutableLiveData()
    val agoraToToken:MutableLiveData<AgoraToTokenResponse> = MutableLiveData()
    val agoraCallResponse : MutableLiveData<Success> = MutableLiveData()
    val statusResponse : MutableLiveData<Success> = MutableLiveData()

    fun fetchFav(mentorId:String) {
        try {
            viewModelScope.launch {
                coroutineScope {

                    val fav = async {
                        val response = favouriteRepo.getFavourite(mentorId)
                        if (response.isSuccessful && response.body() != null) {
                            favData.postValue(response.body())
                        }
                    }

                    val fromToken =  async {
                        val response = favouriteRepo.getAgoraFromToken(mentorId)
                        if (response.isSuccessful && response.body() != null) {
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
        }catch (ex:Exception){

        }
    }

    fun getChannelData(agoraToId: String?,channelName:String?){
        try {
            viewModelScope.launch (Dispatchers.IO){
                val response = favouriteRepo.getChannelData(agoraToId,channelName)
                if (response.isSuccessful && response.body()!=null){
                    agoraToToken.postValue(response.body())
                }
            }
        }catch (ex:Throwable){
            Timber.d(ex)
        }
    }

    fun statusChange(userIdMentor: String?,status:String?){
        try {
            viewModelScope.launch(Dispatchers.IO) {
                val response = favouriteRepo.getStatus(userIdMentor,status)
                if (response.isSuccessful && response.body()!=null){
                    statusResponse.postValue(response.body())
                }
            }
        }catch (ex:Throwable){
            Timber.d(ex)
        }
    }
}