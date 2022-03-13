package com.joshtalks.joshskills.ui.fpp.repository

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.fpp.model.RecentCallResponse
import retrofit2.Response

class RecentCallsRepository {
    private val p2pNetworkService by lazy { AppObjectController.p2pNetworkService }

    suspend fun fetchRecentCallsFromApi(): Response<RecentCallResponse>? {
        try {
            return p2pNetworkService.getRecentCallsList(Mentor.getInstance().getId())
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
        return null
    }

    suspend fun sendFppRequest(receiverMentorId: String) {
        try {
            p2pNetworkService.sendFppRequest(receiverMentorId)
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }

    }

    suspend fun deleteFppRequest(receiverMentorId: String) {
        try {
            p2pNetworkService.deleteFppRequest(receiverMentorId)
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }

    }

    suspend fun confirmOrRejectFppRequest(senderMentorId: String, map: Map<String, String>) {
        try {
            p2pNetworkService.confirmOrRejectFppRequest(senderMentorId, map)
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    suspend fun blockUser(map: Map<String, String>) {
        try {
            var res = p2pNetworkService.blockFppUser(map)
            if (res.isSuccessful) {
                fetchRecentCallsFromApi()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }

    }
}