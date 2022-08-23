package com.joshtalks.joshskills.ui.callWithExpert.repository

import com.joshtalks.joshskills.core.AppObjectController

class ExpertListRepo {
    suspend fun getExpertList() = AppObjectController.commonNetworkService.getExpertList()
}