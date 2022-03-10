package com.joshtalks.joshskills.ui.special_practice.repo

import com.joshtalks.joshskills.core.AppObjectController

class SpecialPracticeRepo {
    suspend fun getSpecialData(params: HashMap<String, String>) =
        AppObjectController.commonNetworkService.getSpecialPracticeDetails(params)
}