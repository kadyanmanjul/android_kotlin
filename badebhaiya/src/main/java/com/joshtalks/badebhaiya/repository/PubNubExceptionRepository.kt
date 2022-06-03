package com.joshtalks.badebhaiya.repository

import com.joshtalks.badebhaiya.repository.model.PubNubExceptionRequest
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance
import javax.inject.Inject

class PubNubExceptionRepository {
    private val service = RetrofitInstance.conversationRoomNetworkService

    suspend fun sendPubNubException(params: PubNubExceptionRequest) = service.sendPubNubException(params)
}