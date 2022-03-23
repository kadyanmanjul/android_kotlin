package com.joshtalks.joshskills.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.AppObjectController.Companion.appDatabase
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.HashMap

class ExtendFreeTrialViewModel(application: Application) : AndroidViewModel(application)  {

    private val _extendedFreeTrialCourseNetworkData = MutableSharedFlow<List<InboxEntity>>(replay = 0)
    val extendedFreeTrialCourseNetworkData: SharedFlow<List<InboxEntity>>
        get() = _extendedFreeTrialCourseNetworkData
    var isDataObtainedProcessRunninng : MutableLiveData<Boolean> = MutableLiveData()

    fun extendFreeTrial() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val extras: HashMap<String,String > = HashMap()
                extras["mentor_id"] = Mentor.getInstance().getId()
                val response = AppObjectController.chatNetworkService.extendFreeTrial(extras)
                if(response.isSuccessful){
                    getCourseFromServer()
               }else{
                    isDataObtainedProcessRunninng.postValue(false)
                    showToast("Free Trial can't be extended")
                }
            } catch (ex: Exception) {
                isDataObtainedProcessRunninng.postValue(false)
                ex.showAppropriateMsg()
            }
        }
    }

    private fun getCourseFromServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val courseListResponse =
                    AppObjectController.chatNetworkService.getRegisteredCourses()
                if (courseListResponse.isEmpty()) {
                    _extendedFreeTrialCourseNetworkData.emit(emptyList())
                    return@launch
                }
                appDatabase.courseDao().insertRegisterCourses(courseListResponse).let {
                    delay(1000)
                    _extendedFreeTrialCourseNetworkData.emit(
                        appDatabase.courseDao().getRegisterCourseMinimal()
                    )
                }
            } catch (ex: Exception) {
                isDataObtainedProcessRunninng.postValue(false)
                ex.printStackTrace()
            }
        }
    }

}