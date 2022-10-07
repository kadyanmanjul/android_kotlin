package com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels

import android.app.Application
import android.os.Message
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.InterestModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallInterestViewModel(val applicationContext:Application) :AndroidViewModel(applicationContext){

    private val singleLiveEvent = EventLiveData
    private val p2pNetworkService by lazy { AppObjectController.p2pNetworkService }

    var levelLiveData = MutableLiveData <Array<HashMap<String,String>>>()
    var interestLiveData = MutableLiveData<InterestModel>()

    init {
        getUserInterests()
    }

    fun getUserLevelDetails(){
        viewModelScope.launch {
            try {
                val response = p2pNetworkService.getUserLevelDetails()
                if (response.isSuccessful){
                    levelLiveData.postValue(response.body())
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

    fun sendUserLevel(id:Int){
        viewModelScope.launch {
            try {
                val hash = hashMapOf<String,Int>()
                hash["level"] = id
                p2pNetworkService.sendUserSpeakingLevel(hash)
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

    fun sendUserInterest(ids:List<Int>){
        viewModelScope.launch {
            try {
                val hash = hashMapOf<String,List<Int>>()
                hash["level"] = ids
                p2pNetworkService.sendUserInterestDetails(hash)
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

    fun getUserInterests(){
        viewModelScope.launch {
            try {
                val response = p2pNetworkService.getUserInterestDetails()
                if (response.isSuccessful){
                    interestLiveData.postValue(response.body())
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

    fun sendEvent(fragment: Int) {
        val msg = Message.obtain().apply {
            what = fragment
        }
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                singleLiveEvent.value = msg
            }
        }
    }
}