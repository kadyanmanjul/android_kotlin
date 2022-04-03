package com.joshtalks.joshskills.ui.userprofile.repository

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.LinkAttribution

class ShareFromProfileRepository{

    private val apiService by lazy { AppObjectController.commonNetworkService }

    suspend fun getDeepLink(requestData: LinkAttribution){
            apiService.getDeepLink(requestData)
    }
}