package com.joshtalks.joshskills.ui.groupchat.messagelist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cometchat.pro.models.BaseMessage
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.groupchat.utils.Utils.getMessagesFromJSONArray
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray

class CometChatMessageListViewModel(application: Application) : AndroidViewModel(application) {
    private val jobs = arrayListOf<Job>()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val pinnedMsgs: MutableLiveData<List<BaseMessage>> = MutableLiveData()

    fun getPinnedMessages(groupId: String) {
        apiCallStatusLiveData.postValue(ApiCallStatus.START)
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getPinnedMessages(groupId)
                if (response.isSuccessful && response.body() != null) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    val messagesList =
                        getMessagesFromJSONArray(JSONArray(response.body()!!.toString()))
                    messagesList.sortBy { it.sentAt }
                    pinnedMsgs.postValue(messagesList)
                    return@launch
                } else if (response.errorBody() != null
                    && response.errorBody()!!.string().contains("mentor_id is not valid")
                ) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                    showToast(AppObjectController.joshApplication.getString(R.string.user_does_not_exist))
                } else {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                    showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }
}
