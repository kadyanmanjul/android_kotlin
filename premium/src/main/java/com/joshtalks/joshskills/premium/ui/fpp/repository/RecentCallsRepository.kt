package com.joshtalks.joshskills.premium.ui.fpp.repository

import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.ui.fpp.model.RecentCallResponse
import retrofit2.Response

class RecentCallsRepository {
    private val p2pNetworkService by lazy { AppObjectController.p2pNetworkService }

    suspend fun fetchRecentCallsFromApi(): Response<RecentCallResponse> =
        p2pNetworkService.getRecentCallsList(Mentor.getInstance().getId())

    suspend fun sendFppRequest(receiverMentorId: String,map: HashMap<String, String>) =
        p2pNetworkService.sendFppRequest(receiverMentorId,map)

    suspend fun deleteFppRequest(receiverMentorId: String) =
        p2pNetworkService.deleteFppRequest(receiverMentorId)

    suspend fun confirmOrRejectFppRequest(senderMentorId: String, map: Map<String, String>) =
        p2pNetworkService.confirmOrRejectFppRequest(senderMentorId, map)

    suspend fun blockUser(map: Map<String, String>) = p2pNetworkService.blockFppUser(map)
}