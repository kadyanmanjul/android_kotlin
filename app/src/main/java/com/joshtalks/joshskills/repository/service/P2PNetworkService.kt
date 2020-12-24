package com.joshtalks.joshskills.repository.service

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.HashMap

interface P2PNetworkService {

    @POST("$DIR/voicecall/agora_token/")
    suspend fun getAgoraClientToken(@Body params: Map<String, String>): HashMap<String, String>

    @POST("$DIR/voicecall/agora_call_response/")
    suspend fun getAgoraCallResponse(@Body params: Map<String, String?>)

    @POST("$DIR/voicecall/agora_start_recording/")
    suspend fun startP2PCallRecording(@Body params: Map<String, String?>): HashMap<String, String>

    @POST("$DIR/voicecall/agora_stop_recording/")
    suspend fun stopP2PCallRecording(@Body params: Map<String, String?>): Any

    @GET("$DIR/voicecall/agora_user_profile/{id}/")
    suspend fun getUserDetailOnCall(@Path("id") id: String): HashMap<String, String>


}
