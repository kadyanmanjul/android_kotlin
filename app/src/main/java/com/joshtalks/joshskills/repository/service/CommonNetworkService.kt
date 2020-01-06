package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.repository.server.ComplaintResponse
import com.joshtalks.joshskills.repository.server.RequestComplaint
import com.joshtalks.joshskills.repository.server.TypeOfHelpModel
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CommonNetworkService {

    @GET("$DIR/support/category/")
    suspend fun getHelpCategory(): List<TypeOfHelpModel>


    @POST("$DIR/support/complaint/")
    suspend fun submitComplaint(@Body requestComplaint: RequestComplaint): ComplaintResponse



}