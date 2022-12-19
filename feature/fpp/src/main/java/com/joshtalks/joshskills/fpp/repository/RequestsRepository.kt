package com.joshtalks.joshskills.fpp.repository

import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.ui.fpp.PendingRequestResponse
import retrofit2.Response

class RequestsRepository {
    private val p2pNetworkService by lazy { AppObjectController.p2pNetworkService }

    suspend fun getPendingRequestsList(): Response<PendingRequestResponse> =
        p2pNetworkService.getPendingRequestsList()

    suspend fun confirmOrRejectFppRequest(senderMentorId: String, map: Map<String, String>) =
        p2pNetworkService.confirmOrRejectFppRequest(senderMentorId, map)
}
