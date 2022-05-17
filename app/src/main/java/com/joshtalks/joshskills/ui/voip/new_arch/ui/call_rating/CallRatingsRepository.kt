package com.joshtalks.joshskills.ui.voip.new_arch.ui.call_rating

import com.joshtalks.joshskills.core.AppObjectController

class CallRatingsRepository {
    private val p2pNetworkService by lazy { AppObjectController.p2pNetworkService }

    suspend fun blockUser(map: Map<String, String>) {
        p2pNetworkService.blockFppUser(map)
    }

    suspend fun submitCallRating(map: HashMap<String, String>) {
       p2pNetworkService.submitCallRatings(map)
    }
}