package com.joshtalks.joshskills.ui.voip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.voip.VoipCallDetailModel
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VoipCallingViewModel(application: Application) : AndroidViewModel(application) {

    val voipDetailsLiveData: MutableLiveData<VoipCallDetailModel> = MutableLiveData()
    fun getUserForTalk(courseId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reqObj = mapOf("course_id" to courseId)
                val response = AppObjectController.commonNetworkService.voipInitDetails(reqObj)
                voipDetailsLiveData.postValue(response)
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                voipDetailsLiveData.postValue(null)
            }
        }
    }

}
