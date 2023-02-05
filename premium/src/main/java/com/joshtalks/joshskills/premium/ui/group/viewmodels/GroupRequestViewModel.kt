package com.joshtalks.joshskills.premium.ui.group.viewmodels

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.premium.base.BaseViewModel
import com.joshtalks.joshskills.premium.constants.DISMISS_PROGRESS_BAR
import com.joshtalks.joshskills.premium.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.premium.constants.OPEN_PROFILE_PAGE
import com.joshtalks.joshskills.premium.constants.SHOW_PROGRESS_BAR
import com.joshtalks.joshskills.premium.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.premium.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.premium.core.analytics.ParamKeys
import com.joshtalks.joshskills.premium.core.showToast
import com.joshtalks.joshskills.premium.ui.group.adapters.GroupRequestAdapter
import com.joshtalks.joshskills.premium.ui.group.analytics.GroupAnalytics
import com.joshtalks.joshskills.premium.ui.group.constants.CLOSED_GROUP
import com.joshtalks.joshskills.premium.ui.group.model.GroupRequest
import com.joshtalks.joshskills.premium.ui.group.repository.GroupRepository
import com.joshtalks.joshskills.premium.ui.group.utils.pushMetaMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupRequestViewModel : BaseViewModel() {

    val repository = GroupRepository()
    val requestAdapter = GroupRequestAdapter()
    val noRequests = ObservableBoolean(false)

    var conversationId: String = ""
    var groupId: String = ""

    fun onBackPress() {
        MixPanelTracker.publishEvent(MixPanelEvent.BACK)
            .addParam(ParamKeys.SCREEN_NAME,"groups request to join screen")
            .push()
        message.what = ON_BACK_PRESSED
        singleLiveEvent.value = message
    }

    fun showProgressDialog(msg: String) {
        message.what = SHOW_PROGRESS_BAR
        message.obj = msg
        singleLiveEvent.value = message
    }

    fun dismissProgressDialog() {
        message.what = DISMISS_PROGRESS_BAR
        singleLiveEvent.value = message
    }

    fun openProfile(mentorId: String) {
        message.what = OPEN_PROFILE_PAGE
        message.obj = mentorId
        singleLiveEvent.value = message
        GroupAnalytics.push(GroupAnalytics.Event.OPEN_PROFILE_FROM_REQUEST, groupId, mentorId)
    }

    val openProfileOnClick: (String) -> Unit = { openProfile(it) }

    val requestBtnResponse: (String, String, Boolean) -> Unit = { mentorId, name, allow ->
        showProgressDialog("Allowing to join group...")
        viewModelScope.launch {
            try {
                val request = GroupRequest(
                    mentorId = mentorId,
                    groupId = groupId,
                    allow = allow,
                    groupType = CLOSED_GROUP
                )
                val response = repository.sendRequestResponse(request)
                if (response && allow) {
                    pushMetaMessage("${name.substringBefore(" ")} has joined the group", groupId, mentorId)
                    GroupAnalytics.push(GroupAnalytics.Event.REQUEST_ACCEPTED, groupId, mentorId)
                    MixPanelTracker.publishEvent(MixPanelEvent.GROUP_REQUEST_ALLOW)
                        .addParam(ParamKeys.GROUP_ID, request.groupId)
                        .addParam(ParamKeys.MENTOR_ID, request.mentorId)
                        .push()
                } else if (response && !allow) {
                    GroupAnalytics.push(GroupAnalytics.Event.REQUEST_DECLINED, groupId, mentorId)
                    MixPanelTracker.publishEvent(MixPanelEvent.GROUP_REQUEST_DECLINE)
                        .addParam(ParamKeys.GROUP_ID, request.groupId)
                        .addParam(ParamKeys.MENTOR_ID, request.mentorId)
                        .push()
                } else
                    showToast("Error responding to the request")
                dismissProgressDialog()
            } catch (e: Exception) {
                dismissProgressDialog()
                showToast("Error responding to the request")
                e.printStackTrace()
            }
        }
    }

    fun getRequestList() {
        showProgressDialog("Loading...")
        viewModelScope.launch(Dispatchers.IO) {
            val response = repository.fetchRequestList(groupId) ?: listOf()
            withContext(Dispatchers.Main) {
                if (response.isEmpty())
                    noRequests.set(true)
                requestAdapter.addRequestsToList(response)
                dismissProgressDialog()
            }
        }
    }
}