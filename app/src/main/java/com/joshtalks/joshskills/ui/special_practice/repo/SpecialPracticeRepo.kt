package com.joshtalks.joshskills.ui.special_practice.repo

import com.joshtalks.joshskills.core.AppObjectController

class SpecialPracticeRepo {
    suspend fun getSpecialData(params: HashMap<String, Any>) =
        AppObjectController.commonNetworkService.getSpecialPracticeDetails(params)
}