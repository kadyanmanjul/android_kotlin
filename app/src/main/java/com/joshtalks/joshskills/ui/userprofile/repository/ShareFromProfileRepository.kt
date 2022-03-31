package com.joshtalks.joshskills.ui.userprofile.repository

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.LinkAttribution
import com.joshtalks.joshskills.util.showAppropriateMsg

class ShareFromProfileRepository{

    private val apiService by lazy { AppObjectController.commonNetworkService }

    suspend fun getDeepLink(requestData: LinkAttribution){
        try {
            apiService.getDeepLink(requestData)
        } catch (ex: Throwable) {
            ex.showAppropriateMsg()
        }
    }
}