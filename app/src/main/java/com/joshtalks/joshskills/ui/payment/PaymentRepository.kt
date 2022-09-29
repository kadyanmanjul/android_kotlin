package com.joshtalks.joshskills.ui.payment

import com.joshtalks.joshskills.core.AppObjectController

class PaymentRepository {

    private val network by lazy {
        AppObjectController.commonNetworkService
    }

    suspend fun verifyPayment(orderId: String) = network.verifyPaymentV3(orderId)
}