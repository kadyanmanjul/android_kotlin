package com.joshtalks.joshskills.ui.nps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.INSTANCE_ID
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.NPSByUserRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException

class NPSViewModel(application: Application) : AndroidViewModel(application) {
    var selectedRating: Int = -1
    private val _apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val apiCallStatusLiveData: LiveData<ApiCallStatus> = _apiCallStatusLiveData

    fun submitNPS(eventName: String?, extraInfo: String?, courseId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val npsByUserRequest = NPSByUserRequest(
                    PrefManager.getStringValue(INSTANCE_ID, true),
                    Mentor.getInstance().getId(),
                    eventName,
                    selectedRating,
                    extraInfo,
                    courseId
                )

                AppObjectController.commonNetworkService.submitNPSResponse(npsByUserRequest)
                _apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
            } catch (ex: HttpException) {
                _apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
            } catch (ex: Exception) {
                _apiCallStatusLiveData.postValue(ApiCallStatus.RETRY)
                ex.printStackTrace()
            }
        }
    }
}
