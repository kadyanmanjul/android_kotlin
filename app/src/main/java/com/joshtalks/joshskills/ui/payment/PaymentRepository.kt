package com.joshtalks.joshskills.ui.payment

import com.joshtalks.joshskills.core.AppObjectController
import kotlinx.coroutines.CoroutineScope
import retrofit2.Response

class PaymentRepository {

    private val network by lazy {
        AppObjectController.commonNetworkService
    }

    suspend fun verifyPayment(params: Map<String, String>): Response<Any> = network.verifyPaymentWithResponse(params)
}