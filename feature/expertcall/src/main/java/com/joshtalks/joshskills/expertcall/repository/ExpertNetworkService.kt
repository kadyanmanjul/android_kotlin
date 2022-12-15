package com.joshtalks.joshskills.expertcall.repository

import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.repository.local.model.WalletBalance
import com.joshtalks.joshskills.common.repository.service.DIR
import com.joshtalks.joshskills.expertcall.model.*
import retrofit2.Response
import retrofit2.http.*

object ExpertNetwork {
    val expertNetworkService: ExpertNetworkService by lazy {
        AppObjectController.retrofit.create(ExpertNetworkService::class.java)
    }
}

interface ExpertNetworkService {
    @GET("$DIR/micro_payment/get_experts/")
    suspend fun getExpertList(): Response<ExpertListResponse>

    @GET("$DIR/micro_payment/user_wallet/{pk}/")
    suspend fun getWalletBalance(@Path("pk") mentorId: String): Response<WalletBalance>

    @GET("$DIR/micro_payment/check_wallet_balance/")
    suspend fun getCallNowStatus(@Query("expert_id") expertId: String): Response<WalletBalance>

    @GET("$DIR/micro_payment/get_amount_list/")
    suspend fun getAvailableAmounts(@Query("gaid") gaid: String): Response<AvailableAmount>

    @GET("$DIR/micro_payment/get_wallet_transactions/{mentor}/")
    suspend fun getWalletTransactions(@Path("mentor") mentorId: String, @Query("page") page: Int): Response<TransactionResponse>

    @GET("$DIR/micro_payment/get_payment_logs/{mentor}/")
    suspend fun getPaymentTransactions(@Path("mentor") mentorId: String, @Query("page") page: Int): Response<WalletLogResponse>

    @GET("$DIR/micro_payment/get_upgrade_details/")
    suspend fun getUpgradeDetails(@Query("gaid") gaid: String): Response<ExpertUpgradeDetails>

    @POST("$DIR/impression/track_buy_course_impression/")
    suspend fun saveNewBuyPageLayoutImpression(@Body params: Map<String, String>): Response<Void>
}