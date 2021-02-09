package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.repository.server.voip.RequestUserLocation
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.*

interface P2PNetworkService {

    @POST("$DIR/voicecall/agora_token/")
    suspend fun getAgoraClientToken(@Body params: Map<String, String>): Response<HashMap<String, String>>

    @POST("$DIR/voicecall/agora_call_response/")
    suspend fun getAgoraCallResponse(@Body params: Map<String, String?>)

    @POST("$DIR/voicecall/agora_start_recording/")
    suspend fun startP2PCallRecording(@Body params: Map<String, String?>): HashMap<String, String>

    @POST("$DIR/voicecall/agora_stop_recording/")
    suspend fun stopP2PCallRecording(@Body params: Map<String, String?>): Any

    @GET("$DIR/voicecall/agora_user_profile/{id}/")
    suspend fun getUserDetailOnCall(@Path("id") id: String): HashMap<String, String>

    @POST("$DIR/voicecall/agora_call_location/")
    suspend fun uploadUserLocationAgora(@Body params: RequestUserLocation): Response<Void>


}
