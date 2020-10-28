package com.joshtalks.joshskills.ui.voip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.util.showAppropriateMsg
import java.util.HashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VoipCallingViewModel(application: Application) : AndroidViewModel(application) {

    val voipDetailsLiveData: MutableLiveData<HashMap<String, String?>?> = MutableLiveData()
    fun getUserForTalk(courseId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getP2PUser(courseId)
                if (response != null) {
                    voipDetailsLiveData.postValue(response)
                } else {
                    voipDetailsLiveData.postValue(null)
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                voipDetailsLiveData.postValue(null)
            }
        }
    }
}
