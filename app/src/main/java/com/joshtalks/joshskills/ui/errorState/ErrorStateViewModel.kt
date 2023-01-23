package com.joshtalks.joshskills.ui.errorState

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ErrorStateViewModel : ViewModel(){

    fun saveApiFail(eventName:String, payload:String,exception: String){
        viewModelScope.launch (Dispatchers.IO){
            val response = AppObjectController.commonNetworkService.pushApiLogging(
                ErrorScreen(
                    apiErrorCode = eventName,
                    payload = payload,
                    exception = exception
                )
            )
            if (!response.isSuccessful){
               // AppObjectController.appDatabaseConsistents.errorScreenDao().insetErrorCode(ErrorScreen(errorCode = eventName))
            }
        }
    }

}