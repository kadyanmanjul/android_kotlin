package com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.viewmodels

import android.app.Application
import android.os.Message
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.premium.base.EventLiveData
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.models.InterestModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class CallInterestViewModel(val applicationContext:Application) :AndroidViewModel(applicationContext){

    private val singleLiveEvent = EventLiveData
    private val p2pNetworkService by lazy { AppObjectController.p2pNetworkService }

    var levelLiveData = MutableLiveData <Array<HashMap<String,String>>>()
    var interestLiveData = MutableLiveData<InterestModel>()

    init {
        getUserInterests()
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
                hash["interest_ids"] = ids
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
                    interestLiveData.value?.clear() // to ensure there are no duplicates incase fragment is called from backstack
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

    fun saveImpression(eventName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("mentor_id", Mentor.getInstance().getId()),
                    Pair("event_name", eventName)
                )
                AppObjectController.commonNetworkService.saveImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }
}