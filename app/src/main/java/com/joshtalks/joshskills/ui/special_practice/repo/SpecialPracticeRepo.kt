package com.joshtalks.joshskills.ui.special_practice.repo

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.ui.special_practice.model.SaveVideoModel

class SpecialPracticeRepo {
    suspend fun getSpecialData(params: HashMap<String, String>) =
        AppObjectController.commonNetworkService.getSpecialPracticeDetails(params)

    suspend fun saveRecordedVideo(params: SaveVideoModel) =
        AppObjectController.commonNetworkService.saveVideoOnServer(params)
}