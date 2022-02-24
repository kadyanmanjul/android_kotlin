package com.joshtalks.joshskills.core.abTest

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

const val DIR = "api/skill/v1"

interface ABTestNetworkService {

    @GET("$DIR/ab_test/get_variation/")
    suspend fun getCampaignData(@Query("campaign_key") campaign: String): Response<ABTestCampaignData>

    @GET("$DIR/ab_test/get_variations/")
    suspend fun getAllCampaigns(@Query("campaign_key_list") campaigns: String): Response<List<ABTestCampaignData>>

    @POST("$DIR/ab_test/track_conversion/")
    suspend fun postGoalData(@Body params: Map<String, String>): Response<Void>
}