package com.joshtalks.joshskills.ui.voip.share_call

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LinkAttribution
import com.joshtalks.joshskills.ui.referral.ReferralViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

class ShareWithFriendsViewModel(application: Application) : AndroidViewModel(application) {

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
}