package com.joshtalks.joshskills.base.services

import com.joshtalks.joshskills.base.constants.DIR
import com.joshtalks.joshskills.base.model.common.ABTestCampaignData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ABTestNetworkService {

    @GET("$DIR/ab_test/get_variation/")
    suspend fun getCampaignData(@Query("campaign_key") campaign: String): Response<ABTestCampaignData>

    @GET("$DIR/ab_test/get_variations/")
    suspend fun getAllCampaigns(@Query("campaign_key_list") campaigns: String): Response<List<ABTestCampaignData>>

    @POST("$DIR/ab_test/track_conversion/")
    suspend fun postGoalData(@Body params: Map<String, String>): Response<Void>
}