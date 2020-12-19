package com.joshtalks.joshskills.repository.service

import retrofit2.http.Body
import retrofit2.http.POST
import java.util.HashMap

interface P2PNetworkService {

    @POST("$DIR/voicecall/agora_token/")
    suspend fun getAgoraClientToken(@Body params: Map<String, String>): HashMap<String, String>

    @POST("$DIR/voicecall/agora_call_response/")
    suspend fun getAgoraCallResponse(@Body params: Map<String, String?>)

}
