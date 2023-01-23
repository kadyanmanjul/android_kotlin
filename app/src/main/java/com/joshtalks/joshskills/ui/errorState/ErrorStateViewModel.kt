package com.joshtalks.joshskills.ui.errorState

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ErrorStateViewModel : ViewModel(){

    fun saveApiFail(eventName:String){
        viewModelScope.launch (Dispatchers.IO){
            val requestData = hashMapOf(
                Pair("mentor_id", Mentor.getInstance().getId()),
                Pair("event_name", eventName)
            )
           val response =  AppObjectController.commonNetworkService.saveImpression(requestData)
            if (!response.isSuccessful){
                AppObjectController.appDatabaseConsistents.errorScreenDao().insetErrorCode(ErrorScreen(eventName))
            }
        }
    }

}