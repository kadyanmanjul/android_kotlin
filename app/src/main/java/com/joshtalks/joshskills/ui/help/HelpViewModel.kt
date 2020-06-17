package com.joshtalks.joshskills.ui.help

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.server.ComplaintResponse
import com.joshtalks.joshskills.repository.server.TypeOfHelpModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class HelpViewModel(application: Application) : AndroidViewModel(application) {

    var context: JoshApplication = getApplication()
    val typeOfHelpModelLiveData: MutableLiveData<List<TypeOfHelpModel>> = MutableLiveData()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    lateinit var complaintResponse: ComplaintResponse
    private val jobs = arrayListOf<Job>()

    fun getAllHelpCategory() {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.commonNetworkService.getHelpCategoryV2()
                if (response.isSuccessful) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    typeOfHelpModelLiveData.postValue(response.body())
                    return@launch
                }

            } catch (ex: Exception) {
                when (ex) {
                    is HttpException -> {
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        showToast(context.getString(R.string.internet_not_available_msz))
                    }
                    else -> {
                        Crashlytics.logException(ex)
                    }
                }
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

    override fun onCleared() {
        super.onCleared()
        jobs.forEach { it.cancel() } // cancels the job and waits for its completion
    }
}