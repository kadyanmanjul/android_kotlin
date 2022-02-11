package com.joshtalks.joshskills.ui.activity_feed.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.toObject
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.ActivityFeedList
import com.joshtalks.joshskills.ui.activity_feed.FirstTimeUser
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedResponseFirebase
import com.joshtalks.joshskills.ui.activity_feed.repository.FirestoreFeedRepository
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActivityFeedViewModel(application: Application) : AndroidViewModel(application) {
    val firebaseRepository = FirestoreFeedRepository()
    var currentFeed: MutableLiveData<ActivityFeedResponseFirebase> = MutableLiveData()
    val apiCallStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val feedDataList: MutableLiveData<ActivityFeedList> = MutableLiveData()
    private var feedTime = System.currentTimeMillis()
    private var localFlag = false
    fun getFeed() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatus.postValue(ApiCallStatus.START)
                val collectionRef = firebaseRepository.getActivityFeed()
                collectionRef.addSnapshotListener { value, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }
                    if (FirstTimeUser.flag) {
                        for (doc in value!!.documentChanges) {
                            if (doc.type == DocumentChange.Type.ADDED) {
                                if (localFlag) {
                                    var timeGap = (System.currentTimeMillis() - feedTime).div(1000)
                                    if (timeGap >= 0.05) {
                                        apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                                        currentFeed.value = doc.document.toObject()
                                        currentFeed.value!!.photoUrl =
                                            doc.document.get("photo_url").toString()
                                        feedTime = System.currentTimeMillis()
                                    }
                                } else {
                                    apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                                    currentFeed.value = doc.document.toObject()
                                    currentFeed.value!!.photoUrl =
                                        doc.document.get("photo_url").toString()
                                }
                            }
                        }
                    }
                    localFlag = true
                    FirstTimeUser.flag = true
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                apiCallStatus.postValue(ApiCallStatus.FAILED)
            }
        }

    }

    fun getActivityFeed() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getActivityFeedData()
                if (response.isSuccessful && response.body() != null) {
                    feedDataList.postValue(response.body()!!)
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                apiCallStatus.postValue(ApiCallStatus.FAILED)
            }
        }
    }

    fun engageActivityFeedTime(impressionId: String, startTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (impressionId.isNullOrBlank())
                    return@launch

                AppObjectController.commonNetworkService.engageActivityFeedTime(
                    impressionId,
                    mapOf("time_spent" to startTime)
                )

            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

}