package com.joshtalks.joshskills.ui.activity_feed.viewModel

import android.annotation.SuppressLint
import android.os.Handler
import android.provider.Contacts
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.clevertap.android.sdk.Utils.runOnUiThread
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.ui.activity_feed.ActivityFeedListAdapter
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedResponse
import com.joshtalks.joshskills.ui.activity_feed.repository.ActivityFeedRepository
import com.joshtalks.joshskills.ui.activity_feed.utils.*
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.sql.Timestamp

class ActivityFeedViewModel : BaseViewModel() {
    val firebaseRepository = ActivityFeedRepository()

    val adapter = ActivityFeedListAdapter()
    val isScrollToEndButtonVisible = ObservableBoolean(false)
    val updateProfilePicOrBorder = ObservableBoolean(false)
    val fetchingAllFeed = ObservableBoolean(false)
    var startTime = System.currentTimeMillis()
    var impressionId = ObservableField(EMPTY)


//    fun getFeed() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                fetchingAllFeed.set(true)
//                val collectionRef = firebaseRepository.getActivityFeed()
//                collectionRef.addSnapshotListener { value, e ->
//                    if (e != null) {
//                        return@addSnapshotListener
//                    }
//                    if (FirstTimeUser.flag) {
//                        for (doc in value!!.documentChanges) {
//                            if (doc.type == DocumentChange.Type.ADDED) {
//                                if (localFlag) {
//                                    val timeGap = (System.currentTimeMillis() - feedTime).div(1000)
//                                    if (timeGap >= 0.5) {
//                                        fetchingAllFeed.set(false)
//                                        currentFeed = doc.document.toObject()
//                                        currentFeed.photoUrl =
//                                            doc.document.get("photo_url").toString()
//                                        if(doc.document.get("event_id").toString()!="null") {
//                                            currentFeed.eventId =
//                                                doc.document.get("event_id").toString().toInt()
//                                        }
//                                        currentFeed.mediaUrl =
//                                            doc.document.get("media_url").toString()
//                                        currentFeed.mentorId =
//                                            doc.document.get("mentor_id").toString()
//                                        if(doc.document.get("media_duration").toString()!="null") {
//                                            currentFeed.duration =
//                                                doc.document.get("media_duration").toString()
//                                                    .toInt()
//                                        }
//                                        feedTime = System.currentTimeMillis()
//                                        currentFeed?.let { adapter.items.add(0, it) }
//                                        adapter.notifyItemInserted(0)
//                                        message.what= ON_ITEM_ADDED
//                                        singleLiveEvent.value=message
//                                    }
//                                } else {
//                                    fetchingAllFeed.set(false)
//                                    currentFeed = doc.document.toObject()
//                                    currentFeed.photoUrl =
//                                        doc.document.get("photo_url").toString()
//                                    if(doc.document.get("event_id").toString()!="null") {
//                                        currentFeed.eventId =
//                                            doc.document.get("event_id").toString().toInt()
//                                    }
//                                    currentFeed.mediaUrl =
//                                        doc.document.get("media_url").toString()
//                                    currentFeed.mentorId =
//                                        doc.document.get("mentor_id").toString()
//                                    if(doc.document.get("media_duration").toString()!="null") {
//                                        currentFeed.duration =
//                                            doc.document.get("media_duration").toString()
//                                                .toInt()
//                                    }
//                                    currentFeed?.let { adapter.items.add(0, it) }
//                                    adapter.notifyItemInserted(0)
//                                }
//
//                            }
//                        }
//                    }
//                    localFlag = true
//                    FirstTimeUser.flag = true
//                }
//            } catch (ex: Throwable) {
//                ex.showAppropriateMsg()
//                fetchingAllFeed.set(false)
//            }
//        }
//
//    }

    fun getActivityFeed(timestamp: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if(timestamp.isEmpty()) {
                    fetchingAllFeed.set(true)
                }
                val response =
                    AppObjectController.commonNetworkService.getActivityFeedData(timestamp)
                if (response.isSuccessful) {
                    response.body()?.let {
                        fetchingAllFeed.set(false)
                        impressionId.set(response.body()?.impressionId)
                        it.activityList?.forEach {currentFeed->
                            addItem(currentFeed)
                            delay(1000)
                        }
                        it.activityList?.let{
                            if(it.size==0){
                                delay(60L)
                            }
                        }

                        response.body()?.timestamp?.let { getActivityFeed(it) }
                    }

                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                fetchingAllFeed.set(false)
            }
        }
    }
    @SuppressLint("RestrictedApi")
    private fun addItem(activityFeedResponse: ActivityFeedResponse) {
        runOnUiThread {
            adapter.items.add(0, activityFeedResponse)
            adapter.notifyItemInserted(0)
            message.what= ON_ITEM_ADDED
            singleLiveEvent.value=message
        }
    }

    fun engageActivityFeedTime(impressionId: String, startTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (impressionId.isBlank())
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

    val onItemClick: (ActivityFeedResponse, Int) -> Unit = { it, type ->
        when (type) {
            OPEN_FEED_USER_PROFILE -> {
                message.what = OPEN_FEED_USER_PROFILE
                message.obj = it
                singleLiveEvent.value = message
            }
            OPEN_PROFILE_IMAGE_FRAGMENT -> {
                message.what = OPEN_PROFILE_IMAGE_FRAGMENT
                message.obj = it
                singleLiveEvent.value = message
            }
        }
    }

    fun onBackPress(view: View) {
        saveEngageTime()
    }

    fun onScrollToEnd(view: View) {
        message.what = FEED_SCROLL_TO_END
        singleLiveEvent.value = message
    }

    fun saveEngageTime() {
        startTime = System.currentTimeMillis().minus(startTime).div(1000)
        if (startTime > 0 && impressionId.get()?.isNotBlank() == true) {
            engageActivityFeedTime(impressionId.get() ?: EMPTY, startTime)
        }
        message.what = ON_FEED_BACK_PRESSED
        singleLiveEvent.value = message
    }

}