package com.joshtalks.joshskills.ui.voip.share_call

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LinkAttribution
import com.joshtalks.joshskills.ui.group.repository.ABTestRepository
import com.joshtalks.joshskills.ui.referral.ReferralViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

class ShareWithFriendsViewModel(application: Application) : AndroidViewModel(application) {

    val repository: ABTestRepository by lazy { ABTestRepository() }

    fun getDeepLink(deepLink: String, contentId: String) {
        viewModelScope.launch {
            try {
                val requestData = LinkAttribution(
                    mentorId = Mentor.getInstance().getId(),
                    contentId = contentId,
                    sharedItem = "TWENTY_MINUTE_IMAGE",
                    sharedItemType = "IM",
                    deepLink = deepLink
                )
                val res = AppObjectController.commonNetworkService.getDeepLink(requestData)
                Timber.i(res.body().toString())
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    fun postGoal(goal: String, campaign: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.postGoal(goal)
            if (campaign != null) {
                val data = ABTestRepository().getCampaignData(campaign)
                data?.let {
                    val props = JSONObject()
                    props.put("Variant", data?.variantKey ?: EMPTY)
                    props.put("Variable", AppObjectController.gsonMapper.toJson(data?.variableMap))
                    props.put("Campaign", campaign)
                    props.put("Goal", goal)
                    MixPanelTracker().publishEvent(goal, props)
                }
            }
        }
    }
}