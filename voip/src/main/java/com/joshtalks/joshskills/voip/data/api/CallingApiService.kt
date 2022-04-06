package com.joshtalks.joshskills.voip.data.api

import com.joshtalks.joshskills.base.constants.DIR
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CallingApiService {
    @POST("$DIR/p2p/call")
    suspend fun setUpConnection(@Body request : ConnectionRequest) : Response<Unit>

    @POST("$DIR/p2p/call_response")
    suspend fun callAccept(@Body request : CallAcceptRequest) : Response<Unit>
}


//"mentor_id": "dc83c689-c914-4694-b132-e93faf0aab23",
//"topic_id": 14,
//"course_id": 151

//call_id: call_id , response:ACCEPT/DECLINE, mentor_id:mentor_id (edited)