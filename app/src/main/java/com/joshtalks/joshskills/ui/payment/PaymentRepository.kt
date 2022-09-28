package com.joshtalks.joshskills.ui.payment

import com.joshtalks.joshskills.core.AppObjectController
import kotlinx.coroutines.CoroutineScope
import retrofit2.Response

class PaymentRepository {

    private val network by lazy {
        AppObjectController.commonNetworkService
    }

    suspend fun verifyPayment(orderId: String) = network.verifyPaymentV3(orderId)
}