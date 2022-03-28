package com.joshtalks.joshskills.ui.fpp.repository

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.ui.fpp.model.PendingRequestResponse
import retrofit2.Response

class RequestsRepository {
    private val p2pNetworkService by lazy { AppObjectController.p2pNetworkService }

    suspend fun getPendingRequestsList(): Response<PendingRequestResponse> =
        p2pNetworkService.getPendingRequestsList()

    suspend fun confirmOrRejectFppRequest(senderMentorId: String, map: Map<String, String>) =
        p2pNetworkService.confirmOrRejectFppRequest(senderMentorId, map)
}
