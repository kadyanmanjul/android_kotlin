package com.joshtalks.joshskills.ui.fpp.repository

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.fpp.model.RecentCallResponse
import retrofit2.Response

class RecentCallsRepository {
    private val p2pNetworkService by lazy { AppObjectController.p2pNetworkService }

    suspend fun fetchRecentCallsFromApi(): Response<RecentCallResponse> =
        p2pNetworkService.getRecentCallsList(Mentor.getInstance().getId())

    suspend fun sendFppRequest(receiverMentorId: String) =
        p2pNetworkService.sendFppRequest(receiverMentorId)

    suspend fun deleteFppRequest(receiverMentorId: String) =
        p2pNetworkService.deleteFppRequest(receiverMentorId)

    suspend fun confirmOrRejectFppRequest(senderMentorId: String, map: Map<String, String>) =
        p2pNetworkService.confirmOrRejectFppRequest(senderMentorId, map)

    suspend fun blockUser(map: Map<String, String>) {
        val res = p2pNetworkService.blockFppUser(map)
        if (res.isSuccessful) {
            fetchRecentCallsFromApi()
        }
    }
}